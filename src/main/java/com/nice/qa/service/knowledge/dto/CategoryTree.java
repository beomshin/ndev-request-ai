package com.nice.qa.service.knowledge.dto;

import java.util.List;

/**
 * 동적 폼 위저드의 분기 트리 구조를 담는 불변 레코드.
 *
 * <p>ground truth 데이터는 {@code docs/catalog/catalog_category_tree_v1.yaml}에 정의되어 있으며,
 * KB 담당 팀이 관리한다. 이 레코드는 해당 YAML을 파싱한 결과를 메모리에 보관한다.</p>
 *
 * <h2>위저드 분기 흐름 (Wizard Branch Flow)</h2>
 * <ol>
 *   <li>기능 구분 선택 ({@link FuncType}) — 예: 신규(NEW), 수정(MODIFY)</li>
 *   <li>카테고리 선택 ({@link CategoryNode}) — 예: 결제창, Web API</li>
 *   <li>세부 유형 선택 ({@link SubType}) — 예: 카카오페이, 네이버페이</li>
 * </ol>
 *
 * <p>각 {@link SubType}에는 FE가 동적 분기를 렌더링하는 데 필요한 메타 정보
 * ({@code paymentMethods}, {@code freeText}, {@code specMatchHints}, {@code availableFuncTypes})가 포함된다.</p>
 *
 * @see com.nice.qa.service.knowledge.StubKnowledgeClient
 */
public record CategoryTree(
        /** 1단계 선택지: 기능 구분 목록 (예: NEW, MODIFY) */
        List<FuncType> funcTypes,         // 1단계: 기능 구분

        /** 2단계 선택지: 카테고리 목록 (각 카테고리 하위에 세부유형 포함) */
        List<CategoryNode> categories     // 2단계: 카테고리 → 세부유형
) {

    /**
     * 기능 구분(1단계) 항목.
     *
     * <p>요청서 작성의 최상위 분기로, 기능의 성격을 구분한다.
     * 예: 신규 연동 개발({@code NEW}), 기존 기능 수정({@code MODIFY})</p>
     */
    public record FuncType(
            /**
             * 기능 구분 코드.
             * 예: {@code "NEW"}, {@code "MODIFY"}
             */
            String code,                  // NEW | MODIFY

            /** 화면에 표시되는 기능 구분 이름 */
            String label,

            /** 기능 구분에 대한 부연 설명 (선택 항목) */
            String description
    ) {}

    /**
     * 카테고리(2단계) 노드. 하위에 세부유형 목록을 포함한다.
     *
     * <p>{@code inputMode}가 {@code FREE_TEXT}인 경우 세부유형 목록 대신
     * 자유 입력 필드를 렌더링한다.</p>
     */
    public record CategoryNode(
            /** 카테고리 코드 (예: {@code "payment_window"}) */
            String code,

            /** 화면에 표시되는 카테고리 이름 */
            String label,

            /**
             * 카테고리의 입력 방식.
             * {@code "SELECT"}: 세부유형 드롭다운 선택 /
             * {@code "FREE_TEXT"}: 자유 텍스트 입력
             */
            String inputMode,             // SELECT | FREE_TEXT

            /** 3단계 세부유형 목록 */
            List<SubType> subTypes
    ) {}

    /**
     * 세부 유형(3단계) 항목. 위저드 분기에 필요한 모든 메타 정보를 포함한다.
     *
     * <p>FE는 이 레코드의 정보를 바탕으로 결제수단 선택 UI, 자유 입력 필드,
     * 규격서 자동매칭 힌트, 허용된 기능 유형 등을 동적으로 렌더링한다.</p>
     */
    public record SubType(
            /** 세부유형 코드 (예: {@code "kakaopay"}) */
            String code,

            /** 화면에 표시되는 세부유형 이름 */
            String label,

            /**
             * 이 세부유형에서 선택 가능한 결제수단 목록.
             * 빈 리스트이면 결제수단 매핑 없음 (결제와 무관한 항목)
             */
            List<String> paymentMethods,        // [] = 결제수단 매핑 없음

            /**
             * "기타" 자유 입력 분기 여부.
             * {@code true}이면 목록 선택 외 자유 텍스트 입력 필드를 추가로 표시
             */
            boolean freeText,                   // true = "기타" 자유 입력 분기

            /**
             * F10 규격서 자동매칭({@code matchSpecDocs}) 후보 {@code doc_id} 힌트 목록.
             * {@link com.nice.qa.service.knowledge.KnowledgeClient#matchSpecDocs} 호출 시 활용
             */
            List<String> specMatchHints,        // F10 matchSpecDocs 후보 doc_id

            /**
             * 이 세부유형에서 허용된 기능 구분({@link FuncType}) 코드 목록.
             * 빈 리스트 또는 {@code null}이면 모든 기능 구분 가용;
             * 명시된 경우 해당 기능 구분에서만 이 세부유형 선택 가능
             */
            List<String> availableFuncTypes     // [] 또는 null = 둘 다 가용, 명시되면 그 funcType에서만
    ) {}
}
