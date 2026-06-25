package com.nice.qa.service.knowledge.dto;

import java.util.List;

/**
 * 동적폼 분기 트리. docs/catalog/catalog_category_tree_v1.yaml 이 ground truth.
 * 위저드 분기 메타(payment_methods, free_text, spec_match_hints, available_func_types)도
 * 함께 노출해 FE가 funcType/카테고리에 따라 동적 분기를 그릴 수 있게 한다.
 */
public record CategoryTree(
        List<FuncType> funcTypes,         // 1단계: 기능 구분
        List<CategoryNode> categories     // 2단계: 카테고리 → 세부유형
) {
    public record FuncType(
            String code,                  // NEW | MODIFY
            String label,
            String description
    ) {}

    public record CategoryNode(
            String code,
            String label,
            String inputMode,             // SELECT | FREE_TEXT
            List<SubType> subTypes
    ) {}

    public record SubType(
            String code,
            String label,
            List<String> paymentMethods,        // [] = 결제수단 매핑 없음
            boolean freeText,                   // true = "기타" 자유 입력 분기
            List<String> specMatchHints,        // F10 matchSpecDocs 후보 doc_id
            List<String> availableFuncTypes     // [] 또는 null = 둘 다 가용, 명시되면 그 funcType에서만
    ) {}
}
