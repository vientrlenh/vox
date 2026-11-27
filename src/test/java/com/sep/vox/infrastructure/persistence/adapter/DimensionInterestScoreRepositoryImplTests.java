package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.DimensionInterestScoreRepository;
import com.sep.vox.support.IntegrationTest;

/**
 * A student retaking the interest quiz -- every time from the second on -- calls {@code replaceScores} with the same
 * {@code learner_profile_id} that already has old rows. Runs on a real DB because this bug is a FLUSH ORDER bug of
 * Hibernate: {@code deleteByLearnerProfileId} (not {@code @Modifying}) only queues one
 * {@code EntityDeleteAction}, and Hibernate always executes inserts BEFORE deletes in the flush queue
 * regardless of the call order in code -- a unit test with a mock repository can never see this,
 * because a mock has no flush queue at all.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class DimensionInterestScoreRepositoryImplTests {

    @Autowired
    private DimensionInterestScoreRepository repository;

    /**
     * Reproduces the real bug: {@code idx_dimension_interest_profile_dimension} blows up on the
     * INSERT in the SECOND replaceScores call, because the old row (same dimension) had not yet
     * been deleted when saveAll ran.
     */
    @Test
    void should_replace_scores_again_without_violating_the_unique_index() {
        UUID profileId = UUID.randomUUID();

        repository.replaceScores(profileId, Map.of(
            "TECH_GAMING", 0.6202,
            "TRAVEL_PLACES", 0.1667
        ));

        assertThatCode(() -> repository.replaceScores(profileId, Map.of(
            "TECH_GAMING", 0.7000,
            "SPORTS_HEALTH", 0.2000
        ))).doesNotThrowAnyException();

        Map<String, Double> current = repository.findByLearnerProfile(profileId);
        assertThat(current).containsEntry("TECH_GAMING", 0.7000);
        assertThat(current).containsEntry("SPORTS_HEALTH", 0.2000);
        // TRAVEL_PLACES belongs to the first run and is absent from the second -- replaceScores
        // means REPLACE entirely, not accumulate.
        assertThat(current).doesNotContainKey("TRAVEL_PLACES");
    }

    /**
     * Three runs in a row, exactly like a student retaking the quiz several times in one sitting.
     */
    @Test
    void should_survive_three_consecutive_replacements() {
        UUID profileId = UUID.randomUUID();

        for (int round = 1; round <= 3; round++) {
            double roundScore = 0.1 * round;
            assertThatCode(() -> repository.replaceScores(profileId, Map.of("TECH_GAMING", roundScore)))
                .doesNotThrowAnyException();
        }

        assertThat(repository.findByLearnerProfile(profileId)).containsEntry("TECH_GAMING", 0.3);
    }
}
