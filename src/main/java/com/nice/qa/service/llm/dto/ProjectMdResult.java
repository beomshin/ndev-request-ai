package com.nice.qa.service.llm.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ProjectMdResult {

    /**
     * 기본 정보
     */
    private String author;                                  // 작성자
    private String createdDate;                             // 작성일
    private String department;                              // 작성 부서
    private String projectId;                               // 프로젝트 ID

    /**
     * 가맹점 정보
     */
    private String mid;                                     // MID (가맹점 ID)
    private String merchantName;                            // 가맹점 상호명
    private String merchantBusinessNumber;                  // 가맹점 사업자번호

    /**
     * 원천사 정보
     */
    private String providerName;                            // 원천사 상호명
    private String providerCollaborationBackground;         // 원천사 협업 배경

    /**
     * 추진 배경 및 목표
     */
    private String promotionBackground;                     // 추진 배경
    private String additionalInfo;                          // 추가 정보
    private String targetDate;                              // 목표 일정
    private String productName;                             // 제품명

    /**
     * 문제점 및 서비스 정보
     */
    private String issueAndImprovement;                     // 문제점 및 개선점
    private String issueVerificationMethod;                 // 문제를 확인하는 방법
    private String serviceChannelAndPaymentMethod;          // 서비스 제공 채널 / 결제 방식
    private String authApprovalAcquirerSubject;             // 인증/승인/매입 주체

    /**
     * 결제 정책
     */
    private String minimumPaymentAmount;                    // 최소 결제 금액 제한 (단위: 원)
    private String partialCancelAndRefundPolicy;            // 부분취소 및 환불 정책
    private String cashReceiptIssuer;                       // 현금영수증 발행 주체

    /**
     * 수익 및 기대효과
     */
    private String expectedRevenue;                         // 예상 수익
    private String expectedLoss;                            // 예상 손해
    private String expectedEffect;                          // 기대 효과

    /**
     * 이관 및 개발 정보
     */
    private String transferGuide;                           // 타 부서/제휴사 이관 가이드
    private Boolean developmentInProgress;                  // 개발 진행 여부
    private Boolean merchantRelatedDevelopment;             // 가맹점과 관계되는 개발 여부
    private Boolean providerRelatedDevelopment;             // 원천사와 관계되는 개발 여부
    private Boolean newServiceOrSelfImprovement;            // 신규 서비스 / 자체 개선 개발 여부
    private Boolean securityAndAuditDevelopment;            // 보안 및 감사 대응 개발 여부


}
