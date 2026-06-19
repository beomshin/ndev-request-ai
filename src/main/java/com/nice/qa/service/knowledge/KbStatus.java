package com.nice.qa.service.knowledge;

// KB 학습/적재 현황. 대시보드 노출용 — 우리는 read-only.
public record KbStatus(
        long totalDocuments,
        long indexedDocuments,
        String lastUpdatedAt
) {
}
