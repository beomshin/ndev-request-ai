package com.nice.qa.model.api.dto;

import jakarta.validation.constraints.NotBlank;


/**
 * 개발요청서 생성 API 요청 DTO (Request DTO for creating a new DevRequest).
 *
 * <p>이 레코드는 위저드 UI 에서 사용자가 입력한 핵심 필드들을 서버로 전달한다.
 * Phase 0 단계에서는 전체 요건 정의서 필드 중 필수 핵심 필드만 수신하며,
 * 상세 설계 단계에서 나머지 정보를 추가로 수집한다.</p>
 *
 * <p>설계 문서 §5 DevRequest 섹션에 전체 필드 목록이 정의되어 있다.</p>
 *
 * <p>This record carries the core wizard inputs from the frontend to the server.
 * Phase 0 only collects the essential fields; additional details are gathered
 * in subsequent design phases. See design.md §5 DevRequest for the full field list.</p>
 *
 * @param funcType               기능 유형 — 위저드 최상위 분기 키
 *                               (예: "기존 서비스 수정·개선" 또는 "신규 서비스 개발")
 *                               (Top-level wizard branch key,
 *                               e.g. "기존 서비스 수정·개선" or "신규 서비스 개발")
 * @param category               서비스 카테고리 코드
 *                               (예: "pg표준결제창", "API", "해외결제", "기타서비스")
 *                               (Service category code,
 *                               e.g. "pg표준결제창", "API", "해외결제", "기타서비스")
 * @param subType                카테고리 하위 세부유형 코드.
 *                               기타서비스의 경우 클라이언트가 "(없음)" 등의 값을 전달한다.
 *                               (Sub-type code under the selected category.
 *                               For "기타서비스", the client sends a placeholder such as "(없음)".)
 * @param author                 요청서 작성자 이름 (Name of the person submitting the request)
 * @param department             작성자 소속 부서명 (Department of the request author)
 * @param serviceName            요청 대상 서비스명 (Name of the target service being requested)
 * @param background             개발 요청의 추진 배경 설명
 *                               (Description of the business background driving this request)
 * @param targetSchedule         목표 완료 일정 (문자열 형식, 예: "2025-Q3")
 *                               (Target completion schedule as a free-form string, e.g. "2025-Q3")
 * @param problemAndImprovement  현재 문제점 및 개선 목표 설명
 *                               (Description of current problems and the intended improvements)
 */
public record DevRequestRequest(

        // ── 위저드 분기 필드 (Wizard branch / routing fields) ──────────────────

        /**
         * 기능 유형 코드 — 위저드 최상위 분기 키.
         * 허용값: "기존 서비스 수정·개선", "신규 서비스 개발"
         * (Top-level wizard branch key. Allowed values: "기존 서비스 수정·개선", "신규 서비스 개발")
         */
        @NotBlank String funcType,

        /**
         * 서비스 카테고리 코드.
         * 허용값 예시: "pg표준결제창", "API", "해외결제", "기타서비스"
         * (Service category code. Examples: "pg표준결제창", "API", "해외결제", "기타서비스")
         */
        @NotBlank String category,

        /**
         * 카테고리 하위 세부유형 코드.
         * 기타서비스 선택 시 클라이언트는 "(없음)" 등의 대체 값을 전달한다.
         * (Sub-type code for the selected category.
         * The client sends a placeholder like "(없음)" when "기타서비스" is selected.)
         */
        @NotBlank String subType,

        // ── 공통 포맷 핵심 필드 (Common core fields) ──────────────────────────

        /**
         * 요청서 작성자 이름 (Name of the request author).
         */
        @NotBlank String author,

        /**
         * 작성자 소속 부서명 (Department name of the author).
         */
        @NotBlank String department,

        /**
         * 개발 요청 대상 서비스명 (Name of the service to be developed or modified).
         */
        @NotBlank String serviceName,

        /**
         * 개발 요청의 추진 배경 — 비즈니스 맥락 및 계기를 서술한다.
         * (Business background and motivation behind this development request.)
         */
        @NotBlank String background,

        /**
         * 목표 완료 일정 — 자유 문자열 형식 (예: "2025-Q3", "2025-09-30").
         * (Target schedule as a free-form string, e.g. "2025-Q3" or "2025-09-30".)
         */
        @NotBlank String targetSchedule,

        /**
         * 현재 시스템의 문제점과 이번 요청을 통해 달성하고자 하는 개선 목표.
         * (Description of current pain points and the improvement goals of this request.)
         */
        @NotBlank String problemAndImprovement
) {
}
