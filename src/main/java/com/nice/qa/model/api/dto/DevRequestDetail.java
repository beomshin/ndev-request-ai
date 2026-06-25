package com.nice.qa.model.api.dto;

import com.nice.qa.entity.DevRequest;
import com.nice.qa.entity.DevRequestStatus;

import java.time.LocalDateTime;

/**
 * 상세 조회 응답. 본문 텍스트(MD/Diagram/details)까지 모두 포함.
 */
public record DevRequestDetail(
        Long id,
        String title,
        String categoryPath,
        DevRequestStatus status,
        String statusLabel,
        String author,
        String dept,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String details,
        String combinedMarkdown,
        String flowDiagram,
        String unconfirmedSection
) {
    public static DevRequestDetail from(DevRequest e) {
        return new DevRequestDetail(
                e.getId(),
                e.getTitle(),
                e.getCategoryPath(),
                e.getStatus(),
                e.getStatus() != null ? e.getStatus().getLabel() : null,
                e.getAuthor(),
                e.getDept(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getDetails(),
                e.getCombinedMarkdown(),
                e.getFlowDiagram(),
                e.getUnconfirmedSection()
        );
    }
}
