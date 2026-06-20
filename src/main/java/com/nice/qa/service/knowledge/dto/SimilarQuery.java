package com.nice.qa.service.knowledge.dto;

import java.util.List;

// 유사 요청 검색 입력.
public record SimilarQuery(
        String funcType,
        String category,
        String subType,
        List<String> keywords
) {
}
