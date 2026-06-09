package com.nice.qa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// LLM이 돌려주는 케이스 한 건. JSON 필드명은 한국어 키로 받음.
public record TestCaseDto(
        @JsonProperty("결제수단") String paymentMethod,
        @JsonProperty("인증방식") String authMethod,
        @JsonProperty("할부") String installment,
        @JsonProperty("케이스분류") String category,   // 정상 / 예외 / 경계
        @JsonProperty("테스트내용") String content,
        @JsonProperty("예상결과") String expected,
        @JsonProperty("근거") String rationale
) {
}
