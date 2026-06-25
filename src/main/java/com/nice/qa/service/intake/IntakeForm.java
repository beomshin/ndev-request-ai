package com.nice.qa.service.intake;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 신규 지불수단 등록 입력 폼 스키마.
 * docs/policy/payment_method_intake_form_v1.md 의 frontmatter를 그대로 노출.
 * FE가 inputType별 위젯으로 동적 렌더링한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IntakeForm(
        String docId,
        String title,
        String version,
        List<Section> sections,
        List<Field> fields
) {
    public record Section(String code, String name, int order) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Field(
            String policyId,
            String section,
            String label,
            String inputType,        // text | number | boolean | select | multiselect | group
            Boolean required,
            List<String> options,    // select / multiselect 전용
            Object defaultValue,     // 타입 다양 — 그대로 노출
            String placeholder,
            String helpText,
            String pattern,
            String format,
            String unit,
            Integer maxLength,
            String key,              // group 안 nested field 식별 (예: PAYMENT_WINDOW, ko/en/zh)
            List<Field> fields,      // group 일 때 nested
            String sourceDocId
    ) {}
}
