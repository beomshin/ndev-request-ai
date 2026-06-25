package com.nice.qa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 개발요청서 영속 엔티티.
 *
 * <p>저장 흐름: 사용자가 위저드/`/result`에서 [저장]을 누를 때 한 건이 들어온다.
 * 생성(generate)은 무저장 초안 → 저장(save)은 명시적 단계. 둘은 별도 엔드포인트.
 *
 * <p>소프트 삭제: {@code @SQLDelete} 로 DELETE를 UPDATE로 갈아끼우고,
 * Repository 쿼리에서 `deleted = false` 조건을 항상 추가해 가린다.
 */
@Entity
@Table(name = "dev_request",
        indexes = {
                @Index(name = "ix_dev_request_status", columnList = "status"),
                @Index(name = "ix_dev_request_created", columnList = "created_at"),
                @Index(name = "ix_dev_request_deleted", columnList = "deleted")
        })
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE dev_request SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DevRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 화면에 노출되는 제목 (보통 productName/serviceName) */
    @Column(nullable = false, length = 300)
    private String title;

    /** "결제창 / 카드" 같은 분류 경로 — funcType / category / subType 을 슬래시로 합성 */
    @Column(name = "category_path", length = 300)
    private String categoryPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DevRequestStatus status;

    @Column(length = 100)
    private String author;

    @Column(name = "dept", length = 100)
    private String dept;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 위저드 입력 원본 등 자유 형식 JSON 문자열 (재현/감사용) */
    @Lob
    @Column(name = "details", columnDefinition = "CLOB")
    private String details;

    /** Gemini가 만든 종합 MD (표준 양식 v1.6) — SHARE 붙여넣기 대상 본문 */
    @Lob
    @Column(name = "combined_markdown", columnDefinition = "CLOB")
    private String combinedMarkdown;

    /** mxGraph XML 또는 PlantUML 소스 (이미지 자체가 아닌 텍스트 소스를 보관) */
    @Lob
    @Column(name = "flow_diagram", columnDefinition = "CLOB")
    private String flowDiagram;

    /** 위저드가 모은 [잘 모름/추가 확인] 항목 JSON 또는 MD 섹션 */
    @Lob
    @Column(name = "unconfirmed_section", columnDefinition = "CLOB")
    private String unconfirmedSection;

    /** 소프트 삭제 플래그 — @SQLDelete가 true로 세팅 */
    @Column(nullable = false)
    private boolean deleted = false;

    public static DevRequest of(
            String title,
            String categoryPath,
            DevRequestStatus status,
            String author,
            String dept,
            String details,
            String combinedMarkdown,
            String flowDiagram,
            String unconfirmedSection
    ) {
        DevRequest e = new DevRequest();
        e.title = title;
        e.categoryPath = categoryPath;
        e.status = status != null ? status : DevRequestStatus.DRAFT;
        e.author = author;
        e.dept = dept;
        e.details = details;
        e.combinedMarkdown = combinedMarkdown;
        e.flowDiagram = flowDiagram;
        e.unconfirmedSection = unconfirmedSection;
        return e;
    }
}
