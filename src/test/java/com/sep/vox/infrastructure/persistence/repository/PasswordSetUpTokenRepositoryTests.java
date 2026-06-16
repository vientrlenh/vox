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
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.infrastructure.persistence.adapter.PasswordSetUpTokenRepositoryImpl;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    PasswordSetUpTokenRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasswordSetUpTokenRepositoryTests extends ContainerTestConfig {

    @Autowired
    private PasswordSetUpTokenRepository passwordSetUpTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenSave_thenReturnsPersistedPasswordSetUpToken() {
        var userId = UUID.randomUUID();

        var saved = passwordSetUpTokenRepository.save(newToken(userId, "token-hash"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTokenHash()).isEqualTo("token-hash");
        assertThat(saved.getUsedAt()).isNull();
    }

    @Test
    void whenFindById_thenReturnsPasswordSetUpToken() {
        var saved = passwordSetUpTokenRepository.save(newToken(UUID.randomUUID(), "find-id-token"));

        var found = passwordSetUpTokenRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getTokenHash()).isEqualTo("find-id-token");
    }

    @Test
    void whenFindByUserIdAndTokenHash_thenReturnsMatchingToken() {
        var userId = UUID.randomUUID();
        passwordSetUpTokenRepository.save(newToken(userId, "matching-token"));
        passwordSetUpTokenRepository.save(newToken(UUID.randomUUID(), "matching-token"));

        var found = passwordSetUpTokenRepository.findByUserIdAndTokenHash(userId, "matching-token");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getTokenHash()).isEqualTo("matching-token");
    }

    @Test
    void whenUpdateUsedTokenForValidToken_thenMarksTokenUsed() {
        var userId = UUID.randomUUID();
        var tokenHash = "valid-update-token";
        passwordSetUpTokenRepository.save(newToken(userId, tokenHash));
        entityManager.flush();

        var usedAt = OffsetDateTime.now();
        var updated = passwordSetUpTokenRepository.updateUsedToken(userId, tokenHash, usedAt);
        entityManager.clear();

        var found = passwordSetUpTokenRepository.findByUserIdAndTokenHash(userId, tokenHash);
        assertThat(updated).isEqualTo(1);
        assertThat(found).isPresent();
        assertThat(found.get().getUsedAt()).isNotNull();
    }

    @Test
    void whenUpdateUsedTokenForExpiredToken_thenDoesNotUpdate() {
        var userId = UUID.randomUUID();
        var tokenHash = "expired-update-token";
        var now = OffsetDateTime.now();
        passwordSetUpTokenRepository.save(
            new PasswordSetUpToken(userId, tokenHash, now.minusDays(3), now.minusDays(1), null)
        );
        entityManager.flush();

        var updated = passwordSetUpTokenRepository.updateUsedToken(userId, tokenHash, now);

        assertThat(updated).isZero();
    }

    private static PasswordSetUpToken newToken(UUID userId, String tokenHash) {
        var now = OffsetDateTime.now();
        return new PasswordSetUpToken(
            userId,
            tokenHash,
            now,
            now.plusDays(2),
            null
        );
    }
}
