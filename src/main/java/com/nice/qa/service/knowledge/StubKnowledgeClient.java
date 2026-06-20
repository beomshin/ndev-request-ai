package com.nice.qa.service.knowledge;

import com.nice.qa.service.knowledge.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

    private final DocRepository docRepository;

    @Override
    public CategoryTree getCategoryTree() {
        List<CategoryTree.FuncType> funcTypes = List.of(
                new CategoryTree.FuncType("modify", "기존 서비스 수정·개선"),
                new CategoryTree.FuncType("new", "신규 서비스 개발")
        );

        List<CategoryTree.CategoryNode> categories = List.of(
                new CategoryTree.CategoryNode("pg_std_pay", "pg표준결제창", List.of(
                        new CategoryTree.SubType("card", "카드"),
                        new CategoryTree.SubType("bank", "계좌이체"),
                        new CategoryTree.SubType("virtual_account", "가상계좌"),
                        new CategoryTree.SubType("mobile", "휴대폰결제"),
                        new CategoryTree.SubType("etc", "기타")
                )),
                new CategoryTree.CategoryNode("api", "API", List.of(
                        new CategoryTree.SubType("new_api", "신규API생성"),
                        new CategoryTree.SubType("billing", "빌링"),
                        new CategoryTree.SubType("sms_link", "SMS링크결제"),
                        new CategoryTree.SubType("etc", "기타")
                )),
                new CategoryTree.CategoryNode("overseas", "해외결제", List.of(
                        new CategoryTree.SubType("alipay", "알리페이"),
                        new CategoryTree.SubType("wechat", "위챗페이"),
                        new CategoryTree.SubType("linepay", "라인페이"),
                        new CategoryTree.SubType("etc", "기타")
                )),
                new CategoryTree.CategoryNode("etc", "기타서비스", List.of())
        );

        return new CategoryTree(funcTypes, categories);
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
