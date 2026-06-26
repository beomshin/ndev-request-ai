package com.nice.qa.service.llm.md;

import com.nice.qa.service.llm.dto.ProjectMdResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * {@link ProjectMdResult} DTO를 개발 표준 요청 양식(v1.7) 마크다운 문서로 변환하는 렌더러.
 *
 * <p>LLM이 추론·구조화한 {@link ProjectMdResult} 객체를 입력으로 받아,
 * 사람이 읽고 검토할 수 있는 마크다운 형식의 개발 요청서를 출력한다.
 * 출력 문서는 YAML front matter + 8개 섹션으로 구성된다.
 *
 * <p>섹션 구성:
 * <ol>
 *   <li>요청 정보 — 작성자·날짜·부서·프로젝트 ID</li>
 *   <li>R&amp;R 판정 — PG개발실 담당 여부 및 이관 가이드, 개발 유형 체크박스</li>
 *   <li>요청 사항 — 목표 일정, 제품명, 문제점, 확인 방법</li>
 *   <li>기술/정책 세부 요구사항 — 채널, 결제 방식, 정책 항목</li>
 *   <li>개발 범위 및 복잡도 — AI 분석 결과</li>
 *   <li>기대 효과 — 수익/손해/효과</li>
 *   <li>현업 재확인 필요 항목 — 핑퐁 방지용 질문 목록 및 AI 추론 근거</li>
 *   <li>처리 상태 — 접수/리뷰/담당자 초기 상태 (기본값)</li>
 * </ol>
 *
 * <p>실제 문자열 조립은 {@link MarkdownBuilder}를 통해 fluent 스타일로 이루어진다.
 *
 * @see ProjectMdResult  렌더링 대상 DTO (LLM 응답 역직렬화 결과)
 * @see MarkdownBuilder  마크다운 문자열 조립 헬퍼
 * @see com.nice.qa.service.llm.promt.ProjectPromptBuilder  LLM 프롬프트 조립기 (입력 측)
 */
@Component
public class StandardMarkdownRenderer {

    /**
     * 출력 마크다운 문서 상단에 삽입되는 YAML front matter.
     *
     * <p>문서 카테고리, 문서 유형, 버전을 메타데이터로 기록한다.
     * 지식 저장소(KB) 인덱싱 및 문서 관리 시스템에서 활용된다.
     */
    private static final String FRONT_MATTER = """
            ---
            category: "템플릿/출력양식"
            document_type: "개발 표준 요청 양식"
            version: "v1.7"
            ---
            """;

    /**
     * {@link ProjectMdResult} 객체를 마크다운 문자열로 렌더링한다.
     *
     * <p>처리 흐름:
     * <ol>
     *   <li>{@link MarkdownBuilder}를 생성하고 front matter, H1 제목을 추가한다.</li>
     *   <li>섹션 1~6을 순서대로 {@link MarkdownBuilder}에 추가한다.</li>
     *   <li>섹션 7(현업 재확인 항목)은 {@code pendingQuestions}와 {@code assumptionList}
     *       유무에 따라 조건부 렌더링한다.</li>
     *   <li>섹션 8(처리 상태)은 항상 기본값으로 출력된다.</li>
     * </ol>
     *
     * @param r LLM이 분석·추론한 구조화 결과 DTO. {@code null} 필드는 빈 값으로 처리된다.
     * @return 완성된 마크다운 문자열 (UTF-8 텍스트, 줄 끝 {@code \n}).
     */
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

    /**
     * 섹션 7 — 현업 재확인 필요 항목(핑퐁 방지)을 {@link MarkdownBuilder}에 추가한다.
     *
     * <p>LLM이 추론할 수 없어 현업에게 반드시 확인해야 할 질문 목록을 번호 순서로 출력한다.
     * 질문 목록이 비어 있으면 "모든 항목 추론 완료" 메시지를 출력하여 IT 담당자가
     * 추가 문의 없이 착수 가능함을 명시한다.
     *
     * <p>이 섹션이 핑퐁 방지의 핵심 — IT가 현업에게 한 번에 질문을 던질 수 있도록
     * 질문을 한 곳에 모아 제공한다.
     *
     * @param mb        마크다운 빌더 (누적 버퍼에 직접 추가)
     * @param questions LLM이 생성한 재확인 질문 목록. {@code null} 또는 빈 리스트 허용.
     */
    private static void appendPendingQuestions(MarkdownBuilder mb, List<String> questions) {
        mb.h2("7. 현업 재확인 필요 항목 (핑퐁 방지)");
        if (questions == null || questions.isEmpty()) {
            // 질문 목록이 없으면 AI가 모든 필드를 추론·확정했음을 명시
            mb.raw("> 추가 확인 사항 없음 — AI가 모든 항목을 추론 완료했습니다.").blank();
            return;
        }
        // 질문이 있으면 IT 담당자에게 한 번에 현업에 확인하라는 안내 문구 출력
        mb.raw("> 아래 항목을 현업에게 한 번에 확인하면 추가 핑퐁 없이 개발을 착수할 수 있습니다.").blank();
        // 질문을 1-indexed 번호 목록으로 출력 (현업이 직접 답변 가능한 형식)
        for (int i = 0; i < questions.size(); i++) {
            mb.raw(String.format("%d. %s", i + 1, questions.get(i)));
        }
        mb.blank();
    }

    /**
     * 섹션 7.1 — AI 추론 근거(가정 목록)를 {@link MarkdownBuilder}에 추가한다.
     *
     * <p>LLM이 현업 입력에 명시되지 않은 항목을 참고 문서·PG 도메인 지식으로
     * 추론하여 채운 경우, 해당 항목의 가정 내용과 근거를 투명하게 공개한다.
     * IT 담당자가 가정이 틀렸을 경우 직접 수정할 수 있도록 한다.
     *
     * @param mb          마크다운 빌더 (누적 버퍼에 직접 추가)
     * @param assumptions LLM이 기록한 추론 근거 목록. {@code null} 또는 빈 리스트 허용.
     */
    private static void appendAssumptions(MarkdownBuilder mb, List<String> assumptions) {
        mb.h3("7.1 AI 추론 근거 (가정 목록)");
        if (assumptions == null || assumptions.isEmpty()) {
            // 추론 항목이 없으면 간단히 안내
            mb.raw("> 추론 항목 없음").blank();
            return;
        }
        // 추론 항목이 있으면 검토·수정 요청 안내 문구와 함께 항목별 불릿 목록 출력
        mb.raw("> 아래 항목은 입력에 명시되지 않아 AI가 참고 문서·PG 도메인 지식으로 추론했습니다. 확인 후 수정하세요.").blank();
        for (String assumption : assumptions) {
            mb.raw("- " + assumption);
        }
        mb.blank();
    }

    /**
     * null-safe 문자열 변환 헬퍼.
     *
     * @param value 원본 문자열 ({@code null} 허용)
     * @return {@code null}이면 빈 문자열, 그 외에는 원본 값
     */
    private static String text(String value) {
        return value == null ? "" : value;
    }

    /**
     * LLM의 Boolean 개발 진행 여부 판정 결과를 사람이 읽기 쉬운 상태 텍스트로 변환한다.
     *
     * <p>LLM이 R&amp;R 판정 불가 시 {@code null}을 반환할 수 있으므로 null-safe 처리한다.
     *
     * @param inProgress {@code true}: PG개발실 담당 가능, {@code false}: 이관 필요, {@code null}: 판정 불가
     * @return 이모지 포함 한국어 상태 문자열
     */
    private static String devStatus(Boolean inProgress) {
        if (inProgress == null) {
            // LLM이 R&R 판정을 못 한 경우 — 양쪽 경우를 모두 표시
            return "[ 진행 가능 / ❌ PG개발실 R&R 아님 - 접수 불가 ]";
        }
        return inProgress ? "✅ 진행 가능" : "❌ PG개발실 R&R 아님 - 접수 불가";
    }

    /**
     * LLM이 반환한 복잡도 코드({@code LOW}/{@code MID}/{@code HIGH})를
     * 이모지가 포함된 설명 레이블로 변환한다.
     *
     * <p>대소문자를 무시하여 매핑하므로 {@code low}, {@code Low} 등도 처리된다.
     * 알 수 없는 값은 원본 문자열 그대로 반환한다.
     *
     * @param complexity LLM이 판정한 복잡도 코드. 빈 문자열 또는 {@code null} 허용.
     * @return 이모지 포함 한국어 복잡도 레이블. 값이 없으면 빈 문자열.
     */
    private static String complexityLabel(String complexity) {
        if (!StringUtils.hasText(complexity)) return "";
        // LLM 응답의 대소문자 차이를 허용하기 위해 toUpperCase() 후 비교
        return switch (complexity.toUpperCase()) {
            case "LOW"  -> "🟢 LOW — 단순 파라미터/설정 수준";
            case "MID"  -> "🟡 MID — 신규 API 연동 또는 기존 로직 수정";
            case "HIGH" -> "🔴 HIGH — 원천사 신규 협의/계약, 복수 시스템 영향";
            // 알 수 없는 복잡도 값은 원본 그대로 반환 (LLM이 비표준 값을 반환한 경우 대비)
            default     -> complexity;
        };
    }

    /**
     * 최소 결제 금액 문자열을 한국 원화 포맷({@code N,NNN원})으로 변환한다.
     *
     * <p>LLM이 반환하는 값은 "1000", "1,000원", "1000원" 등 형식이 일정하지 않으므로
     * 숫자만 추출하여 {@link String#format}으로 통일된 포맷으로 출력한다.
     *
     * @param amount LLM이 반환한 최소 결제 금액 문자열. 빈 문자열 또는 {@code null} 허용.
     * @return {@code "1,000원"} 형식의 포맷된 문자열. 숫자가 없으면 원본 값 그대로 반환.
     */
    private static String formatAmount(String amount) {
        if (!StringUtils.hasText(amount)) return "";
        // 숫자 이외의 문자(원, 쉼표, 공백 등) 제거하여 순수 숫자만 추출
        String digits = amount.replaceAll("[^0-9]", "");
        // 숫자가 없는 경우(예: "미정", "협의 후 결정") 원본 문자열 그대로 반환
        return digits.isEmpty() ? amount : String.format("%,d원", Long.parseLong(digits));
    }
}
