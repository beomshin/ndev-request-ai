package com.nice.qa.model.api.dto;

import com.nice.qa.entity.DevRequestStatus;
import jakarta.validation.constraints.NotBlank;

/**
 * 신규 저장 / 수정 요청 공용 페이로드.
 * - generate(=무저장 초안) 결과를 그대로 받아 저장할 수도 있고,
 *   사용자가 폼에서 작성 중인 초안만 저장할 수도 있어 LLM 결과(combinedMarkdown 등)는 모두 선택형.
 */
public record DevRequestSaveRequest(
        @NotBlank String title,
        String categoryPath,          // "결제창 / 카드" 등
        DevRequestStatus status,      // null이면 DRAFT
        String author,
        String dept,
        String details,               // 위저드 입력 원본 JSON (자유 형식)
        String combinedMarkdown,      // Gemini 종합 MD
        String flowDiagram,           // mxGraph XML or PlantUML 소스
        String unconfirmedSection     // 추가 확인 항목 (JSON 또는 MD)
) {
}
