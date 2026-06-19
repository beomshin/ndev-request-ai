package com.nice.qa.service.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

// 분기 트리 시드 + 더미 청크/규격/유사요청/현황 반환.
// 실제 KB 연동 전까지의 자리채움 — 실제 데이터·임베딩·검색은 KB 담당이 채움.
@Component
@ConditionalOnProperty(name = "knowledge.provider", havingValue = "stub", matchIfMissing = true)
public class StubKnowledgeClient implements KnowledgeClient {

    // 프롬프트에 명시된 분기 트리 시드
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
                // 기타서비스는 세부유형 없음
                new CategoryTree.CategoryNode("etc", "기타서비스", List.of())
        );

        return new CategoryTree(funcTypes, categories);
    }

    @Override
    public List<KnowledgeChunk> search(String query, KbFilter filter) {
        // 일반 placeholder 청크 — 실제 임베딩/RAG 결과는 KB 담당이 채움
        return List.of(
                new KnowledgeChunk("chunk-1", "샘플 KB 청크 #1",
                        "(KB 검색 결과 placeholder) 카테고리=" + filter.category() + ", 세부유형=" + filter.subType(),
                        "stub://kb/chunk-1", 0.92),
                new KnowledgeChunk("chunk-2", "샘플 KB 청크 #2",
                        "(KB 검색 결과 placeholder)",
                        "stub://kb/chunk-2", 0.81)
        );
    }

    @Override
    public List<SpecDocRef> matchSpecDocs(String category, String subType) {
        // 더미 규격서 — 실제 매칭은 KB 담당
        return List.of(
                new SpecDocRef("spec-1",
                        "[" + category + "/" + subType + "] 규격서 (샘플)",
                        "stub://kb/spec-1", "v0")
        );
    }

    @Override
    public List<PastRequestRef> findSimilarRequests(SimilarQuery query) {
        // 현 단계엔 적재된 과거 요청서가 없을 수 있으므로 빈 결과 graceful 처리 + 샘플 1건
        return List.of(
                new PastRequestRef("past-1",
                        "유사 과거 요청 (샘플)",
                        "(KB에 데이터가 들어오면 실제 유사도 기반 결과로 대체됩니다)",
                        0.75, "2026-01-01")
        );
    }

    @Override
    public KbStatus getStatus() {
        return new KbStatus(0L, 0L, "stub");
    }
}
