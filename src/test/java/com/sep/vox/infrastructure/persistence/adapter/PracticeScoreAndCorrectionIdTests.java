package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.repository.PracticeCriterionScoreRepository;
import com.sep.vox.domain.repository.TurnCorrectionRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeCriterionScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.personalization.TurnCorrectionJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeCriterionScoreRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTurnCorrectionRepository;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({PracticeCriterionScoreRepositoryImpl.class, TurnCorrectionRepositoryImpl.class})
class PracticeScoreAndCorrectionIdTests {

    @Autowired
    private PracticeCriterionScoreRepository scoreRepository;
    @Autowired
    private TurnCorrectionRepository correctionRepository;
    @Autowired
    private SpringDataPracticeCriterionScoreRepository springDataScoreRepository;
    @Autowired
    private SpringDataTurnCorrectionRepository springDataCorrectionRepository;

    @Test
    void should_store_a_version_7_id_for_a_new_criterion_score() {
        UUID evaluationId = UUID.randomUUID();

        scoreRepository.upsertByCode(evaluationId, "GRAMMAR", 6.5);

        List<PracticeCriterionScoreJpaEntity> stored = springDataScoreRepository.findAll();
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getId().version()).isEqualTo(7);
    }

    @Test
    void should_store_version_7_ids_and_return_corrections_in_the_order_they_were_saved() {
        UUID turnId = UUID.randomUUID();

        saveCorrection(turnId, "first");
        saveCorrection(turnId, "second");
        saveCorrection(turnId, "third");

        List<TurnCorrectionJpaEntity> stored = springDataCorrectionRepository.findAll();
        assertThat(stored).allSatisfy(entity -> assertThat(entity.getId().version()).isEqualTo(7));
        assertThat(correctionRepository.findByTurnIdOrderById(turnId))
            .extracting(dto -> dto.originalText())
            .containsExactly("first", "second", "third");
    }

    private void saveCorrection(UUID turnId, String originalText) {
        correctionRepository.save(turnId, "GRAMMAR", originalText, "corrected", "why", null);
    }
}
