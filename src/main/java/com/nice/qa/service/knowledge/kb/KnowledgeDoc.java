package com.nice.qa.service.knowledge.kb;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 지식저장소({@code knowledge_base}) 폴더의 마크다운 문서 1건을 나타내는 불변 레코드.
 *
 * <p>YAML 프론트매터에서 추출된 구조화된 메타데이터 필드와,
 * 프론트매터를 제외한 마크다운 본문({@link #markdown})이 분리되어 저장된다.</p>
 *
 * <h2>필드-메타 매핑 (Field-to-Frontmatter Mapping)</h2>
 * <pre>
 * doc_id        → id
 * title         → title
 * category      → category
 * version       → version
 * last_updated  → lastUpdated
 * status        → status
 * file_size     → fileSize
 * chunk_count   → chunkCount
 * </pre>
 *
 * <p>위 매핑 외의 프론트매터 항목은 {@link #meta}({@link JsonNode})에 원본 그대로 보관되어
 * 추가 필드 접근 시 사용할 수 있다.</p>
 *
 * @see KnowledgeBaseService
 */
public record KnowledgeDoc(
        /** 문서 고유 식별자. 프론트매터의 {@code doc_id} 값; 없으면 파일명(확장자 제외)을 사용 */
        String id,

        /** 문서 제목. 프론트매터의 {@code title} 값; 없으면 파일명으로 대체 */
        String title,

        /**
         * 문서 카테고리.
         * 예: "원천사 규격", "결제창", "WEB API"
         */
        String category,        // 원천사 규격 / 결제창 / WEB API

        /** 문서 버전 (예: "1.0.0"). 프론트매터에 없으면 {@code null} */
        String version,

        /** 마지막 업데이트 날짜 문자열 (예: "2024-01-15"). 프론트매터에 없으면 {@code null} */
        String lastUpdated,

        /**
         * 문서 상태.
         * 예: "active", "deprecated", "draft"
         */
        String status,

        /** 파일 크기 문자열 (예: "12KB"). 프론트매터에 없으면 {@code null} */
        String fileSize,

        /**
         * 벡터 DB 청크 분할 수.
         * 프론트매터에 {@code chunk_count}가 없으면 {@code null}
         */
        Integer chunkCount,

        /** 원본 파일명 (확장자 포함, 예: "kakaopay_spec.md") */
        String filename,

        /**
         * YAML 프론트매터 전체를 Jackson {@link JsonNode}로 보관.
         * 위 구조화 필드에 매핑되지 않은 추가 메타데이터 접근에 사용
         */
        JsonNode meta,

        /** 프론트매터({@code --- ... ---})를 제외한 마크다운 본문 전체 */
        String markdown
) {
}
