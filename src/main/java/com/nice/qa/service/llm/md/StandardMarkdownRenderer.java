package com.nice.qa.service.llm.md;

import com.nice.qa.service.llm.dto.ProjectMdResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * ProjectMdResult → 개발 표준 요청 양식(v1.7) 마크다운 렌더러.
 */
@Component
public class StandardMarkdownRenderer {

    private static final String FRONT_MATTER = """
            ---
            category: "템플릿/출력양식"
            document_type: "개발 표준 요청 양식"
            version: "v1.7"
            ---
            """;

    public String render(ProjectMdResult r) {
        MarkdownBuilder mb = new MarkdownBuilder()
                .raw(FRONT_MATTER).blank()
                .h1("[개발 요청서] " + text(r.getProductName())).blank()

                // ── 1. 요청 정보 ────────────────────────────────────────────
                .h2("1. 요청 정보")
                .field("작성자", r.getAuthor())
                .field("작성일", r.getCreatedDate())
                .field("작성부서", r.getDepartment())
                .field("프로젝트 ID", r.getProjectId()).blank()

                // ── 2. R&R 판정 ─────────────────────────────────────────────
                .h2("2. 요청 배경 정보 및 R&R 검증")
                .h3("2.1 개발팀 R&R 자동 판별 결과 [AI 판단 결과]")
                .field("개발 진행 여부", devStatus(r.getDevelopmentInProgress()))
                .field("타 부서/제휴사 이관 가이드", r.getTransferGuide()).blank()

                .h3("2.2 개발 유형 체크 (AI 자동 판단)")
                .checkbox("가맹점과 관계되는 개발이에요", r.getMerchantRelatedDevelopment())
                .checkbox("원천사와 관계되는 개발이에요", r.getProviderRelatedDevelopment())
                .checkbox("신규 서비스, 자체개선 개발이에요", r.getNewServiceOrSelfImprovement())
                .checkbox("보안 및 감사 대응 개발이에요", r.getSecurityAndAuditDevelopment()).blank()

                .h3("2.3 세부 배경 정보")
                .h4("2.3.1 가맹점 정보 (가맹점 관련 개발일 때만 작성)")
                .field("MID", r.getMid())
                .field("가맹점 상호명", r.getMerchantName())
                .field("가맹점 사업자번호", r.getMerchantBusinessNumber()).blank()

                .h4("2.3.2 원천사 정보 (원천사 관련 개발일 때만 작성)")
                .field("원천사 상호명", r.getProviderName())
                .field("원천사 협업 배경", r.getProviderCollaborationBackground()).blank()

                .h4("2.3.3 추진 배경 정보 (전 공통 필수)")
                .field("추진 배경", r.getPromotionBackground())
                .field("추가 정보", r.getAdditionalInfo()).blank()

                // ── 3. 요청 사항 ────────────────────────────────────────────
                .h2("3. 요청 사항")
                .field("목표일정", r.getTargetDate())
                .field("제품명", r.getProductName())
                .field("문제점 및 개선점", r.getIssueAndImprovement())
                .field("문제를 확인하는 방법", r.getIssueVerificationMethod()).blank()

                // ── 4. 기술/정책 세부 요구사항 ─────────────────────────────
                .h2("4. 기술/정책 세부 요구사항 (AI 인터뷰 취합 결과 반영)")
                .field("서비스 제공 채널 / 결제 방식", r.getServiceChannelAndPaymentMethod())
                .field("인증/승인/매입 주체", r.getAuthApprovalAcquirerSubject())
                .field("최소 결제 금액 제한", formatAmount(r.getMinimumPaymentAmount()))
                .field("부분취소 및 환불 정책", r.getPartialCancelAndRefundPolicy())
                .field("현금영수증 발행 주체", r.getCashReceiptIssuer()).blank()

                // ── 5. 개발 범위 및 복잡도 ─────────────────────────────────
                .h2("5. 개발 범위 및 복잡도 (AI 분석)")
                .field("개발 범위", r.getDevelopmentScope())
                .field("예상 복잡도", complexityLabel(r.getEstimatedComplexity()))
                .field("선행 조건", r.getPrerequisiteActions())
                .field("리스크 및 제약", r.getRiskAndConstraints()).blank()

                // ── 6. 기대 효과 ────────────────────────────────────────────
                .h2("6. 기대 효과")
                .field("예상수익", r.getExpectedRevenue())
                .field("예상손해", r.getExpectedLoss())
                .field("기대효과", r.getExpectedEffect()).blank();

        // ── 7. 현업 재확인 필요 항목 (핑퐁 방지) ──────────────────────────
        appendPendingQuestions(mb, r.getPendingQuestions());
        appendAssumptions(mb, r.getAssumptionList());

        // ── 8. 처리 상태 ────────────────────────────────────────────────────
        mb.h2("8. 처리 상태 (Default 세팅)")
                .field("진행상태", "[접수]")
                .field("개발 리뷰 상태", "[대기]")
                .field("개발 스펙 상태", "[대기]")
                .field("개발 담당자", "(미정)")
                .field("개발 리뷰 링크", "")
                .field("개발 스펙 링크", "");

        return mb.toString();
    }

    private static void appendPendingQuestions(MarkdownBuilder mb, List<String> questions) {
        mb.h2("7. 현업 재확인 필요 항목 (핑퐁 방지)");
        if (questions == null || questions.isEmpty()) {
            mb.raw("> 추가 확인 사항 없음 — AI가 모든 항목을 추론 완료했습니다.").blank();
            return;
        }
        mb.raw("> 아래 항목을 현업에게 한 번에 확인하면 추가 핑퐁 없이 개발을 착수할 수 있습니다.").blank();
        for (int i = 0; i < questions.size(); i++) {
            mb.raw(String.format("%d. %s", i + 1, questions.get(i)));
        }
        mb.blank();
    }

    private static void appendAssumptions(MarkdownBuilder mb, List<String> assumptions) {
        mb.h3("7.1 AI 추론 근거 (가정 목록)");
        if (assumptions == null || assumptions.isEmpty()) {
            mb.raw("> 추론 항목 없음").blank();
            return;
        }
        mb.raw("> 아래 항목은 입력에 명시되지 않아 AI가 참고 문서·PG 도메인 지식으로 추론했습니다. 확인 후 수정하세요.").blank();
        for (String assumption : assumptions) {
            mb.raw("- " + assumption);
        }
        mb.blank();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String devStatus(Boolean inProgress) {
        if (inProgress == null) {
            return "[ 진행 가능 / ❌ PG개발실 R&R 아님 - 접수 불가 ]";
        }
        return inProgress ? "✅ 진행 가능" : "❌ PG개발실 R&R 아님 - 접수 불가";
    }

    private static String complexityLabel(String complexity) {
        if (!StringUtils.hasText(complexity)) return "";
        return switch (complexity.toUpperCase()) {
            case "LOW"  -> "🟢 LOW — 단순 파라미터/설정 수준";
            case "MID"  -> "🟡 MID — 신규 API 연동 또는 기존 로직 수정";
            case "HIGH" -> "🔴 HIGH — 원천사 신규 협의/계약, 복수 시스템 영향";
            default     -> complexity;
        };
    }

    private static String formatAmount(String amount) {
        if (!StringUtils.hasText(amount)) return "";
        String digits = amount.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? amount : String.format("%,d원", Long.parseLong(digits));
    }
}
