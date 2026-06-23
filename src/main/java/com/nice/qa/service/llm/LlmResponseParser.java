package com.nice.qa.service.llm;

import org.springframework.util.StringUtils;

/**
 * LLM 응답 텍스트의 공통 후처리 유틸리티.
 * DocService / FlowService 양쪽이 동일한 처리를 필요로 하므로 여기서 일괄 관리한다.
 */
public final class LlmResponseParser {

    private LlmResponseParser() {}

    /**
     * 모델이 코드펜스(백틱 3개 + 언어명)를 붙였을 경우 제거하고 순수 텍스트만 반환.
     */
    public static String stripCodeFence(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return trimmed.strip();
    }
}
