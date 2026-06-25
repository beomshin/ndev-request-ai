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
 * 개발요청서 영속 엔티티 (Development Request JPA Entity).
 *
 * <p>저장 흐름: 사용자가 위저드(Wizard) 또는 {@code /result} 화면에서 [저장]을 누를 때
 * 한 건이 DB에 삽입된다. AI 생성(generate)은 무저장 초안 상태이며,
 * 저장(save)은 별도의 명시적 단계로 분리되어 있다 — 두 기능은 서로 다른 엔드포인트를 사용한다.
 *
 * <p>소프트 삭제 (Soft Delete): {@code @SQLDelete} 어노테이션이 SQL DELETE 문을
 * UPDATE 문으로 대체하여 {@code deleted} 플래그를 {@code true}로 설정한다.
 * Repository 쿼리에는 {@code deleted = false} 조건이 항상 포함되어 삭제된 데이터를 숨긴다.
 *
 * <p>테이블명: {@code dev_request}
 * 인덱스: status, created_at, deleted 컬럼에 각각 인덱스가 설정되어 목록 조회 성능을 보장한다.
 */
@Entity
@Table(name = "dev_request",
        indexes = {
                // 상태별 필터링 조회 성능을 위한 인덱스 (index for status-based filtering)
                @Index(name = "ix_dev_request_status", columnList = "status"),
                // 생성일 기준 정렬·범위 조회 성능을 위한 인덱스 (index for date-range ordering)
                @Index(name = "ix_dev_request_created", columnList = "created_at"),
                // 소프트 삭제 필터링 성능을 위한 인덱스 (index for soft-delete filtering)
                @Index(name = "ix_dev_request_deleted", columnList = "deleted")
        })
@EntityListeners(AuditingEntityListener.class) // @CreatedDate / @LastModifiedDate 자동 채움 활성화
@SQLDelete(sql = "UPDATE dev_request SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
// DELETE 호출 시 실제 행을 삭제하지 않고 deleted = true 로 업데이트 (soft delete)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// JPA 프록시 생성을 위해 기본 생성자를 PROTECTED로 제한 (required by JPA spec, kept non-public to enforce factory method usage)
public class DevRequest {

    /**
     * 자동 증가 기본키 (Auto-increment primary key).
     * DB 시퀀스/IDENTITY 전략으로 생성되며 애플리케이션이 직접 할당하지 않는다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 화면에 노출되는 요청서 제목 (Display title shown in the list/detail UI).
     * 보통 productName 또는 serviceName 값으로 채워진다.
     * 최대 300자, NOT NULL.
     */
    @Column(nullable = false, length = 300)
    private String title;

    /**
     * 기능 분류 경로 (Function category path).
     * 위저드에서 선택한 funcType / category / subType 을 슬래시({@code /})로 연결한 합성 문자열.
     * 예: {@code "결제창 / 카드"}, {@code "정산 / 매출 / 일별"}
     * Repository의 {@code categoryStartsWith} Spec에서 부분 일치 검색에 사용된다.
     */
    @Column(name = "category_path", length = 300)
    private String categoryPath;

    /**
     * 요청서의 현재 처리 상태 (Current processing status of the request).
     * {@link DevRequestStatus} 열거형을 문자열로 저장한다 (EnumType.STRING).
     * NOT NULL.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DevRequestStatus status;

    /**
     * 요청서 작성자 이름 (Name of the person who created the request).
     * 최대 100자, nullable.
     */
    @Column(length = 100)
    private String author;

    /**
     * 작성자 소속 부서명 (Department of the author).
     * 최대 100자, nullable.
     */
    @Column(name = "dept", length = 100)
    private String dept;

    /**
     * 엔티티 최초 생성 일시 (Timestamp when the entity was first persisted).
     * {@code @CreatedDate}로 자동 설정되며 이후 수정 불가 ({@code updatable = false}).
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티 마지막 수정 일시 (Timestamp of the most recent update).
     * {@code @LastModifiedDate}로 자동 갱신된다.
     * 소프트 삭제 시 {@code @SQLDelete} SQL에서도 직접 갱신된다.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 위저드 입력 원본 데이터를 담은 자유 형식 JSON 문자열 (Raw wizard input as a JSON string).
     * 요청서 재현 및 감사(audit) 목적으로 원본 그대로 보관한다.
     * CLOB 타입으로 저장되어 길이 제한이 없다.
     */
    @Lob
    @Column(name = "details", columnDefinition = "CLOB")
    private String details;

    /**
     * Gemini AI가 생성한 종합 마크다운 문서 (AI-generated combined Markdown document).
     * 표준 양식 v1.6 기준으로 작성된 본문이며, Confluence/SHARE 붙여넣기 대상이다.
     * CLOB 타입으로 저장된다.
     */
    @Lob
    @Column(name = "combined_markdown", columnDefinition = "CLOB")
    private String combinedMarkdown;

    /**
     * 플로우 다이어그램 텍스트 소스 (Text source for the flow diagram).
     * mxGraph XML 또는 PlantUML 소스 문자열을 저장하며, 렌더링된 이미지가 아닌 소스를 보관한다.
     * 프론트엔드에서 소스를 받아 다이어그램으로 변환·렌더링한다.
     * CLOB 타입으로 저장된다.
     */
    @Lob
    @Column(name = "flow_diagram", columnDefinition = "CLOB")
    private String flowDiagram;

    /**
     * 미확인·추가 확인 필요 항목 섹션 (Section listing items that need further clarification).
     * 위저드 입력 중 "잘 모름" 또는 "추가 확인 필요"로 표시된 항목을
     * JSON 배열 또는 마크다운 목록 형태로 보관한다.
     * CLOB 타입으로 저장된다.
     */
    @Lob
    @Column(name = "unconfirmed_section", columnDefinition = "CLOB")
    private String unconfirmedSection;

    /**
     * 소프트 삭제 플래그 (Soft-delete flag).
     * {@code true}이면 논리적으로 삭제된 레코드이며 일반 조회에서 제외된다.
     * {@code @SQLDelete}가 DELETE 실행 시 이 값을 {@code true}로 설정한다.
     * 기본값은 {@code false} (삭제되지 않은 상태).
     */
    @Column(nullable = false)
    private boolean deleted = false;

    /**
     * 정적 팩토리 메서드 — 모든 필드를 명시적으로 전달받아 엔티티를 생성한다
     * (Static factory method to create a fully populated DevRequest entity).
     *
     * <p>기본 생성자가 PROTECTED이므로 외부에서는 이 메서드를 통해서만 인스턴스를 생성한다.
     * {@code status}가 {@code null}인 경우 {@link DevRequestStatus#DRAFT}를 기본값으로 사용한다.
     *
     * @param title              화면 노출 제목 (display title)
     * @param categoryPath       기능 분류 경로, 슬래시 구분 (slash-separated category path)
     * @param status             초기 상태; null이면 DRAFT로 설정 (initial status; defaults to DRAFT if null)
     * @param author             작성자 이름 (author name)
     * @param dept               작성자 소속 부서 (author's department)
     * @param details            위저드 입력 원본 JSON (raw wizard input JSON)
     * @param combinedMarkdown   AI 생성 종합 마크다운 (AI-generated combined markdown)
     * @param flowDiagram        플로우 다이어그램 소스 (flow diagram source text)
     * @param unconfirmedSection 미확인 항목 섹션 (unconfirmed items section)
     * @return 새로 생성된 {@link DevRequest} 인스턴스 (newly created entity instance)
     */
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
        // status가 null이면 기본값 DRAFT 적용 (fall back to DRAFT when status is not provided)
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
