package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import(AssessmentItemResponseTurnRepositoryImpl.class)
class AssessmentItemResponseTurnIdTests {

    @Autowired
    private AssessmentItemResponseTurnRepository repository;

    @Test
    void should_store_a_version_7_id_for_a_new_turn() {
        AssessmentItemResponseTurn saved = repository.upsert(turn(UUID.randomUUID(), 1, "first"));

        assertThat(saved.getId().version()).isEqualTo(7);
    }

    @Test
    void should_keep_the_id_when_the_same_response_and_turn_order_is_upserted_again() {
        UUID responseId = UUID.randomUUID();
        AssessmentItemResponseTurn first = repository.upsert(turn(responseId, 1, "first"));

        AssessmentItemResponseTurn second = repository.upsert(turn(responseId, 1, "second"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getTranscript()).isEqualTo("second");
    }

    private static AssessmentItemResponseTurn turn(UUID responseId, int turnOrder, String transcript) {
        return AssessmentItemResponseTurn.builder()
            .assessmentItemResponseId(responseId)
            .turnOrder(turnOrder)
            .turnType(TurnType.MAIN)
            .transcript(transcript)
            .build();
    }
}
