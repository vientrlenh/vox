package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentItemResponseRepository;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import(AssessmentItemResponseRepositoryImpl.class)
class AssessmentItemResponseIdTests {

    private static final UUID VERSION_5_ID = UUID.fromString("6ba7b810-9dad-51d1-80b4-00c04fd430c8");
    private static final UUID VERSION_7_ID = UUID.fromString("0190f2b4-7c3e-7a52-9c1d-3f2b8a6e4d10");

    @Autowired
    private AssessmentItemResponseRepository repository;
    @Autowired
    private SpringDataAssessmentItemResponseRepository springDataRepository;

    @Test
    void should_keep_a_version_5_id_sent_by_the_ai_service() {
        repository.save(response(VERSION_5_ID));

        assertThat(springDataRepository.findById(VERSION_5_ID)).isPresent();
    }

    @Test
    void should_keep_a_version_7_id_sent_by_the_ai_service() {
        repository.save(response(VERSION_7_ID));

        assertThat(springDataRepository.findById(VERSION_7_ID)).isPresent();
    }

    @Test
    void should_store_a_version_7_id_when_the_row_has_no_id() {
        AssessmentItemResponse saved = repository.save(response(null));

        assertThat(saved.getId().version()).isEqualTo(7);
        assertThat(springDataRepository.findById(saved.getId())).isPresent();
    }

    private static AssessmentItemResponse response(UUID id) {
        return AssessmentItemResponse.builder()
            .id(id)
            .sessionId(UUID.randomUUID())
            .paperItemId(UUID.randomUUID())
            .durationSeconds(10)
            .submittedAt(Instant.now())
            .build();
    }
}
