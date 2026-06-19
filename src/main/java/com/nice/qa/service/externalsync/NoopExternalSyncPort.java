package com.nice.qa.service.externalsync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// F13 자리만 남김. 실제 연계 어댑터(SHARE 등)는 추후.
@Component
@ConditionalOnProperty(name = "external-sync.provider", havingValue = "noop", matchIfMissing = true)
public class NoopExternalSyncPort implements ExternalSyncPort {

    @Override
    public SyncResult publish(PublishedRequest req) {
        return SyncResult.skipped("external-sync.provider=noop");
    }
}
