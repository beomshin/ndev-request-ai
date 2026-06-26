package com.nice.qa.service.knowledge.dto;

/**
 * F10 기능 — 규격서 자동매칭 결과를 담는 불변 레코드.
 *
 * <p>카테고리·세부유형에 따라 KB에서 자동으로 매칭된 원천사 규격서 또는
 * 제공사 문서의 참조 정보를 표현한다.</p>
 *
 * <p>설계자는 이 목록을 통해 요청서 작성 시 관련 공식 문서를
 * 빠르게 확인·첨부할 수 있다.</p>
 *
 * @see com.nice.qa.service.knowledge.KnowledgeClient#matchSpecDocs(String, String)
 */
public record SpecDocRef(
        /**
         * 규격서 문서 고유 식별자.
         * KB 내부 {@code doc_id} 또는 파일명 기반 ID
         */
        String id,

        /**
         * 규격서 문서 제목.
         * 예: "카카오페이 결제창 연동 규격서 v2.3"
         */
        String title,

        /**
         * 규격서 접근 URL 또는 classpath 경로.
         * 외부 URL({@code reference_url} 메타)이 있으면 해당 값을 사용하고,
         * 없으면 classpath 내부 경로를 fallback으로 사용한다.
         */
        String url,

        /**
         * 규격서 버전 문자열.
         * 프론트매터의 {@code version} 메타값; 정보가 없으면 빈 문자열
         */
        String version
) {
}
