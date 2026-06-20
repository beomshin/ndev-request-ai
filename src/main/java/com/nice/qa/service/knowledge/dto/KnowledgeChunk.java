package com.nice.qa.service.knowledge.dto;

// KB 검색 결과 청크.
public record KnowledgeChunk(
        String id,
        String title,
        String content,
        String source,
        double score
) {
}
