package com.nice.qa.controller;

import com.nice.qa.service.knowledge.KnowledgeClient;
import com.nice.qa.service.knowledge.dto.CategoryTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FE 위저드 분기 트리 조회.
 */
@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final KnowledgeClient knowledgeClient;

    @GetMapping("/")
    public CategoryTree getCatalog() {
        return knowledgeClient.getCategoryTree();
    }
}
