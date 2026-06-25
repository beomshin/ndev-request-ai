package com.nice.qa.service.llm.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ProjectMdResult {

    // ── 기본 정보 ──────────────────────────────────────────
    private String author;
    private String createdDate;
    private String department;
    private String projectId;

    // ── 가맹점 정보 ────────────────────────────────────────
    private String mid;
    private String merchantName;
    private String merchantBusinessNumber;

    // ── 원천사 정보 ────────────────────────────────────────
    private String providerName;
    private String providerCollaborationBackground;

    // ── 추진 배경 및 목표 ──────────────────────────────────
    private String promotionBackground;
    private String additionalInfo;
    private String targetDate;
    private String productName;

    // ── 문제점 및 서비스 정보 ──────────────────────────────
    private String issueAndImprovement;
    private String issueVerificationMethod;
    private String serviceChannelAndPaymentMethod;
    private String authApprovalAcquirerSubject;

    // ── 결제 정책 ──────────────────────────────────────────
    private String minimumPaymentAmount;
    private String partialCancelAndRefundPolicy;
    private String cashReceiptIssuer;

    // ── 수익 및 기대효과 ───────────────────────────────────
    private String expectedRevenue;
    private String expectedLoss;
    private String expectedEffect;

    // ── 이관 및 개발 정보 ──────────────────────────────────
    private String transferGuide;
    private Boolean developmentInProgress;
    private Boolean merchantRelatedDevelopment;
    private Boolean providerRelatedDevelopment;
    private Boolean newServiceOrSelfImprovement;
    private Boolean securityAndAuditDevelopment;

    // ── 핑퐁 방지: IT↔현업 재확인 필요 항목 ──────────────
    /**
     * AI가 추론할 수 없어 현업에게 반드시 재확인해야 할 질문 목록.
     * 이 목록이 있으면 IT가 별도 문의 없이 첫 답변에 담아 한 번에 해결할 수 있다.
     */
    private List<String> pendingQuestions;

    /**
     * 추론으로 채운 값의 근거·가정 목록.
     * 현업이 명시하지 않았지만 AI가 합리적으로 추론한 내용을 투명하게 공개한다.
     */
    private List<String> assumptionList;

    /**
     * 개발 착수 전 반드시 완료해야 할 선행 조건.
     * 예: 원천사와 연동 계약 체결, MID 발급, 보안 심의, 타팀 API 협의 등.
     */
    private String prerequisiteActions;

    /**
     * IT 관점에서 이번 개발의 구체적인 범위.
     * 신규/수정 대상 모듈·API·화면과 기존 서비스에 미치는 영향 범위를 포함한다.
     */
    private String developmentScope;

    /**
     * 예상 개발 복잡도: LOW | MID | HIGH.
     * LOW: 단순 파라미터 추가/설정 변경 수준
     * MID: 신규 API 연동 또는 기존 로직 수정
     * HIGH: 원천사 신규 협의·계약, 복수 시스템 영향, 보안 심의 수반
     */
    private String estimatedComplexity;

    /**
     * 개발 진행 중 예상되는 리스크와 제약 사항.
     * 일정 리스크, 원천사 대응 지연, 정책 충돌, 기술 제약 등을 기술한다.
     */
    private String riskAndConstraints;
}
