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
 * {@link KnowledgeClient}의 스텁(Stub) 구현체.
 *
 * <p>실제 KB(지식저장소) 어댑터가 완성되기 전까지 사용하는 자리채움 구현체다.
 * 분기 트리 시드 데이터는 {@code classpath:docs/catalog/catalog_category_tree_v1.yaml}에서,
 * 문서 검색 결과는 {@link DocRepository}가 관리하는 {@code classpath:docs/} 하위 마크다운 파일에서 반환한다.</p>
 *
 * <h2>활성화 조건 (Activation Condition)</h2>
 * <p>{@code application.properties}(또는 YAML)에 {@code knowledge.provider=stub}으로 설정하거나,
 * 해당 속성이 없을 때(matchIfMissing=true) 기본으로 활성화된다.</p>
 *
 * <h2>제약 사항 (Constraints)</h2>
 * <ul>
 *   <li>검색 점수({@code score})는 항상 0.0 — Phase 0에서는 {@link DocRepository}의 내부 정렬로 대체</li>
 *   <li>classpath 자원은 런타임에 변경 불가이므로 부팅 시 1회 캐싱이 안전함</li>
 * </ul>
 *
 * @see KnowledgeClient
 * @see DocRepository
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "knowledge.provider", havingValue = "stub", matchIfMissing = true)
@RequiredArgsConstructor
public class StubKnowledgeClient implements KnowledgeClient {

    /** RAG 검색 결과로 반환할 최대 청크 수 */
    private static final int CHUNK_LIMIT = 5;

    /** 청크 본문 최대 글자 수 — 초과 시 말줄임(...) 처리 */
    private static final int CONTENT_MAX_CHARS = 800;

    /** 분기 트리를 담고 있는 YAML 파일의 classpath 상대 경로 */
    private static final String CATALOG_YAML_PATH = "docs/catalog/catalog_category_tree_v1.yaml";

    /** classpath:docs/ 하위 마크다운 파일을 메모리에 보관하는 문서 저장소 */
    private final DocRepository docRepository;

    /**
     * 부팅 시 YAML을 한 번 파싱해 메모리에 보관하는 분기 트리 캐시.
     * classpath 자원은 런타임에 변경되지 않으므로 캐싱이 안전하다.
     */
    private CategoryTree cachedCategoryTree;

    /**
     * 애플리케이션 기동 시 분기 트리 YAML을 파싱하여 {@link #cachedCategoryTree}에 저장한다.
     *
     * <p>YAML 파일을 읽어 {@code func_types} 배열과 {@code categories} 배열을 각각
     * {@link CategoryTree.FuncType} 및 {@link CategoryTree.CategoryNode} 리스트로 변환한다.</p>
     *
     * @throws IllegalStateException YAML 파일을 읽거나 파싱하는 데 실패한 경우
     */
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

    /**
     * 부팅 시 캐싱된 분기 트리를 반환한다.
     *
     * @return 캐싱된 {@link CategoryTree} 인스턴스 (부팅 후 항상 non-null)
     */
    @Override
    public CategoryTree getCategoryTree() {
        return cachedCategoryTree;
    }

    /**
     * YAML {@code func_types} 배열 노드를 {@link CategoryTree.FuncType} 리스트로 변환한다.
     *
     * @param arr YAML에서 파싱된 func_types 배열 노드
     * @return 기능 구분 목록 (배열이 아닌 경우 빈 리스트 반환)
     */
    private static List<CategoryTree.FuncType> parseFuncTypes(JsonNode arr) {
        List<CategoryTree.FuncType> result = new ArrayList<>();
        // 배열 노드가 아닌 경우(누락 또는 잘못된 구조) 빈 리스트를 즉시 반환
        if (!arr.isArray()) return result;
        for (JsonNode n : arr) {
            result.add(new CategoryTree.FuncType(
                    n.path("id").asText(),
                    n.path("label").asText(),
                    n.path("description").asText("") // description은 선택 항목 — 없으면 빈 문자열
            ));
        }
        return result;
    }

    /**
     * YAML {@code categories} 배열 노드를 {@link CategoryTree.CategoryNode} 리스트로 변환한다.
     *
     * <p>각 카테고리 노드 하위의 {@code sub_types} 배열도 재귀 없이 직접 파싱한다.</p>
     *
     * @param arr YAML에서 파싱된 categories 배열 노드
     * @return 카테고리 노드 목록 (배열이 아닌 경우 빈 리스트 반환)
     */
    private static List<CategoryTree.CategoryNode> parseCategories(JsonNode arr) {
        List<CategoryTree.CategoryNode> result = new ArrayList<>();
        // 배열 노드가 아닌 경우 빈 리스트를 즉시 반환
        if (!arr.isArray()) return result;
        for (JsonNode c : arr) {
            List<CategoryTree.SubType> subs = new ArrayList<>();
            JsonNode subArr = c.path("sub_types");
            if (subArr.isArray()) {
                for (JsonNode s : subArr) {
                    subs.add(new CategoryTree.SubType(
                            s.path("id").asText(),
                            s.path("label").asText(),
                            toStringList(s.path("payment_methods")),       // 결제수단 목록
                            s.path("free_text").asBoolean(false),          // 기타(자유입력) 분기 여부
                            toStringList(s.path("spec_match_hints")),      // F10 규격 매칭 힌트 doc_id 목록
                            toStringList(s.path("available_func_types"))   // 허용된 기능유형 목록
                    ));
                }
            }
            result.add(new CategoryTree.CategoryNode(
                    c.path("id").asText(),
                    c.path("label").asText(),
                    c.path("input_mode").asText(""), // input_mode 누락 시 빈 문자열로 기본값 처리
                    subs
            ));
        }
        return result;
    }

    /**
     * JSON 배열 노드를 문자열 리스트로 변환하는 유틸리티 메서드.
     *
     * @param arr JSON 배열 노드
     * @return 각 요소의 텍스트 값 리스트 (배열이 아닌 경우 빈 불변 리스트 반환)
     */
    private static List<String> toStringList(JsonNode arr) {
        // 배열 노드가 아닌 경우(null 노드, 단일 값 등) 빈 불변 리스트 반환
        if (!arr.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        arr.forEach(n -> result.add(n.asText()));
        return result;
    }

    /**
     * RAG 검색: 쿼리와 필터 조건을 키워드로 토큰화하여 {@link DocRepository}에서 검색한다.
     *
     * <p>쿼리 문자열, 카테고리, 세부유형을 공백·쉼표·슬래시 기준으로 분리한 뒤
     * {@code docs/} 전체 문서에서 키워드 매칭 점수 상위 {@link #CHUNK_LIMIT}건을 반환한다.</p>
     *
     * @param query  자연어 검색 쿼리
     * @param filter 카테고리·세부유형 필터
     * @return 관련 지식 청크 목록 (최대 {@value #CHUNK_LIMIT}건)
     */
    @Override
    public List<KnowledgeChunk> search(String query, KbFilter filter) {
        // 쿼리 + 카테고리/세부유형을 키워드로 합쳐 docs/* 전체에서 검색
        List<String> keywords = DocRepository.tokenize(query, filter.category(), filter.subType());
        return docRepository.search(keywords, null, CHUNK_LIMIT).stream()
                .map(this::toChunk)
                .toList();
    }

    /**
     * F10 기능 — 카테고리·세부유형에 해당하는 규격서를 자동 매칭한다.
     *
     * <p>검색 순서:</p>
     * <ol>
     *   <li>{@code docs/spec/} 폴더에서 키워드 매칭 상위 3건</li>
     *   <li>{@code docs/provider/} 폴더에서 키워드 매칭 상위 3건</li>
     *   <li>두 폴더 모두 매칭 결과가 없으면 각 폴더 전체를 기본 후보로 반환 — 사용자에게 "관련 자료 풀"을 보여주는 효과</li>
     * </ol>
     *
     * @param category 카테고리 코드
     * @param subType  세부유형 코드
     * @return 매칭된 규격서 참조 목록
     */
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

        // spec 결과와 provider 결과를 합쳐 SpecDocRef 목록으로 변환
        return java.util.stream.Stream.concat(specHits.stream(), providerHits.stream())
                .map(this::toSpecRef)
                .toList();
    }

    /**
     * F11 기능 — 유사 과거 요청을 검색한다.
     *
     * <p>현 단계(Phase 0)에는 KB에 적재된 과거 요청 데이터가 없어 결과가 비어 있는 것이 정상이다.
     * 다만 흐름 검증 목적으로 {@code templates} 폴더의 양식 1건을 "유사 사례" 대체값으로 반환한다.</p>
     *
     * @param query 유사 요청 검색 조건
     * @return 유사 과거 요청 목록 (Phase 0에서는 최대 1건의 templates 문서)
     */
    @Override
    public List<PastRequestRef> findSimilarRequests(SimilarQuery query) {
        // 현 단계엔 KB에 적재된 과거 요청 데이터가 없어 비어있는 게 정상.
        // 다만 흐름 검증용으로 templates 폴더의 양식 1건을 "유사 사례"로 보여준다.
        return docRepository.byFolder("templates").stream()
                .limit(1)
                .map(d -> new PastRequestRef(
                        d.id(),
                        d.title(),
                        DocRepository.excerpt(d.body(), 200), // 미리보기 요약 200자 제한
                        0.0,                                   // Phase 0 스텁: 유사도 점수 미산정
                        d.meta().getOrDefault("last_updated", "")
                ))
                .toList();
    }

    /**
     * KB 현황을 반환한다.
     *
     * <p>스텁 구현에서는 {@link DocRepository}가 보유한 문서 수를 총 문서 수·인덱싱 수 모두에 동일하게 사용한다.</p>
     *
     * @return KB 현황 정보 (총 문서 수, 인덱싱 수, 최종 업데이트 시각)
     */
    @Override
    public KbStatus getStatus() {
        return new KbStatus(docRepository.size(), docRepository.size(), nullSafe(docRepository.latestUpdate()));
    }

    /**
     * {@link DocRepository.KnowledgeDoc}을 {@link KnowledgeChunk}로 변환한다.
     *
     * <p>본문은 {@value #CONTENT_MAX_CHARS}자로 잘라 반환하며,
     * Phase 0 스텁에서는 점수({@code score})가 의미 없으므로 0.0으로 고정한다.
     * 실제 정렬 순서는 {@link DocRepository#search} 내부에서 결정된다.</p>
     *
     * @param d 원본 문서
     * @return 변환된 지식 청크
     */
    private KnowledgeChunk toChunk(DocRepository.KnowledgeDoc d) {
        return new KnowledgeChunk(
                d.id(),
                d.title(),
                DocRepository.excerpt(d.body(), CONTENT_MAX_CHARS),
                d.path(),
                0.0 // Phase 0 stub에서는 점수 의미 없음 — 정렬은 DocRepository 내부에서 끝남
        );
    }

    /**
     * {@link DocRepository.KnowledgeDoc}을 {@link SpecDocRef}로 변환한다.
     *
     * <p>메타데이터에 {@code reference_url}이 있으면 해당 값을 URL로 사용하고,
     * 없으면 classpath 경로({@code d.path()})를 fallback으로 사용한다.</p>
     *
     * @param d 원본 문서
     * @return 변환된 규격서 참조
     */
    private SpecDocRef toSpecRef(DocRepository.KnowledgeDoc d) {
        return new SpecDocRef(
                d.id(),
                d.title(),
                d.meta().getOrDefault("reference_url", d.path()), // 외부 URL 없으면 내부 경로 사용
                d.meta().getOrDefault("version", "")              // version 메타 없으면 빈 문자열
        );
    }

    /**
     * null 안전 문자열 변환 유틸리티.
     *
     * @param s 변환 대상 문자열 (null 허용)
     * @return null이면 빈 문자열, 아니면 원본 문자열
     */
    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
