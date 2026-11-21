package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.infrastructure.persistence.adapter.PasswordSetUpTokenRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

@RepositoryTest
@Import({
    PasswordSetUpTokenRepositoryImpl.class
})
class PasswordSetUpTokenRepositoryTests {

    @Autowired
    private PasswordSetUpTokenRepository passwordSetUpTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenSave_thenReturnsPersistedPasswordSetUpToken() {
        UUID userId = UUID.randomUUID();

        PasswordSetUpToken saved = passwordSetUpTokenRepository.save(newToken(userId, "token-hash"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTokenHash()).isEqualTo("token-hash");
        assertThat(saved.getUsedAt()).isNull();
    }

    @Test
    void whenFindById_thenReturnsPasswordSetUpToken() {
        PasswordSetUpToken saved = passwordSetUpTokenRepository.save(newToken(UUID.randomUUID(), "find-id-token"));

        Optional<PasswordSetUpToken> found = passwordSetUpTokenRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getTokenHash()).isEqualTo("find-id-token");
    }

    @Test
    void whenFindByUserIdAndTokenHash_thenReturnsMatchingToken() {
        UUID userId = UUID.randomUUID();
        passwordSetUpTokenRepository.save(newToken(userId, "matching-token"));
        passwordSetUpTokenRepository.save(newToken(UUID.randomUUID(), "matching-token"));

        Optional<PasswordSetUpToken> found = passwordSetUpTokenRepository.findByUserIdAndTokenHash(userId, "matching-token");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getTokenHash()).isEqualTo("matching-token");
    }

    @Test
    void whenUpdateUsedTokenForValidToken_thenMarksTokenUsed() {
        UUID userId = UUID.randomUUID();
        String tokenHash = "valid-update-token";
        passwordSetUpTokenRepository.save(newToken(userId, tokenHash));
        entityManager.flush();

        Instant usedAt = Instant.now();
        int updated = passwordSetUpTokenRepository.updateUsedToken(userId, tokenHash, usedAt);
        entityManager.clear();

        Optional<PasswordSetUpToken> found = passwordSetUpTokenRepository.findByUserIdAndTokenHash(userId, tokenHash);
        assertThat(updated).isEqualTo(1);
        assertThat(found).isPresent();
        assertThat(found.get().getUsedAt()).isNotNull();
    }

    @Test
    void whenUpdateUsedTokenForExpiredToken_thenDoesNotUpdate() {
        UUID userId = UUID.randomUUID();
        String tokenHash = "expired-update-token";
        Instant now = Instant.now();
        passwordSetUpTokenRepository.save(
            PasswordSetUpToken.builder()
            .userId(userId)
            .tokenHash(tokenHash)
            .createdAt(now.minus(3, ChronoUnit.DAYS))
            .expiredAt(now.minus(1, ChronoUnit.DAYS))
            .build()
        );
        entityManager.flush();

        int updated = passwordSetUpTokenRepository.updateUsedToken(userId, tokenHash, now);

        assertThat(updated).isZero();
    }

    private static PasswordSetUpToken newToken(UUID userId, String tokenHash) {
        Instant now = Instant.now();
        return PasswordSetUpToken.builder()
            .userId(userId)
            .tokenHash(tokenHash)
            .createdAt(now)
            .expiredAt(now.plus(2, ChronoUnit.DAYS))
            .build();
    }
}
