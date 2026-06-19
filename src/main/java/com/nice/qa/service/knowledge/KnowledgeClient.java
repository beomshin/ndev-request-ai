package com.nice.qa.service.knowledge;

import java.util.List;

// KB(지식저장소) 격리. 데이터는 타 담당이 채우고 우리는 조회만.
// 반환·입력 스키마는 우리가 정의(이 패키지). Phase 0은 StubKnowledgeClient.
public interface KnowledgeClient {

    // 동적폼 분기 트리 — KB 보유, 상시 수정 가능. 스키마는 §10-2에 따라 우리가 정의.
    CategoryTree getCategoryTree();

    // RAG 검색: 필터로 관련 청크 조회.
    List<KnowledgeChunk> search(String query, KbFilter filter);

    // F10 규격 자동매칭.
    List<SpecDocRef> matchSpecDocs(String category, String subType);

    // F11 + 설계flow 근거: 유사 과거 요청 (KB 보유 데이터 한정, §10-3).
    List<PastRequestRef> findSimilarRequests(SimilarQuery query);

    // 학습/적재 현황(read-only).
    KbStatus getStatus();
}
