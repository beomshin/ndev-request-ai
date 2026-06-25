package com.nice.qa.service.knowledge.kb;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * knowledge_base 폴더의 md 문서 1건. frontmatter는 {@link #meta}에 통째로(JSON으로 변환),
 * body는 {@link #markdown}에 분리 저장.
 */
public record KnowledgeDoc(
        String id,
        String title,
        String category,        // 원천사 규격 / 결제창 / WEB API
        String version,
        String lastUpdated,
        String status,
        String fileSize,
        Integer chunkCount,
        String filename,
        JsonNode meta,
        String markdown
) {
}
