package com.nice.qa.service.llm;

import java.util.List;

// 본문 마크다운 + 추가확인 항목 목록.
public record RequestDocResult(
        String bodyMarkdown,
        List<String> additionalChecks
) {
}
