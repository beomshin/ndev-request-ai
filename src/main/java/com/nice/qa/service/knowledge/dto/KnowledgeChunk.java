package com.nice.qa.service.knowledge.dto;

/**
 * KB(지식저장소) RAG 검색 결과로 반환되는 문서 청크(Chunk) 불변 레코드.
 *
 * <p>RAG(Retrieval-Augmented Generation) 파이프라인에서 AI 응답 생성 시
 * 프롬프트 컨텍스트로 주입되는 단위 자료이다.
 * 하나의 원본 문서는 여러 청크로 분할될 수 있으며,
 * 각 청크는 독립적으로 검색·활용된다.</p>
 *
 * <p>Phase 0(스텁) 단계에서는 {@code score}가 항상 {@code 0.0}이며,
 * 실제 KB 어댑터 연동 후 유사도 기반 점수가 채워진다.</p>
 *
 * @see com.nice.qa.service.knowledge.KnowledgeClient#search(String, KbFilter)
 */
public record KnowledgeChunk(
        /**
         * 청크 고유 식별자.
         * 원본 문서 ID 기반으로 생성되며, 동일 문서의 청크는 접미사로 구분될 수 있다.
         */
        String id,

        /**
         * 청크가 속한 원본 문서의 제목.
         * UI에서 출처 표시 또는 참고 문서 링크에 활용된다.
         */
        String title,

        /**
         * 청크 본문 텍스트.
         * AI 프롬프트 컨텍스트로 주입되는 실제 내용이며,
         * 최대 글자 수는 호출측({@link com.nice.qa.service.knowledge.StubKnowledgeClient})에서 제한한다.
         */
        String content,

        /**
         * 청크의 출처 경로.
         * classpath 상대 경로 또는 외부 URL 형태로 제공된다.
         */
        String source,

        /**
         * 쿼리와의 유사도 점수 (0.0 ~ 1.0 범위 권장).
         * 값이 클수록 쿼리와 관련성이 높으며, 결과는 이 점수 내림차순으로 정렬된다.
         * Phase 0 스텁에서는 항상 {@code 0.0}으로 반환된다.
         */
        double score
) {
}
