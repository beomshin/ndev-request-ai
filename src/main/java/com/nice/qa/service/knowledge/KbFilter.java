package com.nice.qa.service.knowledge;

// KB 검색 필터. 카테고리/세부유형이 null이면 미적용.
public record KbFilter(
        String category,
        String subType
) {
    public static KbFilter of(String category, String subType) {
        return new KbFilter(category, subType);
    }
}
