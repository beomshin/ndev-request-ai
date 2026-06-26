package com.nice.qa.service.knowledge.dto;

/**
 * F11 기능 — 유사 과거 요청 참조 정보를 담는 불변 레코드.
 *
 * <p>현재 요청서와 유사한 과거 완료 요청을 KB에서 검색한 결과를 표현한다.
 * 설계자가 과거 사례를 참고하여 요청서를 작성하는 데 활용된다.</p>
 *
 * <h2>Phase 0 제약 (Phase 0 Constraints)</h2>
 * <p>현 단계에는 KB에 적재된 과거 요청 데이터가 없으므로
 * {@link com.nice.qa.service.knowledge.StubKnowledgeClient}에서는
 * {@code templates} 폴더 문서를 대체 데이터로 반환하며, {@code similarity}는 항상 {@code 0.0}이다.</p>
 *
 * @see com.nice.qa.service.knowledge.KnowledgeClient#findSimilarRequests(SimilarQuery)
 */
public record PastRequestRef(
        /**
         * 과거 요청의 고유 식별자.
         * KB 내부 문서 ID 또는 요청서 번호에 해당한다.
         */
        String id,

        /** 과거 요청의 제목 (예: "카카오페이 결제창 신규 연동") */
        String title,

        /**
         * 과거 요청의 요약 미리보기 텍스트.
         * 본문을 일정 글자 수로 잘라 제공되며, 목록 화면에서 내용 파악에 활용된다.
         */
        String summary,

        /**
         * 현재 요청과의 유사도 점수 (0.0 ~ 1.0 범위 권장).
         * 값이 클수록 현재 요청과 더 유사하다.
         * Phase 0 스텁에서는 항상 {@code 0.0}으로 반환된다.
         */
        double similarity,

        /**
         * 과거 요청 생성 일시 문자열.
         * ISO-8601 형식 권장 (예: {@code "2024-01-15"}),
         * 정보가 없으면 빈 문자열
         */
        String createdAt
) {
}
