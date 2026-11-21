package com.sep.vox.infrastructure.persistence.audit;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.AuditActorScopePort;

@Component
public class ThreadLocalAuditActorScope implements AuditActorScopePort {

    private final ThreadLocal<String> currentActor = new ThreadLocal<>();

    @Override
    public <T> T callAs(String actor, Supplier<T> action) {
        String previousActor = currentActor.get();
        currentActor.set(actor);
        try {
            return action.get();
        } finally {
            restore(previousActor);
        }
    }

    public Optional<String> findCurrentActor() {
        return Optional.ofNullable(currentActor.get());
    }

    private void restore(String previousActor) {
        if (previousActor == null) {
            currentActor.remove();
        } else {
            currentActor.set(previousActor);
        }
    }
}
