package com.nice.qa.service.knowledge;

import com.nice.qa.service.knowledge.dto.*;

import java.util.List;

/**
 * 지식저장소(KB: Knowledge Base) 접근 인터페이스.
 *
 * <p>KB 데이터의 소유·적재는 다른 담당 팀이 수행하며,
 * 이 시스템(요청서 AI 서비스)은 <strong>조회 전용(read-only)</strong>으로만 사용한다.</p>
 *
 * <p>반환·입력 스키마는 이 패키지({@code com.nice.qa.service.knowledge.dto})에서 정의하며,
 * KB 구현체가 바뀌어도 호출부 코드가 변경되지 않도록 추상화 경계를 제공한다.</p>
 *
 * <p>Phase 0 단계에서는 {@link StubKnowledgeClient}가 이 인터페이스를 구현하며,
 * 실제 KB 어댑터가 완성되면 별도 구현체로 교체한다.</p>
 *
 * <h2>구현 전략 (Implementation Strategy)</h2>
 * <ul>
 *   <li>실제 KB 연동 전: {@code knowledge.provider=stub} 설정 시 {@link StubKnowledgeClient} 활성화</li>
 *   <li>실제 KB 연동 후: {@code knowledge.provider=real} 등의 설정으로 실 구현체 교체</li>
 * </ul>
 *
 * @see StubKnowledgeClient
 * @see com.nice.qa.service.knowledge.dto.CategoryTree
 * @see com.nice.qa.service.knowledge.dto.KnowledgeChunk
 */
public interface KnowledgeClient {

    /**
     * 동적 폼 분기 트리를 반환한다.
     *
     * <p>분기 트리는 KB가 보유·관리하는 데이터로, 언제든 수정될 수 있다.
     * 스키마 정의는 §10-2 규약에 따라 이 시스템이 담당한다.</p>
     *
     * <p>반환된 {@link CategoryTree}는 위저드 UI가 다음 분기를 결정하는 데 사용된다:</p>
     * <ol>
     *   <li>1단계: 기능 구분 (funcType — NEW/MODIFY 등)</li>
     *   <li>2단계: 카테고리 선택</li>
     *   <li>3단계: 세부 유형(subType) 선택</li>
     * </ol>
     *
     * @return 동적 폼 분기용 카테고리 트리 (null 반환 없음)
     */
    // 동적폼 분기 트리 — KB 보유, 상시 수정 가능. 스키마는 §10-2에 따라 우리가 정의.
    CategoryTree getCategoryTree();

    /**
     * RAG(Retrieval-Augmented Generation) 기반 지식 청크 검색.
     *
     * <p>주어진 자연어 쿼리와 필터 조건에 맞는 관련 문서 청크 목록을 반환한다.
     * AI 응답 생성 시 컨텍스트로 주입되는 근거 자료로 활용된다.</p>
     *
     * @param query  자연어 검색 쿼리 (예: "카카오페이 결제창 연동 방법")
     * @param filter 카테고리 및 세부유형 필터 ({@code null} 필드는 미적용)
     * @return 관련도 순으로 정렬된 지식 청크 목록 (빈 리스트 가능, null 반환 없음)
     */
    // RAG 검색: 필터로 관련 청크 조회.
    List<KnowledgeChunk> search(String query, KbFilter filter);

    /**
     * F10 기능 — 카테고리·세부유형에 따른 규격서 자동 매칭.
     *
     * <p>요청서 작성 시 관련 원천사 규격서 또는 제공사 문서를 자동으로 추천한다.
     * KB에 등록된 spec/provider 폴더 문서를 키워드 매칭으로 찾는다.</p>
     *
     * @param category 카테고리 코드 (예: "payment_window")
     * @param subType  세부유형 코드 (예: "kakaopay")
     * @return 매칭된 규격서 참조 목록 (매칭 결과 없으면 폴더 전체 후보 반환)
     */
    // F10 규격 자동매칭.
    List<SpecDocRef> matchSpecDocs(String category, String subType);

    /**
     * F11 기능 + 설계 흐름 근거 — 유사 과거 요청 검색.
     *
     * <p>현재 요청서와 유사한 과거 완료 요청을 KB에서 찾아 반환한다.
     * KB가 보유한 데이터 범위 내에서만 동작하며(§10-3 참조),
     * Phase 0에서는 templates 폴더 문서를 흐름 검증용 대체 데이터로 사용한다.</p>
     *
     * @param query 유사 요청 검색 조건 (funcType, category, subType, 키워드 목록 포함)
     * @return 유사도 순 과거 요청 참조 목록 (빈 리스트 가능, null 반환 없음)
     */
    // F11 + 설계flow 근거: 유사 과거 요청 (KB 보유 데이터 한정, §10-3).
    List<PastRequestRef> findSimilarRequests(SimilarQuery query);

    /**
     * KB 학습·적재 현황을 조회한다 (read-only).
     *
     * <p>대시보드 또는 관리 화면에서 KB의 전체 문서 수, 인덱싱 완료 수,
     * 마지막 업데이트 시각 등을 표시하는 데 사용된다.</p>
     *
     * @return KB 현황 정보 (null 반환 없음)
     */
    // 학습/적재 현황(read-only).
    KbStatus getStatus();
}
