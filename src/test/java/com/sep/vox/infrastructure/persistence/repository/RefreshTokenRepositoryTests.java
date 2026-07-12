package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.infrastructure.persistence.adapter.RefreshTokenRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    RefreshTokenRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTests extends ContainerTestConfig {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void whenSave_thenReturnsPersistedRefreshToken() {
        var sessionId = UUID.randomUUID();

        var saved = refreshTokenRepository.save(newRefreshToken(sessionId, "hash-save"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSessionId()).isEqualTo(sessionId);
        assertThat(saved.getTokenHash()).isEqualTo("hash-save");
        assertThat(saved.getUsedAt()).isNull();
        assertThat(saved.getReplacedBy()).isNull();
    }

    @Test
    void whenFindById_thenReturnsRefreshToken() {
        var saved = refreshTokenRepository.save(newRefreshToken(UUID.randomUUID(), "hash-find-id"));

        var found = refreshTokenRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getTokenHash()).isEqualTo("hash-find-id");
    }

    @Test
    void whenFindBySessionId_thenReturnsOnlyTokensForThatSession() {
        var sessionId = UUID.randomUUID();
        refreshTokenRepository.save(newRefreshToken(sessionId, "hash-session-1"));
        refreshTokenRepository.save(newRefreshToken(sessionId, "hash-session-2"));
        refreshTokenRepository.save(newRefreshToken(UUID.randomUUID(), "hash-session-other"));

        var found = refreshTokenRepository.findBySessionId(sessionId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(token -> token.getTokenHash())
            .containsExactlyInAnyOrder("hash-session-1", "hash-session-2");
    }

    @Test
    void whenFindByTokenHash_thenReturnsMatchingRefreshToken() {
        var sessionId = UUID.randomUUID();
        refreshTokenRepository.save(newRefreshToken(sessionId, "hash-token-lookup"));
        refreshTokenRepository.save(newRefreshToken(UUID.randomUUID(), "hash-token-other"));

        var found = refreshTokenRepository.findByTokenHash("hash-token-lookup");

        assertThat(found).isPresent();
        assertThat(found.get().getSessionId()).isEqualTo(sessionId);
        assertThat(found.get().getTokenHash()).isEqualTo("hash-token-lookup");
    }

    private static RefreshToken newRefreshToken(UUID sessionId, String tokenHash) {
        var now = OffsetDateTime.now();
        return new RefreshToken(
            sessionId,
            tokenHash,
            now,
            now.plusDays(7),
            null,
            null
        );
    }
}
