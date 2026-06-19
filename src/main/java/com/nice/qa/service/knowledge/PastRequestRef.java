package com.nice.qa.service.knowledge;

// F11 유사 과거 요청 참조. Phase 0 단계엔 KB가 보유한 외부 데이터에 한정.
public record PastRequestRef(
        String id,
        String title,
        String summary,
        double similarity,
        String createdAt
) {
}
