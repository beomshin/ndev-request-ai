package com.nice.qa.service.externalsync;

// 외부 시스템(SHARE/Plane/Confluence)으로 보낼 확정 요청서 표현.
// F13 자리만 남김 — Phase 0은 사용 안 함.
public record PublishedRequest(
        String id,
        String title,
        String bodyMarkdown
) {
}
