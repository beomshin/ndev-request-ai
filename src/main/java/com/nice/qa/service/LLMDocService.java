package com.nice.qa.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.llm.dto.ProjectMdResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMDocService implements DocService {

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private static final String MODEL = "gemini-2.5-flash";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @Override
    public String assembleMarkdown(DevRequestRequest request) {
        Client client = Client.builder()
                .apiKey(geminiApiKey)
                .build();

        // 1) 역할: PG 전문가
        Content systemInstruction = Content.fromParts(Part.fromText(
                "당신은 국내외 PG(전자결제대행) 연동 전문가입니다. "
                        + "결제창 연동, 인증/승인/취소/환불, 정산, 웹훅(노티) 처리, 시큐어 코딩(PCI-DSS) 등 "
                        + "PG 도메인 전반을 깊이 이해하고 있습니다. "
                        + "역할은 개발요청서를 분석해 프로젝트 메타데이터를 정확하고 구현 가능한 형태의 JSON으로 추출/추론하는 것입니다."
        ));

        // 2) 부족분 검색 보완(GoogleSearch). req 에 참고 링크는 없으므로 UrlContext 는 사용하지 않음.
        Tool googleSearchTool = Tool.builder().googleSearch(GoogleSearch.builder()).build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstruction)
                .temperature(0.2f)          // 메타데이터 추출 → 일관/정확이 중요하므로 낮게
                .maxOutputTokens(8192)
                .tools(googleSearchTool)
                .build();

        // 분석할 참고 링크 (2번 입력)
        List<String> links = List.of(
                "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/pg_error_code_manual.md",
                "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/policy/pg_internal_policy.md",
                "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/provider/provider_kakaopay.md",
                "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/provider/provider_payco.md",
                "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/provider/provider_tosspay.md",
                "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/spec/nicepay_auth_spec.md",
                "https://github.com/beomshin/ndev-request-ai/blob/main/src/main/resources/docs/templates/standard_dev_request_output.md"
        );

        String prompt = buildProjectMdPrompt(request, links);

        GenerateContentResponse response = client.models.generateContent(MODEL, prompt, config);
        String json = stripCodeFence(response.text());

        log.info("=== ProjectMdResult 원본 JSON ===\n{}", json);

        ProjectMdResult result = parseOrEmpty(json);
        applyDeterministicFields(result, request);   // 신뢰 가능한 입력값으로 덮어쓰기

        log.info("{}", result);

        return toStandardMarkdown(result);
    }

    private static String buildProjectMdPrompt(DevRequestRequest req, List<String> links) {
        StringBuilder sb = new StringBuilder();

        // 0) 맥락 — 이 작업이 왜 필요한지 모델에게 명확히 인지시킴
        sb.append("# 맥락\n")
                .append("- 요청자는 'IT 지식이 낮은 현업 담당자'다. 결제 시스템 내부 구조나 PG 기술 용어를 잘 모른다.\n")
                .append("- 그래서 요청 내용이 모호하거나, 비즈니스 언어로만 쓰여 IT/개발팀이 그대로는 이해·구현하기 어렵다.\n")
                .append("- 너의 역할은 이 현업의 요청을, PG 도메인 지식과 참고 문서를 근거로 ")
                .append("개발팀이 즉시 이해하고 착수할 수 있는 '표준 개발요청서 메타데이터(JSON)'로 번역·구조화하는 것이다.\n\n");

        // 1) 분석 절차 — 모델이 따라야 할 사고 흐름을 단계로 고정
        sb.append("# 분석 절차 (반드시 순서대로 수행)\n")
                .append("1) 의도 파악: 현업이 진짜로 해결하고 싶은 문제와 목표가 무엇인지 파악한다.\n")
                .append("2) R&R 판정: 이 요청이 PG개발실에서 처리할 일인지 판단한다. ")
                .append("결제창/인증·승인·취소·환불/정산/웹훅 등 PG 연동 영역이면 진행 가능(developmentInProgress=true), ")
                .append("그 외(예: 단순 마케팅 페이지, 타 시스템 영역)면 false 로 두고 transferGuide 에 이관 부서·행동을 적는다.\n")
                .append("3) 개발유형 분류: 아래 기준으로 4개 Boolean 을 판정한다.\n")
                .append("   - merchantRelatedDevelopment: 특정 가맹점(MID·상호명)과 직접 얽힌 개발인가\n")
                .append("   - providerRelatedDevelopment: 원천사(카드사/PG사 등) 규격·연동 변경이 필요한가\n")
                .append("   - newServiceOrSelfImprovement: 신규 서비스이거나 자체 개선 성격인가 (funcType 참고)\n")
                .append("   - securityAndAuditDevelopment: 보안/감사/컴플라이언스 대응 성격인가\n")
                .append("4) IT 명세화: 현업의 비즈니스 표현을 참고 문서의 구체적 PG/기술 용어로 매핑한다 ")
                .append("(결제창 요청 파라미터, 승인/취소/환불 API, 할부개월 옵션 필드, 웹훅 노티 등).\n")
                .append("5) 필드 채우기: 위 결과를 아래 스키마에 정확히 채운다.\n\n");

        // 2) 참고 링크
        sb.append("# 참고 링크 (반드시 직접 열람하여 내용을 추론)\n");
        for (String link : links) {
            sb.append("- ").append(link).append("\n");
        }
        sb.append("\n");

        // 3) 현업 입력
        sb.append("# 개발요청서 입력 (현업 작성 원문)\n")
                .append("- 기능구분(funcType): ").append(nz(req.funcType())).append("\n")
                .append("- 카테고리(category): ").append(nz(req.category())).append("\n")
                .append("- 세부유형(subType): ").append(nz(req.subType())).append("\n")
                .append("- 작성자(author): ").append(nz(req.author())).append("\n")
                .append("- 부서(department): ").append(nz(req.department())).append("\n")
                .append("- 서비스명(serviceName): ").append(nz(req.serviceName())).append("\n")
                .append("- 추진배경(background): ").append(nz(req.background())).append("\n")
                .append("- 목표일정(targetSchedule): ").append(nz(req.targetSchedule())).append("\n")
                .append("- 문제점/개선점(problemAndImprovement): ").append(nz(req.problemAndImprovement())).append("\n\n");

        // 4) issueAndImprovement 전용 작성 규칙 (가장 중요)
        sb.append("# issueAndImprovement 작성 규칙 (가장 중요)\n")
                .append("현업이 쓴 problemAndImprovement 를 그대로 복사하지 말고, 아래 3단을 순서대로 한 값에 담는다.\n")
                .append("(1) 현업 요청의 핵심 의도 요약 — 비즈니스 관점에서 무엇을 원하는지 한두 문장으로.\n")
                .append("(2) IT 기술 해석 — 참고 문서/PG 지식 기반으로 어떤 기능·파라미터·API로 구현되는지 구체화. ")
                .append("가능하면 문서에 등장하는 실제 필드·전문 항목명을 인용한다.\n")
                .append("(3) 근거 표기 — 판단의 출처가 된 문서/항목을 명시한다(예: nicepay_auth_spec.md 의 CardInstallMonth 항목).\n")
                .append("추론이 불확실한 부분은 문장 안에 '(가정)' 을 명시한다. 의도를 왜곡하거나 없는 요구사항을 만들지 않는다.\n\n");

        // 5) 공통 규칙
        sb.append("# 공통 규칙\n")
                .append("1. 입력에 명시된 값은 그대로 반영한다. 단, problemAndImprovement 는 위 규칙에 따라 재해석해 issueAndImprovement 에 담는다.\n")
                .append("2. 입력에 없는 값은 참고 문서 → PG 도메인 지식 → 웹 검색 순으로 합리적으로 추론한다.\n")
                .append("3. 근거가 약하거나 알 수 없으면 빈 문자열(\"\") 또는 null 을 쓴다. 절대 지어내지 않는다.\n")
                .append("4. Boolean 은 true/false 로만, 판단 불가 시 null.\n")
                .append("5. 스키마의 키만 출력한다. 키 추가·삭제·이름변경 금지.\n")
                .append("6. 응답은 오직 JSON 하나만 출력한다. 코드펜스(```), 설명, 머리말 금지.\n\n");

        // 6) 출력 스키마 (표준 양식의 ex 힌트를 설명에 주입)
        sb.append("# 출력 형식 (JSON 스키마 — 키와 타입을 정확히 따른다)\n")
                .append("""
                {
                  "author": "작성자(문자열)",
                  "createdDate": "작성일 YYYY-MM-DD(문자열)",
                  "department": "작성 부서(문자열)",
                  "projectId": "프로젝트 ID(문자열)",
                  "mid": "MID 가맹점 ID. 가맹점 관련 개발일 때만(문자열)",
                  "merchantName": "가맹점 상호명. 가맹점 관련 개발일 때만(문자열)",
                  "merchantBusinessNumber": "가맹점 사업자번호. 가맹점 관련 개발일 때만(문자열)",
                  "providerName": "원천사 상호명. 원천사 관련 개발일 때만(문자열)",
                  "providerCollaborationBackground": "원천사 협업 배경(문자열)",
                  "promotionBackground": "추진 배경 — 어떤 현상이 있고 무엇을 해결/목적하는지(문자열)",
                  "additionalInfo": "원천사 규격서 매핑 데이터 및 테크니컬 체크포인트 요약(문자열)",
                  "targetDate": "목표 일정 YYYY-MM-DD(문자열)",
                  "productName": "제품명(문자열)",
                  "issueAndImprovement": "현업 problemAndImprovement 를 (1)의도요약 (2)IT 기술해석 (3)문서근거 3단으로 재해석한 설명(문자열)",
                  "issueVerificationMethod": "개발자가 직접 재현·확인할 수 있는 경로 및 테스트 조건(문자열)",
                  "serviceChannelAndPaymentMethod": "서비스 채널/결제 방식. ex: 온라인 Web&App / 단건결제 및 빌링결제(문자열)",
                  "authApprovalAcquirerSubject": "인증/승인/매입 주체. ex: 인증-원천사, 승인-자사, 매입-카드사 직매입(문자열)",
                  "minimumPaymentAmount": "최소 결제 금액 하한선. 단위 원, 숫자만. ex: 1000(문자열)",
                  "partialCancelAndRefundPolicy": "부분취소/환불 정책. ex: 부분취소 가능 여부, 복합결제 시 취소 우선순위(문자열)",
                  "cashReceiptIssuer": "현금영수증 발행 주체. PG 대행 / 제휴사 직접 발행 / 해당 없음 중 택1 또는 구체 주체(문자열)",
                  "expectedRevenue": "예상 수익(문자열)",
                  "expectedLoss": "예상 손해(문자열)",
                  "expectedEffect": "이 개발 완료 후 기대하는 정성적/정량적 효과(문자열)",
                  "transferGuide": "developmentInProgress=false 일 때만, 이관할 부서 및 행동 가이드(문자열)",
                  "developmentInProgress": "PG개발실 R&R 해당 여부(true/false/null)",
                  "merchantRelatedDevelopment": "가맹점 관련 개발 여부(true/false/null)",
                  "providerRelatedDevelopment": "원천사 관련 개발 여부(true/false/null)",
                  "newServiceOrSelfImprovement": "신규 서비스/자체 개선 여부(true/false/null)",
                  "securityAndAuditDevelopment": "보안 및 감사 대응 여부(true/false/null)"
                }
                """);

        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** 모델이 ```json ... ``` 코드펜스를 붙였을 경우 제거 */
    private static String stripCodeFence(String text) {
        if (text == null) return "";
        String t = text.strip();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t.strip();
    }

    private static ProjectMdResult parseOrEmpty(String json) {
        try {
            ProjectMdResult r = MAPPER.readValue(json, ProjectMdResult.class);
            return r != null ? r : emptyResult();
        } catch (Exception e) {
            log.error("ProjectMdResult JSON 파싱 실패. raw=\n{}", json, e);
            return emptyResult();
        }
    }

    private static ProjectMdResult emptyResult() {
        try {
            return MAPPER.readValue("{}", ProjectMdResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("빈 ProjectMdResult 생성 실패", e);
        }
    }

    private static void applyDeterministicFields(ProjectMdResult result, DevRequestRequest req) {
        result.setAuthor(req.author());
        result.setDepartment(req.department());

        if (isBlank(result.getProductName())) {
            result.setProductName(req.serviceName());
        }
        if (isBlank(result.getCreatedDate())) {
            result.setCreatedDate(LocalDate.now().toString());
        }
        // funcType("신규 서비스 개발" / "기존 서비스 수정·개선")으로 신규/자체개선 여부 보정
        if (req.funcType() != null) {
            result.setNewServiceOrSelfImprovement(req.funcType().contains("신규"));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** ProjectMdResult → 개발 표준 요청 양식(v1.6) MD 문자열 */
    private static String toStandardMarkdown(ProjectMdResult r) {
        StringBuilder md = new StringBuilder();

        // Front matter
        md.append("---\n")
                .append("category: \"템플릿/출력양식\"\n")
                .append("document_type: \"개발 표준 요청 양식\"\n")
                .append("version: \"v1.6\"\n")
                .append("---\n\n");

        // 제목 (제목 전용 필드가 없어 제품명을 사용)
        md.append("# [개발 요청서] ").append(nz(r.getProductName())).append("\n\n");

        // 1. 요청 정보
        md.append("## 1. 요청 정보\n");
        md.append(field("작성자", r.getAuthor()));
        md.append(field("작성일", r.getCreatedDate()));
        md.append(field("작성부서", r.getDepartment()));
        md.append(field("프로젝트 ID", r.getProjectId())).append("\n");

        // 2. 요청 배경 정보 및 R&R 검증
        md.append("## 2. 요청 배경 정보 및 R&R 검증\n");
        md.append("### 2.1 개발팀 R&R 자동 판별 결과 [AI 판단 결과]\n");
        md.append("- **개발 진행 여부:** ").append(devStatus(r.getDevelopmentInProgress())).append("\n");
        md.append(field("타 부서/제휴사 이관 가이드", r.getTransferGuide())).append("\n");

        md.append("### 2.2 개발 유형 체크 (AI 자동 판단)\n");
        md.append(checkbox(r.getMerchantRelatedDevelopment())).append(" 가맹점과 관계되는 개발이에요\n");
        md.append(checkbox(r.getProviderRelatedDevelopment())).append(" 원천사와 관계되는 개발이에요\n");
        md.append(checkbox(r.getNewServiceOrSelfImprovement())).append(" 신규 서비스, 자체개선 개발이에요\n");
        md.append(checkbox(r.getSecurityAndAuditDevelopment())).append(" 보안 및 감사 대응 개발이에요\n\n");

        md.append("### 2.3 세부 배경 정보\n");
        md.append("#### 2.3.1 가맹점 정보 (가맹점 관련 개발일 때만 작성)\n");
        md.append(field("MID", r.getMid()));
        md.append(field("가맹점 상호명", r.getMerchantName()));
        md.append(field("가맹점 사업자번호", r.getMerchantBusinessNumber())).append("\n");

        md.append("#### 2.3.2 원천사 정보 (원천사 관련 개발일 때만 작성)\n");
        md.append(field("원천사 상호명", r.getProviderName()));
        md.append(field("원천사 협업 배경", r.getProviderCollaborationBackground())).append("\n");

        md.append("#### 2.3.3 추진 배경 정보 (전 공통 필수)\n");
        md.append(field("추진 배경", r.getPromotionBackground()));
        md.append(field("추가 정보", r.getAdditionalInfo())).append("\n");

        // 3. 요청 사항
        md.append("## 3. 요청 사항\n");
        md.append(field("목표일정", r.getTargetDate()));
        md.append(field("목표일정 근거", null));          // DTO에 없는 필드
        md.append(field("제품명", r.getProductName()));
        md.append(field("문제점 및 개선점", r.getIssueAndImprovement()));
        md.append(field("문제를 확인하는 방법", r.getIssueVerificationMethod()));
        md.append(field("문제 발생 빈도 (정량적)", null)); // DTO에 없는 필드
        md.append(field("경쟁사 정보", null)).append("\n");  // DTO에 없는 필드

        // 4. 기술/정책 세부 요구사항
        md.append("## 4. 기술/정책 세부 요구사항 (AI 인터뷰 취합 결과 반영)\n");
        md.append(field("서비스 제공 채널 / 결제 방식", r.getServiceChannelAndPaymentMethod()));
        md.append(field("인증/승인/매입 주체", r.getAuthApprovalAcquirerSubject()));
        md.append(field("최소 결제 금액 제한", formatAmount(r.getMinimumPaymentAmount())));
        md.append(field("부분취소 및 환불 정책", r.getPartialCancelAndRefundPolicy()));
        md.append(field("현금영수증 발행 주체", r.getCashReceiptIssuer())).append("\n");

        // 5. 기대 효과
        md.append("## 5. 기대 효과\n");
        md.append(field("예상수익", r.getExpectedRevenue()));
        md.append(field("예상손해", r.getExpectedLoss()));
        md.append(field("기대효과", r.getExpectedEffect())).append("\n");

        // 6. 처리 상태 (Default 세팅)
        md.append("## 6. 처리 상태 (Default 세팅)\n");
        md.append("- **진행상태:** [접수]\n");
        md.append("- **개발 리뷰 상태:** [대기]\n");
        md.append("- **개발 스펙 상태:** [대기]\n");
        md.append("- **개발 담당자:** (미정)\n");
        md.append("- **개발 리뷰 링크:** \n");
        md.append("- **개발 스펙 링크:** \n");

        return md.toString();
    }

    /** 한 줄/여러 줄 값을 라벨 불릿으로. 여러 줄이면 라벨 아래 들여쓰기. */
    private static String field(String label, String value) {
        String v = nz(value);
        if (v.isEmpty()) {
            return "- **" + label + ":** \n";
        }
        if (v.contains("\n")) {
            return "- **" + label + ":**\n  " + v.replace("\n", "\n  ") + "\n";
        }
        return "- **" + label + ":** " + v + "\n";
    }

    private static String devStatus(Boolean inProgress) {
        if (inProgress == null) return "[ 진행 가능 / ❌ PG개발실 R&R 아님 - 접수 불가 ]";
        return inProgress ? "✅ 진행 가능" : "❌ PG개발실 R&R 아님 - 접수 불가";
    }

    private static String checkbox(Boolean b) {
        return (b != null && b) ? "- [x]" : "- [ ]";
    }

    private static String formatAmount(String amount) {
        if (amount == null || amount.isBlank()) return "";
        String digits = amount.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return amount;       // "PG정책에 따름" 같은 비숫자면 원문 유지
        return String.format("%,d원", Long.parseLong(digits));
    }
}
