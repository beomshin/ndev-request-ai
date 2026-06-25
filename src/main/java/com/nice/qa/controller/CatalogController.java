package com.nice.qa.controller;

import com.nice.qa.service.knowledge.KnowledgeClient;
import com.nice.qa.service.knowledge.dto.CategoryTree;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FE 위저드 분기 트리(카탈로그) 조회 컨트롤러.
 *
 * <p>FE 위저드는 서비스 유형(예: 신규 연동, 기능 추가 등)에 따라
 * 표시할 슬라이드 순서와 입력 폼이 동적으로 달라진다.
 * 이 컨트롤러는 그 분기 기준이 되는 카테고리 트리 구조를 제공한다.
 *
 * <p>카테고리 트리는 {@link KnowledgeClient}를 통해 지식 저장소(knowledge_base)에서 로드하며,
 * 위저드 첫 화면 진입 시 한 번 호출된다.
 */
@Slf4j
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    /**
     * 지식 저장소 클라이언트 — 카테고리 트리 및 관련 메타데이터를 제공한다.
     * 파일 기반(로컬 MD 파일 스캔) 또는 외부 API 기반으로 구현될 수 있다.
     */
    private final KnowledgeClient knowledgeClient;

    /**
     * 위저드 분기 카테고리 트리 조회 (GET /api/catalog/).
     *
     * <p>반환되는 {@link CategoryTree}는 루트 카테고리와 하위 카테고리를 계층 구조로 담는다.
     * FE 위저드는 이 트리를 기반으로 사용자 선택값에 따라 동적으로 슬라이드를 분기한다.
     *
     * @return 위저드 분기에 사용되는 카테고리 계층 트리
     */
    @GetMapping("/")
    public CategoryTree getCatalog() {
        return knowledgeClient.getCategoryTree();
    }
}
