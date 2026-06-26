package com.nice.qa.service.knowledge.dto;

/**
 * KB(지식저장소) 학습·적재 현황을 나타내는 불변 레코드.
 *
 * <p>대시보드 또는 관리 화면에서 KB의 전반적인 상태를 표시하는 데 사용된다.
 * 이 시스템(요청서 AI 서비스)은 KB를 <strong>읽기 전용(read-only)</strong>으로 조회하며,
 * 현황 데이터를 수정하거나 적재하지 않는다.</p>
 *
 * @see com.nice.qa.service.knowledge.KnowledgeClient#getStatus()
 */
public record KbStatus(
        /**
         * KB에 등록된 전체 문서 수.
         * 아직 벡터 인덱싱이 완료되지 않은 문서도 포함한다.
         */
        long totalDocuments,

        /**
         * 벡터 DB 인덱싱이 완료된 문서 수.
         * RAG 검색 시 실제로 활용 가능한 문서 수를 의미한다.
         */
        long indexedDocuments,

        /**
         * KB 데이터의 마지막 업데이트 일시 문자열.
         * ISO-8601 형식 권장 (예: {@code "2024-01-15T10:30:00"}),
         * 정보가 없으면 빈 문자열
         */
        String lastUpdatedAt
) {
}
