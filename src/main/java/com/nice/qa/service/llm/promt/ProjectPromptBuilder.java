package com.nice.qa.service.llm.promt;

import com.nice.qa.model.api.dto.DevRequestRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ProjectMdResult 추출/추론용 프롬프트 조립기.
 * 정적인 지침은 텍스트 블록 상수로, 동적인 입력(링크/요청서)만 메서드로 만든다.
 */
@Component
public class ProjectPromptBuilder {

    private static final String CONTEXT = """
            # 맥락
            - 요청자는 'IT 지식이 낮은 현업 담당자'다. 결제 시스템 내부 구조나 PG 기술 용어를 잘 모른다.
            - 그래서 요청 내용이 모호하거나, 비즈니스 언어로만 쓰여 IT/개발팀이 그대로는 이해·구현하기 어렵다.
            - 너의 역할은 이 현업의 요청을, PG 도메인 지식과 참고 문서를 근거로 개발팀이 즉시 이해하고 착수할 수 있는 '표준 개발요청서 메타데이터(JSON)'로 번역·구조화하는 것이다.
            """;

    private static final String PROCEDURE = """
            # 분석 절차 (반드시 순서대로 수행)
            1) 의도 파악: 현업이 진짜로 해결하고 싶은 문제와 목표가 무엇인지 파악한다.
            2) R&R 판정: 이 요청이 PG개발실에서 처리할 일인지 판단한다. 결제창/인증·승인·취소·환불/정산/웹훅 등 PG 연동 영역이면 진행 가능(developmentInProgress=true), 그 외(예: 단순 마케팅 페이지, 타 시스템 영역)면 false 로 두고 transferGuide 에 이관 부서·행동을 적는다.
            3) 개발유형 분류: 아래 기준으로 4개 Boolean 을 판정한다.
               - merchantRelatedDevelopment: 특정 가맹점(MID·상호명)과 직접 얽힌 개발인가
               - providerRelatedDevelopment: 원천사(카드사/PG사 등) 규격·연동 변경이 필요한가
               - newServiceOrSelfImprovement: 신규 서비스이거나 자체 개선 성격인가 (funcType 참고)
               - securityAndAuditDevelopment: 보안/감사/컴플라이언스 대응 성격인가
            4) IT 명세화: 현업의 비즈니스 표현을 참고 문서의 구체적 PG/기술 용어로 매핑한다 (결제창 요청 파라미터, 승인/취소/환불 API, 할부개월 옵션 필드, 웹훅 노티 등).
            5) 필드 채우기: 위 결과를 아래 스키마에 정확히 채운다.
            """;

    private static final String ISSUE_RULE = """
            # issueAndImprovement 작성 규칙 (가장 중요)
            현업이 쓴 problemAndImprovement 를 그대로 복사하지 말고, 아래 3단을 순서대로 한 값에 담는다.
            (1) 현업 요청의 핵심 의도 요약 — 비즈니스 관점에서 무엇을 원하는지 한두 문장으로.
            (2) IT 기술 해석 — 참고 문서/PG 지식 기반으로 어떤 기능·파라미터·API로 구현되는지 구체화. 가능하면 문서에 등장하는 실제 필드·전문 항목명을 인용한다.
            (3) 근거 표기 — 판단의 출처가 된 문서/항목을 명시한다(예: nicepay_auth_spec.md 의 CardInstallMonth 항목).
            추론이 불확실한 부분은 문장 안에 '(가정)' 을 명시한다. 의도를 왜곡하거나 없는 요구사항을 만들지 않는다.
            """;

    private static final String COMMON_RULES = """
            # 공통 규칙
            1. 입력에 명시된 값은 그대로 반영한다. 단, problemAndImprovement 는 위 규칙에 따라 재해석해 issueAndImprovement 에 담는다.
            2. 입력에 없는 값은 참고 문서 → PG 도메인 지식 → 웹 검색 순으로 합리적으로 추론한다.
            3. 근거가 약하거나 알 수 없으면 빈 문자열 또는 null 을 쓴다. 절대 지어내지 않는다.
            4. Boolean 은 true/false 로만, 판단 불가 시 null.
            5. 스키마의 키만 출력한다. 키 추가·삭제·이름변경 금지.
            6. 응답은 오직 JSON 하나만 출력한다. 코드펜스(백틱 3개), 설명, 머리말 금지.
            """;

    private static final String OUTPUT_SCHEMA = """
            # 출력 형식 (JSON 스키마 — 키와 타입을 정확히 따른다)
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
            """;

    public String build(DevRequestRequest req, List<String> links) {
        return String.join("\n\n",
                CONTEXT.strip(),
                PROCEDURE.strip(),
                referenceLinks(links),
                requestInput(req),
                ISSUE_RULE.strip(),
                COMMON_RULES.strip(),
                OUTPUT_SCHEMA.strip());
    }

    private String referenceLinks(List<String> links) {
        String body = (links == null || links.isEmpty())
                ? "- (참고 링크 없음)"
                : links.stream().map(link -> "- " + link).collect(Collectors.joining("\n"));
        return "# 참고 링크 (반드시 직접 열람하여 내용을 추론)\n" + body;
    }

    private String requestInput(DevRequestRequest req) {
        return """
                # 개발요청서 입력 (현업 작성 원문)
                - 기능구분(funcType): %s
                - 카테고리(category): %s
                - 세부유형(subType): %s
                - 작성자(author): %s
                - 부서(department): %s
                - 서비스명(serviceName): %s
                - 추진배경(background): %s
                - 목표일정(targetSchedule): %s
                - 문제점/개선점(problemAndImprovement): %s"""
                .formatted(
                        nz(req.funcType()), nz(req.category()), nz(req.subType()),
                        nz(req.author()), nz(req.department()), nz(req.serviceName()),
                        nz(req.background()), nz(req.targetSchedule()), nz(req.problemAndImprovement()));
    }

    private static String nz(String value) {
        return Objects.toString(value, "");
    }
}
