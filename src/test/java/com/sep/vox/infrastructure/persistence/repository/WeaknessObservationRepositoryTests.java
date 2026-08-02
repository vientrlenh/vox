package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.personalization.WeaknessObservation;
import com.sep.vox.domain.model.personalization.WeaknessObservationSourceType;
import com.sep.vox.domain.repository.WeaknessObservationRepository;
import com.sep.vox.infrastructure.persistence.adapter.WeaknessObservationRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import(WeaknessObservationRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WeaknessObservationRepositoryTests extends ContainerTestConfig {

    @Autowired
    private WeaknessObservationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_ignore_a_replayed_observation() {
        var observation = observation("tense_control", "I go yesterday");

        var existedBeforeFirstSave = saveIfAbsent(observation);
        var existedBeforeReplaySave = saveIfAbsent(observation);

        assertThat(existedBeforeFirstSave).isFalse();
        assertThat(existedBeforeReplaySave).isTrue();
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void should_keep_the_same_phoneme_with_different_evidence_words() {
        var sourceEvaluationId = UUID.randomUUID();
        var criterionId = UUID.randomUUID();
        var first = observation(
            sourceEvaluationId,
            criterionId,
            "phoneme_s",
            "cats"
        );
        var second = observation(
            sourceEvaluationId,
            criterionId,
            "phoneme_s",
            "books"
        );

        saveIfAbsent(first);
        saveIfAbsent(second);

        assertThat(countRows()).isEqualTo(2);
    }

    /** Trả về true nếu bản ghi đã tồn tại (không lưu lại) -- mô phỏng đúng vòng lặp
     * check-then-save mà UseCase gọi thật sự dùng, vì adapter không còn insert-ignore hàng loạt. */
    private boolean saveIfAbsent(WeaknessObservation observation) {
        var exists = repository.existsForKey(
            observation.getSourceEvaluationId(),
            observation.getFrameworkCriterionId(),
            observation.getSubAttribute(),
            observation.getEvidenceSpan()
        );
        if (!exists) {
            repository.save(observation);
        }
        return exists;
    }

    private WeaknessObservation observation(String subAttribute, String evidence) {
        return observation(
            UUID.randomUUID(),
            UUID.randomUUID(),
            subAttribute,
            evidence
        );
    }

    private WeaknessObservation observation(
            UUID sourceEvaluationId,
            UUID frameworkCriterionId,
            String subAttribute,
            String evidence) {
        return new WeaknessObservation(
            UUID.randomUUID(),
            WeaknessObservationSourceType.EXAM,
            sourceEvaluationId,
            frameworkCriterionId,
            "grammar",
            subAttribute,
            evidence,
            OffsetDateTime.now()
        );
    }

    private int countRows() {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM weakness_observation",
            Integer.class
        );
    }
}
