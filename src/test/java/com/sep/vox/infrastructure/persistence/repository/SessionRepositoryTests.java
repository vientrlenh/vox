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

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.model.session.Session;
import com.sep.vox.domain.repository.SessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.SessionRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    SessionRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SessionRepositoryTests {

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    void whenSave_thenReturnsPersistedSession() {
        var userId = UUID.randomUUID();

        var saved = sessionRepository.save(newSession(userId, "refresh-token-hash"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRefreshTokenHash()).isEqualTo("refresh-token-hash");
    }

    @Test
    void whenFindById_thenReturnsSession() {
        var saved = sessionRepository.save(newSession(UUID.randomUUID(), "find-by-id-token"));

        var found = sessionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getRefreshTokenHash()).isEqualTo("find-by-id-token");
    }

    @Test
    void whenFindByUserIdAndRefreshTokenHash_thenReturnsMatchingSession() {
        var userId = UUID.randomUUID();
        sessionRepository.save(newSession(userId, "matching-refresh-token"));
        sessionRepository.save(newSession(UUID.randomUUID(), "matching-refresh-token"));

        var found = sessionRepository.findByUserIdAndRefreshTokenHash(userId, "matching-refresh-token");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getRefreshTokenHash()).isEqualTo("matching-refresh-token");
    }

    private static Session newSession(UUID userId, String refreshTokenHash) {
        var now = OffsetDateTime.now();
        return new Session(
            userId,
            refreshTokenHash,
            now,
            now.plusDays(30),
            null,
            null
        );
    }
}
