package com.nice.qa.service.knowledge.dto;

import java.util.List;

// 동적폼 분기 트리. KB에 저장되어 상시 수정 가능 — 우리는 조회만.
public record CategoryTree(
        List<FuncType> funcTypes,         // 1단계: 기능 구분
        List<CategoryNode> categories     // 2단계: 카테고리 → 세부유형
) {
    public record FuncType(String code, String label) {
    }

    public record CategoryNode(String code, String label, List<SubType> subTypes) {
    }

    public record SubType(String code, String label) {
    }
}
