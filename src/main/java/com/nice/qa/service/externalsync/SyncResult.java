package com.nice.qa.service.externalsync;

// 외부 연계 결과.
public record SyncResult(
        Status status,
        String message
) {
    public enum Status {SKIPPED, OK, FAIL}

    public static SyncResult skipped(String message) {
        return new SyncResult(Status.SKIPPED, message);
    }
}
