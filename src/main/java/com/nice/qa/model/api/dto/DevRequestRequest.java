package com.nice.qa.model.api.dto;

import jakarta.validation.constraints.NotBlank;


/**
 * 개발요청서 생성 요청. 공통 포맷 핵심 필드 + 위저드 분기 키.
 * 전체 필드는 design.md §5 DevRequest에 있고, Phase 0은 핵심만 받는다.
 * @param funcType
 * @param category
 * @param subType
 * @param author
 * @param department
 * @param serviceName
 * @param background
 * @param targetSchedule
 * @param problemAndImprovement
 */
public record DevRequestRequest(
        // 위저드 분기
        @NotBlank String funcType,         // "기존 서비스 수정·개선" / "신규 서비스 개발"
        @NotBlank String category,         // pg표준결제창 / API / 해외결제 / 기타서비스
        @NotBlank String subType,          // 세부유형 코드 (기타서비스는 클라이언트가 "(없음)" 등으로 전달)

        // 공통 포맷 핵심
        @NotBlank String author,           // 작성자
        @NotBlank String department,       // 부서
        @NotBlank String serviceName,      // 서비스명
        @NotBlank String background,       // 추진배경
        @NotBlank String targetSchedule,   // 목표일정
        @NotBlank String problemAndImprovement // 문제점/개선점
) {
}
