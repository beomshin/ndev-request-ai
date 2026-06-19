package com.nice.qa.service.knowledge;

// F10 규격 자동매칭 결과. KB에 등록된 규격서 참조.
public record SpecDocRef(
        String id,
        String title,
        String url,
        String version
) {
}
