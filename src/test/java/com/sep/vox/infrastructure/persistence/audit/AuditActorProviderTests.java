package com.sep.vox.infrastructure.persistence.audit;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.output.UserContextPort;

@ExtendWith(MockitoExtension.class)
class AuditActorProviderTests {

    @Mock
    private UserContextPort userContextPort;

    private final ThreadLocalAuditActorScope actorScope = new ThreadLocalAuditActorScope();

    private AuditActorProvider provider() {
        return new AuditActorProvider(actorScope, userContextPort);
    }

    @Test
    void noUserGivesSystem() {
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());

        assertThat(provider().getCurrentAuditor()).contains("System");
    }

    @Test
    void authenticatedUserGivesTheUserIdText() {
        UUID userId = UUID.randomUUID();
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.of(userId));

        assertThat(provider().getCurrentAuditor()).contains(userId.toString());
    }

    @Test
    void activeScopeWinsOverTheAuthenticatedUser() {
        Optional<String> auditor = actorScope.callAs("importer", () -> provider().getCurrentAuditor());

        assertThat(auditor).contains("importer");
        verifyNoInteractions(userContextPort);
    }
}
