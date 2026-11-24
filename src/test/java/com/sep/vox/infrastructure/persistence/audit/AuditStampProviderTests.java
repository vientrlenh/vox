package com.sep.vox.infrastructure.persistence.audit;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.output.UserContextPort;

@ExtendWith(MockitoExtension.class)
class AuditStampProviderTests {

    @Mock
    private UserContextPort userContextPort;

    private final ThreadLocalAuditActorScope actorScope = new ThreadLocalAuditActorScope();

    private AuditStampProvider provider() {
        return new AuditStampProvider(new AuditActorProvider(actorScope, userContextPort));
    }

    @Test
    void noUserGivesSystem() {
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());

        assertThat(provider().currentUpdatedBy()).isEqualTo("System");
    }

    @Test
    void authenticatedUserGivesTheUserIdText() {
        UUID userId = UUID.randomUUID();
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.of(userId));

        assertThat(provider().currentUpdatedBy()).isEqualTo(userId.toString());
    }

    @Test
    void activeScopeActorWins() {
        String actor = actorScope.callAs("importer", () -> provider().currentUpdatedBy());

        assertThat(actor).isEqualTo("importer");
    }

    @Test
    void updatedAtIsTheCurrentInstant() {
        Instant before = Instant.now();

        assertThat(provider().currentUpdatedAt()).isBetween(before, Instant.now());
    }
}
