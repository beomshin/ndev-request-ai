package com.nice.qa.service.llm.dto;

import java.util.List;

// 사전검토 경고 목록. 단순 문자열 메시지로 두고 레벨은 후속 확장 시 추가.
public record PreCheckResult(
        List<String> warnings
) {
}
