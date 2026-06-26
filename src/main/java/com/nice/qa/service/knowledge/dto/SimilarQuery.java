package com.nice.qa.service.knowledge.dto;

import java.util.List;

/**
 * F11 유사 과거 요청 검색({@link com.nice.qa.service.knowledge.KnowledgeClient#findSimilarRequests}) 입력 파라미터 레코드.
 *
 * <p>현재 작성 중인 요청서의 기능 구분·카테고리·세부유형·키워드를 조합하여
 * KB에서 유사 과거 요청을 찾는 데 사용된다.</p>
 *
 * <p>필드가 {@code null}이거나 비어 있으면 해당 조건은 검색에서 제외된다.
 * 모든 필드가 {@code null}/빈 값이면 KB 구현체에 따라 전체 검색 또는 빈 결과를 반환할 수 있다.</p>
 *
 * @see com.nice.qa.service.knowledge.KnowledgeClient#findSimilarRequests(SimilarQuery)
 * @see PastRequestRef
 */
public record SimilarQuery(
        /**
         * 기능 구분 코드.
         * 예: {@code "NEW"} (신규 연동 개발), {@code "MODIFY"} (기존 기능 수정)
         */
        String funcType,

        /**
         * 카테고리 코드.
         * 예: {@code "payment_window"}, {@code "web_api"}
         */
        String category,

        /**
         * 세부유형 코드.
         * 예: {@code "kakaopay"}, {@code "naverpay"}
         */
        String subType,

        /**
         * 추가 검색 키워드 목록.
         * 요청서 제목이나 본문에서 추출한 주요 키워드를 포함한다.
         * {@code null} 또는 빈 리스트이면 키워드 조건 미적용
         */
        List<String> keywords
) {
}
