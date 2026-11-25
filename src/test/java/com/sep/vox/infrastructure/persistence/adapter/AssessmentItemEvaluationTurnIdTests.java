package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.domain.repository.AssessmentItemEvaluationTurnRepository;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import(AssessmentItemEvaluationTurnRepositoryImpl.class)
class AssessmentItemEvaluationTurnIdTests {

    @Autowired
    private AssessmentItemEvaluationTurnRepository repository;

    @Test
    void should_store_a_version_7_id_for_every_saved_turn() {
        UUID evaluationId = UUID.randomUUID();
        List<AssessmentItemEvaluationTurn> turns = List.of(turn(evaluationId, 1), turn(evaluationId, 2));

        List<AssessmentItemEvaluationTurn> saved = repository.saveAll(turns);

        assertThat(saved).hasSize(2)
            .allSatisfy(turn -> assertThat(turn.getId().version()).isEqualTo(7));
        assertThat(saved.get(0).getId()).isNotEqualTo(saved.get(1).getId());
    }

    private static AssessmentItemEvaluationTurn turn(UUID evaluationId, int turnOrder) {
        return AssessmentItemEvaluationTurn.recorded(
            evaluationId, turnOrder, TurnType.MAIN, null, "audio.mp3", "Hello", 1, 5, null, null, null);
    }
}
