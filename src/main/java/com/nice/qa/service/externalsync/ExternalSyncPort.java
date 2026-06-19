package com.nice.qa.service.externalsync;

// F13 외부 연계 격리. Phase 0은 NoopExternalSyncPort로 SKIPPED 반환.
public interface ExternalSyncPort {
    SyncResult publish(PublishedRequest req);
}
