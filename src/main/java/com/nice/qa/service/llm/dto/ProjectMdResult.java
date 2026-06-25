package com.nice.qa.service.llm.dto;

import lombok.*;

import java.util.List;

/**
 * LLM이 개발 요청서 입력을 분석·추론하여 생성한 구조화 결과를 담는 DTO.
 *
 * <p>이 클래스는 {@link com.nice.qa.service.llm.promt.ProjectPromptBuilder}가 조립한 프롬프트를
 * Gemini LLM에 전송했을 때 반환되는 JSON 응답을 역직렬화(deserialization)하는 대상이다.
 *
 * <p>LLM은 현업(비IT)이 작성한 추상적인 개발 요청서를 분석하여 다음 세 가지 방식으로 각 필드를 채운다:
 * <ol>
 *   <li><b>확정 정보</b>: 현업이 명시적으로 제공한 값 — 그대로 반영</li>
 *   <li><b>추론 정보</b>: 참고 문서·PG 도메인 지식으로 합리적으로 채운 값
 *       — {@link #assumptionList}에 근거 기록</li>
 *   <li><b>미확보 정보</b>: 추론 불가, 현업에게 재확인 필요한 값
 *       — {@link #pendingQuestions}에 질문으로 변환</li>
 * </ol>
 *
 * <p>이 객체는 {@link com.nice.qa.service.llm.md.StandardMarkdownRenderer}에 전달되어
 * 개발 표준 요청 양식(Markdown v1.7)으로 렌더링된다.
 *
 * <p>Lombok {@code @AllArgsConstructor} + {@code @NoArgsConstructor(PRIVATE)}를 조합하여
 * Jackson 역직렬화 시 전체 생성자를 사용하되, 외부에서 임의 생성하지 못하도록 한다.
 *
 * @see com.nice.qa.service.llm.promt.ProjectPromptBuilder  프롬프트 조립 (입력)
 * @see com.nice.qa.service.llm.md.StandardMarkdownRenderer 마크다운 렌더링 (출력)
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ProjectMdResult {

    // ── 기본 정보 ──────────────────────────────────────────
    /** 요청서 작성자 이름 (현업 담당자) */
    private String author;
    /** 요청서 작성일 (YYYY-MM-DD 형식) */
    private String createdDate;
    /** 작성 부서명 */
    private String department;
    /** 프로젝트 ID (없으면 null) */
    private String projectId;

    // ── 가맹점 정보 ────────────────────────────────────────
    /** MID(가맹점 ID) — 가맹점 관련 개발일 때만 입력 */
    private String mid;
    /** 가맹점 상호명 — 가맹점 관련 개발일 때만 입력 */
    private String merchantName;
    /** 가맹점 사업자등록번호 — 가맹점 관련 개발일 때만 입력 */
    private String merchantBusinessNumber;

    // ── 원천사 정보 ────────────────────────────────────────
    /** 원천사(카드사/PG사) 상호명 — 원천사 관련 개발일 때만 입력 */
    private String providerName;
    /** 원천사와의 협업 배경 및 계약 현황 */
    private String providerCollaborationBackground;

    // ── 추진 배경 및 목표 ──────────────────────────────────
    /** 추진 배경 — 어떤 현상이 있고 무엇을 해결/목적하는지 비즈니스 관점 서술 */
    private String promotionBackground;
    /** 원천사 규격 매핑 데이터 및 테크니컬 체크포인트 요약 추가 정보 */
    private String additionalInfo;
    /** 목표 완료 일정 (YYYY-MM-DD) */
    private String targetDate;
    /** 개발 대상 제품/서비스명 */
    private String productName;

    // ── 문제점 및 서비스 정보 ──────────────────────────────
    /**
     * 문제점 및 개선점 — LLM이 4단 구조로 재해석한 내용:
     * (1) 현업 요청 핵심 의도 요약
     * (2) IT 기술 해석 (파라미터/API 수준)
     * (3) 개발자 기술 체크포인트
     * (4) 판단 근거 문서 출처
     */
    private String issueAndImprovement;
    /** 개발 완료 여부를 개발자가 직접 재현·확인할 수 있는 경로 및 테스트 조건 */
    private String issueVerificationMethod;
    /** 서비스 제공 채널(PC/Mobile/App)과 결제 방식(단건/빌링/링크결제 등) */
    private String serviceChannelAndPaymentMethod;
    /** 인증/승인/매입 주체 (예: 인증-원천사, 승인-자사, 매입-카드사 직매입) */
    private String authApprovalAcquirerSubject;

    // ── 결제 정책 ──────────────────────────────────────────
    /** 최소 결제 금액 하한선 (단위: 원, 숫자만 저장. 예: "1000") */
    private String minimumPaymentAmount;
    /** 부분취소 및 환불 정책 (부분취소 가능 여부, 복합결제 취소 우선순위 등) */
    private String partialCancelAndRefundPolicy;
    /** 현금영수증 발행 주체 (예: PG 대행 / 제휴사 직접 발행 / 해당 없음) */
    private String cashReceiptIssuer;

    // ── 수익 및 기대효과 ───────────────────────────────────
    /** 개발 완료 시 예상 수익 (금액 또는 정성적 기술) */
    private String expectedRevenue;
    /** 개발 완료 시 예상 손해 또는 비용 */
    private String expectedLoss;
    /** 개발 완료 후 기대하는 정성적/정량적 효과 */
    private String expectedEffect;

    // ── 이관 및 개발 정보 ──────────────────────────────────
    /**
     * 이관 가이드 — {@link #developmentInProgress}가 {@code false}일 때만 작성.
     * 처리해야 할 부서와 구체적인 행동 지침을 기술한다.
     */
    private String transferGuide;

    /** PG개발실 R&amp;R 해당 여부. {@code false}이면 {@link #transferGuide}를 참조한다. */
    private Boolean developmentInProgress;
    /** 특정 MID·가맹점 상호명과 직접 얽힌 개발 여부 */
    private Boolean merchantRelatedDevelopment;
    /** 원천사(카드사/PG사) 규격·연동 변경이 필요한 개발 여부 */
    private Boolean providerRelatedDevelopment;
    /** 신규 서비스 출시 또는 자체 시스템 개선 성격의 개발 여부 */
    private Boolean newServiceOrSelfImprovement;
    /** 보안 심의·감사·컴플라이언스 대응 성격의 개발 여부 */
    private Boolean securityAndAuditDevelopment;

    // ── 핑퐁 방지: IT↔현업 재확인 필요 항목 ──────────────
    /**
     * AI가 추론할 수 없어 현업에게 반드시 재확인해야 할 질문 목록.
     *
     * <p>이 목록이 있으면 IT가 별도 문의 없이 첫 답변에 담아 한 번에 해결할 수 있다.
     * 전형적으로 3~5회 발생하는 현업↔IT 핑퐁을 0~1회로 줄이는 것이 핵심 목적이다.
     *
     * <p>질문 형식: "~를 알려주세요 (예: ...)" 형태로 구체적으로 작성.
     * 추론 가능한 항목은 질문 대신 {@link #assumptionList}에 근거와 함께 기록한다.
     */
    private List<String> pendingQuestions;

    /**
     * 추론으로 채운 값의 근거·가정 목록.
     *
     * <p>현업이 명시하지 않았지만 AI가 참고 문서·PG 도메인 지식으로 합리적으로 추론한
     * 항목의 근거를 투명하게 공개한다. IT 담당자가 검토 후 수정할 수 있도록 한다.
     *
     * <p>형식: {@code "항목명: 가정 내용 (근거: 문서명 또는 PG 도메인 관행)"}
     *
     * <p>예시: {@code "serviceChannelAndPaymentMethod: PC/Mobile 모두 적용으로 가정 (근거: 동종 요청 사례 pattern)"}
     */
    private List<String> assumptionList;

    /**
     * 개발 착수 전 반드시 완료해야 할 선행 조건.
     *
     * <p>예시:
     * <ul>
     *   <li>원천사와 연동 계약 체결 확인</li>
     *   <li>MID 발급 후 테스트 환경 등록</li>
     *   <li>보안 심의 통과</li>
     *   <li>타팀 API 제공 일정 확인</li>
     * </ul>
     */
    private String prerequisiteActions;

    /**
     * IT 관점에서 이번 개발의 구체적인 범위.
     *
     * <p>신규/수정 대상 모듈·API·화면과 기존 서비스에 미치는 영향 범위를 포함한다.
     * LLM이 참고 문서를 기반으로 현업 요청에 명시되지 않은 필요 기능까지 추론하여 기술한다.
     */
    private String developmentScope;

    /**
     * 예상 개발 복잡도: {@code LOW} | {@code MID} | {@code HIGH}.
     *
     * <ul>
     *   <li>{@code LOW}: 단순 파라미터 추가/설정 변경 수준</li>
     *   <li>{@code MID}: 신규 API 연동 또는 기존 로직 수정</li>
     *   <li>{@code HIGH}: 원천사 신규 협의·계약, 복수 시스템 영향, 보안 심의 수반</li>
     * </ul>
     *
     * {@link com.nice.qa.service.llm.md.StandardMarkdownRenderer#complexityLabel(String)}에서
     * 이모지 레이블로 변환된다.
     */
    private String estimatedComplexity;

    /**
     * 개발 진행 중 예상되는 리스크와 제약 사항.
     *
     * <p>일정 리스크, 원천사 대응 지연, 정책 충돌, 기술 제약 등을 기술한다.
     */
    private String riskAndConstraints;
}
