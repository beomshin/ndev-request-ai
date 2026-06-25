package com.nice.qa.service.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.nice.qa.service.knowledge.dto.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 분기 트리 시드 + classpath:docs/ 기반 mock 데이터 반환.
 * 실제 KB 어댑터가 들어오기 전까지의 자리채움.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "knowledge.provider", havingValue = "stub", matchIfMissing = true)
@RequiredArgsConstructor
public class StubKnowledgeClient implements KnowledgeClient {

    private static final int CHUNK_LIMIT = 5;
    private static final int CONTENT_MAX_CHARS = 800;

    private static final String CATALOG_YAML_PATH = "docs/catalog/catalog_category_tree_v1.yaml";

    private final DocRepository docRepository;

    /** 부팅 시 yaml을 한 번 파싱해 메모리에 보관. classpath 자원은 런타임 변경 불가라 캐싱 안전. */
    private CategoryTree cachedCategoryTree;

    @PostConstruct
    void loadCatalog() {
        try (InputStream is = new ClassPathResource(CATALOG_YAML_PATH).getInputStream()) {
            JsonNode root = new YAMLMapper().readTree(is);
            this.cachedCategoryTree = new CategoryTree(
                    parseFuncTypes(root.path("func_types")),
                    parseCategories(root.path("categories"))
            );
            log.info("[KB] catalog yaml 로드 완료 — funcTypes={}, categories={}",
                    cachedCategoryTree.funcTypes().size(),
                    cachedCategoryTree.categories().size());
        } catch (IOException e) {
            throw new IllegalStateException("catalog yaml 로드 실패: " + CATALOG_YAML_PATH, e);
        }
    }

    @Override
    public CategoryTree getCategoryTree() {
        return cachedCategoryTree;
    }

    private static List<CategoryTree.FuncType> parseFuncTypes(JsonNode arr) {
        List<CategoryTree.FuncType> result = new ArrayList<>();
        if (!arr.isArray()) return result;
        for (JsonNode n : arr) {
            result.add(new CategoryTree.FuncType(
                    n.path("id").asText(),
                    n.path("label").asText(),
                    n.path("description").asText("")
            ));
        }
        return result;
    }

    private static List<CategoryTree.CategoryNode> parseCategories(JsonNode arr) {
        List<CategoryTree.CategoryNode> result = new ArrayList<>();
        if (!arr.isArray()) return result;
        for (JsonNode c : arr) {
            List<CategoryTree.SubType> subs = new ArrayList<>();
            JsonNode subArr = c.path("sub_types");
            if (subArr.isArray()) {
                for (JsonNode s : subArr) {
                    subs.add(new CategoryTree.SubType(
                            s.path("id").asText(),
                            s.path("label").asText(),
                            toStringList(s.path("payment_methods")),
                            s.path("free_text").asBoolean(false),
                            toStringList(s.path("spec_match_hints")),
                            toStringList(s.path("available_func_types"))
                    ));
                }
            }
            result.add(new CategoryTree.CategoryNode(
                    c.path("id").asText(),
                    c.path("label").asText(),
                    c.path("input_mode").asText(""),
                    subs
            ));
        }
        return result;
    }

    private static List<String> toStringList(JsonNode arr) {
        if (!arr.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        arr.forEach(n -> result.add(n.asText()));
        return result;
    }

    @Override
    public List<KnowledgeChunk> search(String query, KbFilter filter) {
        // 쿼리 + 카테고리/세부유형을 키워드로 합쳐 docs/* 전체에서 검색
        List<String> keywords = DocRepository.tokenize(query, filter.category(), filter.subType());
        return docRepository.search(keywords, null, CHUNK_LIMIT).stream()
                .map(this::toChunk)
                .toList();
    }

    @Override
    public List<SpecDocRef> matchSpecDocs(String category, String subType) {
        // 규격서 매칭: docs/spec/* 와 docs/provider/* 중 카테고리/세부유형 키워드 매칭 상위 N개
        List<String> keywords = DocRepository.tokenize(category, subType);

        List<DocRepository.KnowledgeDoc> specHits = docRepository.search(keywords, "spec", 3);
        List<DocRepository.KnowledgeDoc> providerHits = docRepository.search(keywords, "provider", 3);

        // 매칭 결과가 비면 폴더 전체를 기본 후보로 반환 — 사용자에게 "관련 자료 풀"을 보여주는 효과
        if (specHits.isEmpty() && providerHits.isEmpty()) {
            specHits = docRepository.byFolder("spec");
            providerHits = docRepository.byFolder("provider");
        }

        return java.util.stream.Stream.concat(specHits.stream(), providerHits.stream())
                .map(this::toSpecRef)
                .toList();
    }

    @Override
    public List<PastRequestRef> findSimilarRequests(SimilarQuery query) {
        // 현 단계엔 KB에 적재된 과거 요청 데이터가 없어 비어있는 게 정상.
        // 다만 흐름 검증용으로 templates 폴더의 양식 1건을 "유사 사례"로 보여준다.
        return docRepository.byFolder("templates").stream()
                .limit(1)
                .map(d -> new PastRequestRef(
                        d.id(),
                        d.title(),
                        DocRepository.excerpt(d.body(), 200),
                        0.0,
                        d.meta().getOrDefault("last_updated", "")
                ))
                .toList();
    }

    @Override
    public KbStatus getStatus() {
        return new KbStatus(docRepository.size(), docRepository.size(), nullSafe(docRepository.latestUpdate()));
    }

    private KnowledgeChunk toChunk(DocRepository.KnowledgeDoc d) {
        return new KnowledgeChunk(
                d.id(),
                d.title(),
                DocRepository.excerpt(d.body(), CONTENT_MAX_CHARS),
                d.path(),
                0.0 // Phase 0 stub에서는 점수 의미 없음 — 정렬은 DocRepository 내부에서 끝남
        );
    }

    private SpecDocRef toSpecRef(DocRepository.KnowledgeDoc d) {
        return new SpecDocRef(
                d.id(),
                d.title(),
                d.meta().getOrDefault("reference_url", d.path()),
                d.meta().getOrDefault("version", "")
        );
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
