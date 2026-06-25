package com.nice.qa.controller;

import com.nice.qa.service.knowledge.kb.KnowledgeBaseService;
import com.nice.qa.service.knowledge.kb.KnowledgeDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 지식 저장소 화면 백엔드.
 * docs/knowledge_base/{provider,ui,webapi} 폴더의 md 문서 메타·본문을 노출.
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService service;

    /** 목록 — 본문 markdown은 제외하고 표시용 메타만. */
    @GetMapping
    public List<KnowledgeSummary> list() {
        return service.list().stream().map(KnowledgeSummary::from).toList();
    }

    /**
     * 단건 — frontmatter 전체 + 본문 markdown.
     * doc_id에 점이 포함되어 있어 Spring의 path variable 끝 부분이 잘리지 않도록 정규식으로 ".+"를 허용.
     */
    @GetMapping("/{id:.+}")
    public ResponseEntity<KnowledgeDoc> get(@PathVariable String id) {
        return service.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record KnowledgeSummary(
            String id,
            String title,
            String category,
            String version,
            String lastUpdated,
            String status,
            String fileSize,
            Integer chunkCount,
            String filename
    ) {
        static KnowledgeSummary from(KnowledgeDoc d) {
            return new KnowledgeSummary(
                    d.id(), d.title(), d.category(), d.version(), d.lastUpdated(),
                    d.status(), d.fileSize(), d.chunkCount(), d.filename()
            );
        }
    }
}
