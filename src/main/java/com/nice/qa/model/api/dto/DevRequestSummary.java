package com.nice.qa.model.api.dto;

import com.nice.qa.entity.DevRequest;
import com.nice.qa.entity.DevRequestStatus;

import java.time.LocalDateTime;

/**
 * 개발요청서 목록 조회 응답 DTO (Summary Response DTO for DevRequest list).
 *
 * <p>목록 화면에서 각 요청서를 한 줄로 표시하기 위해 필요한 핵심 메타 정보만 담는다.
 * 본문({@code combinedMarkdown}), 다이어그램({@code flowDiagram}) 등 용량이 큰 텍스트 필드는
 * 의도적으로 제외하여 응답 크기를 최소화한다.</p>
 *
 * <p>상세 정보가 필요한 경우에는 {@link DevRequestDetail} 을 사용한다.</p>
 *
 * <p>This record contains only the lightweight metadata needed to render a single row
 * in the list view. Heavy body fields (Markdown document, flow diagram, etc.) are
 * intentionally omitted to keep the response payload small.
 * Use {@link DevRequestDetail} when the full body is required.</p>
 *
 * @param id          개발요청서 고유 식별자 (Unique primary key of the DevRequest entity)
 * @param title       요청서 제목 (Title of the development request)
 * @param categoryPath 카테고리 경로 (예: "결제창 / 카드")
 *                    (Category hierarchy path, e.g. "결제창 / 카드")
 * @param status      요청서 현재 상태 열거값 (Current status enum value)
 * @param statusLabel 상태 열거값의 한국어 표시 레이블 (Korean display label for the status enum)
 * @param author      작성자 이름 (Author's name)
 * @param dept        작성자 소속 부서 (Author's department)
 * @param createdAt   최초 생성 일시 (Record creation timestamp)
 * @param updatedAt   마지막 수정 일시 (Record last-updated timestamp)
 */
public record DevRequestSummary(

        /** 개발요청서 고유 식별자 (Primary key) */
        Long id,

        /** 요청서 제목 (Request title) */
        String title,

        /** 카테고리 계층 경로 — 슬래시(/) 구분 문자열, 예: "결제창 / 카드"
         *  (Category hierarchy path separated by slashes, e.g. "결제창 / 카드") */
        String categoryPath,

        /** 요청서 현재 상태 열거값 (Current status enum) */
        DevRequestStatus status,

        /** {@link DevRequestStatus} 열거값에 대응하는 한국어 표시 레이블.
         *  {@code status} 가 {@code null} 이면 이 필드도 {@code null} 이다.
         *  (Korean display label for the status enum.
         *  {@code null} when {@code status} is {@code null}.) */
        String statusLabel,

        /** 작성자 이름 (Author's name) */
        String author,

        /** 작성자 소속 부서명 (Author's department name) */
        String dept,

        /** 레코드 최초 생성 일시 (Record creation timestamp) */
        LocalDateTime createdAt,

        /** 레코드 마지막 수정 일시 (Record last-updated timestamp) */
        LocalDateTime updatedAt
) {
    /**
     * {@link DevRequest} 엔티티를 목록용 요약 DTO 로 변환한다 (Factory method).
     *
     * <p>본문 필드({@code details}, {@code combinedMarkdown}, {@code flowDiagram},
     * {@code unconfirmedSection})는 복사하지 않는다.
     * 상태({@code status})가 {@code null} 인 경우 {@code statusLabel} 도 {@code null} 로 설정된다.</p>
     *
     * <p>Converts a {@link DevRequest} JPA entity into this summary DTO.
     * Body fields are intentionally excluded to reduce payload size.
     * If {@code status} is {@code null}, {@code statusLabel} is also set to {@code null}.</p>
     *
     * @param e 변환 대상 {@link DevRequest} 엔티티 (Source DevRequest entity to convert)
     * @return 핵심 메타 필드만 복사된 {@link DevRequestSummary} 인스턴스
     *         (A new {@link DevRequestSummary} instance populated with core metadata fields only)
     */
    public static DevRequestSummary from(DevRequest e) {
        return new DevRequestSummary(
                e.getId(),
                e.getTitle(),
                e.getCategoryPath(),
                e.getStatus(),
                e.getStatus() != null ? e.getStatus().getLabel() : null,
                e.getAuthor(),
                e.getDept(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
