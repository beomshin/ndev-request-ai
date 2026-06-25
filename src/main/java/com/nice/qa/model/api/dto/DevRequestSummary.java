package com.nice.qa.model.api.dto;

import com.nice.qa.entity.DevRequest;
import com.nice.qa.entity.DevRequestStatus;

import java.time.LocalDateTime;

/**
 * 목록용 요약. 본문(MD/Diagram 등 무거운 텍스트)은 빼고 핵심만 넘겨준다.
 */
public record DevRequestSummary(
        Long id,
        String title,
        String categoryPath,
        DevRequestStatus status,
        String statusLabel,
        String author,
        String dept,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
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
