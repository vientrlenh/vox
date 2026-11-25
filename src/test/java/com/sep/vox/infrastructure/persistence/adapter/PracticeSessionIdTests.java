package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.personalization.PracticeSession;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import(PracticeSessionRepositoryImpl.class)
class PracticeSessionIdTests {

    @Autowired
    private PracticeSessionRepository sessionRepository;

    @Test
    void should_store_a_version_7_id_for_a_session_saved_with_a_null_id() {
        PracticeSession saved = sessionRepository.save(PracticeSession.builder()
            .studentId(UUID.randomUUID())
            .practicePaperId(UUID.randomUUID())
            .targetFrameworkBandId(UUID.randomUUID())
            .chosenPracticeTopicId(UUID.randomUUID())
            .targetSubAttributesJson("[]")
            .origin("SELECTED")
            .offeredTopicIdsJson("[]")
            .startedAt(Instant.parse("2026-01-01T00:00:00Z"))
            .lastHeartbeatAt(Instant.parse("2026-01-01T00:00:00Z"))
            .gradedSeconds(0)
            .status("IN_PROGRESS")
            .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId().version()).isEqualTo(7);
    }
}
