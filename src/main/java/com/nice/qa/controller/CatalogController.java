package com.nice.qa.controller;

import com.nice.qa.service.knowledge.CategoryTree;
import com.nice.qa.service.knowledge.KnowledgeClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// FE 위저드 분기 트리 조회.
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final KnowledgeClient knowledgeClient;

    public CatalogController(KnowledgeClient knowledgeClient) {
        this.knowledgeClient = knowledgeClient;
    }

    @GetMapping
    public CategoryTree getCatalog() {
        return knowledgeClient.getCategoryTree();
    }
}
