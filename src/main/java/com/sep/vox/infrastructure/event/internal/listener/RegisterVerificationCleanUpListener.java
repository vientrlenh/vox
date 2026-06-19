package com.sep.vox.infrastructure.event.internal.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.RegisterVerificationConsumedEvent;
import com.sep.vox.application.port.output.CacheManagerPort;

@Component
public class RegisterVerificationCleanUpListener {
    
    private final CacheManagerPort cacheManagerPort;

    public RegisterVerificationCleanUpListener(CacheManagerPort cacheManagerPort) {
        this.cacheManagerPort = cacheManagerPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RegisterVerificationConsumedEvent event) {
        cacheManagerPort.delete(event.key());
    }
}
