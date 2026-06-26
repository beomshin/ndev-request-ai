package com.nice.qa.service.knowledge.dto;

/**
 * KB(지식저장소) 검색 시 적용하는 필터 조건을 담는 불변 레코드.
 *
 * <p>카테고리 및 세부유형 중 {@code null}인 필드는 필터 조건에서 제외되어
 * 해당 차원에 대한 제한 없이 전체 범위를 검색한다.</p>
 *
 * <h2>사용 예시 (Usage Example)</h2>
 * <pre>{@code
 * // 카테고리만 지정하고 세부유형 제한 없이 검색
 * KbFilter filter = KbFilter.of("payment_window", null);
 *
 * // 카테고리와 세부유형 모두 지정
 * KbFilter filter = KbFilter.of("payment_window", "kakaopay");
 * }</pre>
 *
 * @see com.nice.qa.service.knowledge.KnowledgeClient#search(String, KbFilter)
 */
public record KbFilter(
        /**
         * 검색을 제한할 카테고리 코드.
         * {@code null}이면 카테고리 필터 미적용 (전체 카테고리 검색)
         */
        String category,

        /**
         * 검색을 제한할 세부유형 코드.
         * {@code null}이면 세부유형 필터 미적용 (전체 세부유형 검색)
         */
        String subType
) {

    /**
     * 카테고리와 세부유형을 지정하여 {@link KbFilter}를 생성하는 팩토리 메서드.
     *
     * <p>두 인자 모두 {@code null}을 허용하며, {@code null}인 경우 해당 필터 조건이 적용되지 않는다.</p>
     *
     * @param category 카테고리 코드 (null 허용)
     * @param subType  세부유형 코드 (null 허용)
     * @return 생성된 {@link KbFilter} 인스턴스
     */
    public static KbFilter of(String category, String subType) {
        return new KbFilter(category, subType);
    }
}
