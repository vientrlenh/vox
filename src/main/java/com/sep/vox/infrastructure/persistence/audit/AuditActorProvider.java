package com.sep.vox.infrastructure.persistence.audit;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.ActorConstant;

/**
 * Supplies the value stored in createdBy/updatedBy: the actor of the active {@link ThreadLocalAuditActorScope}, else
 * the authenticated user's id, else {@link ActorConstant#SYSTEM}. It never returns an empty result.
 */
@Component
@RequiredArgsConstructor
public class AuditActorProvider implements AuditorAware<String> {

    private final ThreadLocalAuditActorScope actorScope;
    private final UserContextPort userContextPort;

    @Override
    public Optional<String> getCurrentAuditor() {
        String actor = actorScope.findCurrentActor()
                .or(() -> userContextPort.findCurrentAuthenticatedUserId().map(id -> id.toString()))
                .orElse(ActorConstant.SYSTEM);
        return Optional.of(actor);
    }
}
