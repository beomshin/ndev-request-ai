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

        sb.append("# 작업\n")
                .append("아래 '개발요청서 입력'을 분석하여 프로젝트 메타데이터를 추출/추론하고, ")
                .append("지정된 JSON 스키마로만 출력한다.\n\n");

        sb.append("# 참고 링크 (반드시 직접 열람하여 내용을 추론)\n");
        for (String link : links) {
            sb.append("- ").append(link).append("\n");
        }
        sb.append("\n");

        sb.append("# 개발요청서 입력\n")
                .append("- 기능구분(funcType): ").append(nz(req.funcType())).append("\n")
                .append("- 카테고리(category): ").append(nz(req.category())).append("\n")
                .append("- 세부유형(subType): ").append(nz(req.subType())).append("\n")
                .append("- 작성자(author): ").append(nz(req.author())).append("\n")
                .append("- 부서(department): ").append(nz(req.department())).append("\n")
                .append("- 서비스명(serviceName): ").append(nz(req.serviceName())).append("\n")
                .append("- 추진배경(background): ").append(nz(req.background())).append("\n")
                .append("- 목표일정(targetSchedule): ").append(nz(req.targetSchedule())).append("\n")
                .append("- 문제점/개선점(problemAndImprovement): ").append(nz(req.problemAndImprovement())).append("\n\n");

        // ★ 핵심 추가 블록: 현업 입력을 IT가 이해할 수 있게 번역/해석하도록 지시
        sb.append("# 현업 입력 해석 지침 (가장 중요)\n")
                .append("- '문제점/개선점(problemAndImprovement)'은 IT가 아닌 현업이 작성했다. ")
                .append("비즈니스 용어, 축약, 모호하거나 기술적 디테일이 빠진 표현이 섞여 있어 ")
                .append("IT가 그대로는 이해·구현하기 어려울 가능성이 높다.\n")
                .append("- 따라서 위 참고 링크 문서와 PG 도메인 지식을 근거로, ")
                .append("현업이 실제로 요청하는 바가 무엇인지 IT 관점에서 명확하게 해석하여 설명한다.\n")
                .append("- 현업의 비즈니스 표현은 가능한 한 참고 문서에 등장하는 구체적인 PG/기술 용어로 매핑한다 ")
                .append("(예: 결제창 요청 파라미터, 승인/취소/환불 API, 할부 개월 옵션 필드, 웹훅 노티 등).\n")
                .append("- 원래 의도를 왜곡하거나 요구사항을 새로 만들지 않는다. ")
                .append("해석이 불확실한 부분은 문장 안에 '(가정)'을 명시한다.\n")
                .append("- issueAndImprovement 값에는 다음을 순서대로 담는다: ")
                .append("(1) 현업 요청의 핵심 의도 요약, (2) IT가 이해·구현 가능하도록 구체화한 기술적 해석, ")
                .append("(3) 근거가 된 문서/항목 표기.\n\n");

        sb.append("# 규칙\n")
                .append("1. 입력에 명시된 값은 그대로 반영한다. ")
                .append("단, problemAndImprovement 는 위 '현업 입력 해석 지침'에 따라 재해석하여 issueAndImprovement 에 담는다.\n")
                .append("2. 입력에 없는 값은 PG 도메인 지식과 웹 검색으로 합리적으로 추론한다.\n")
                .append("3. 추론 근거가 약하거나 알 수 없으면 빈 문자열(\"\") 또는 null 을 쓴다. 절대 지어내지 않는다.\n")
                .append("4. Boolean 필드는 true/false 로만 표기하고, 판단 불가 시 null.\n")
                .append("5. 아래 스키마의 키만 출력한다. 키 추가·삭제·이름변경 금지.\n")
                .append("6. 응답은 오직 JSON 하나만 출력한다. 코드펜스(```), 설명 문장, 머리말을 절대 붙이지 않는다.\n\n");

        sb.append("# 출력 형식 (JSON 스키마 — 키와 타입을 정확히 따른다)\n")
                .append("""
                    {
                      "author": "작성자(문자열)",
                      "createdDate": "작성일 YYYY-MM-DD(문자열)",
                      "department": "작성 부서(문자열)",
                      "projectId": "프로젝트 ID(문자열)",
                      "mid": "MID 가맹점 ID(문자열)",
                      "merchantName": "가맹점 상호명(문자열)",
                      "merchantBusinessNumber": "가맹점 사업자번호(문자열)",
                      "providerName": "원천사 상호명(문자열)",
                      "providerCollaborationBackground": "원천사 협업 배경(문자열)",
                      "promotionBackground": "추진 배경(문자열)",
                      "additionalInfo": "추가 정보(문자열)",
                      "targetDate": "목표 일정(문자열)",
                      "productName": "제품명(문자열)",
                      "issueAndImprovement": "현업이 입력한 문제점/개선점을, 참고 문서와 PG 지식을 근거로 IT가 이해·구현 가능하도록 재해석한 설명. 현업 의도 요약 + 기술적 해석 + 문서 근거 포함(문자열)",
                      "issueVerificationMethod": "문제를 확인하는 방법(문자열)",
                      "serviceChannelAndPaymentMethod": "서비스 제공 채널 / 결제 방식(문자열)",
                      "authApprovalAcquirerSubject": "인증/승인/매입 주체(문자열)",
                      "minimumPaymentAmount": "최소 결제 금액 제한, 단위 원(문자열)",
                      "partialCancelAndRefundPolicy": "부분취소 및 환불 정책(문자열)",
                      "cashReceiptIssuer": "현금영수증 발행 주체(문자열)",
                      "expectedRevenue": "예상 수익(문자열)",
                      "expectedLoss": "예상 손해(문자열)",
                      "expectedEffect": "기대 효과(문자열)",
                      "transferGuide": "타 부서/제휴사 이관 가이드(문자열)",
                      "developmentInProgress": "개발 진행 여부(true/false/null)",
                      "merchantRelatedDevelopment": "가맹점과 관계되는 개발 여부(true/false/null)",
                      "providerRelatedDevelopment": "원천사와 관계되는 개발 여부(true/false/null)",
                      "newServiceOrSelfImprovement": "신규 서비스/자체 개선 개발 여부(true/false/null)",
                      "securityAndAuditDevelopment": "보안 및 감사 대응 개발 여부(true/false/null)"
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
