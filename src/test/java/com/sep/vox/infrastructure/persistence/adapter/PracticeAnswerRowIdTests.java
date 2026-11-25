package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.repository.PracticeItemEvaluationRepository;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    PracticeItemResponseRepositoryImpl.class,
    PracticeItemEvaluationRepositoryImpl.class,
    PracticeResponseTurnRepositoryImpl.class
})
class PracticeAnswerRowIdTests {

    @Autowired
    private PracticeItemResponseRepository responseRepository;
    @Autowired
    private PracticeItemEvaluationRepository evaluationRepository;
    @Autowired
    private PracticeResponseTurnRepository turnRepository;

    @Test
    void should_store_a_version_7_response_id_and_return_it_again_for_the_same_session_and_question() {
        UUID sessionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        UUID firstId = responseRepository.upsertResponse(sessionId, questionId, "audio", "hello", false);
        UUID repeatedId = responseRepository.upsertResponse(sessionId, questionId, null, "again", true);

        assertThat(firstId.version()).isEqualTo(7);
        assertThat(repeatedId).isEqualTo(firstId);
    }

    @Test
    void should_store_a_version_7_evaluation_id_and_return_it_again_for_the_same_response() {
        UUID responseId = UUID.randomUUID();
        Instant evaluatedAt = Instant.parse("2026-01-01T00:00:00Z");

        UUID firstId = evaluationRepository.upsert(responseId, 7.5, false, evaluatedAt);
        UUID repeatedId = evaluationRepository.upsert(responseId, 8.0, false, evaluatedAt);

        assertThat(firstId.version()).isEqualTo(7);
        assertThat(repeatedId).isEqualTo(firstId);
    }

    @Test
    void should_store_a_version_7_turn_id_and_report_a_repeated_turn_order_as_not_new() {
        UUID responseId = UUID.randomUUID();

        PracticeResponseTurnRepository.TurnWrite first = saveTurn(responseId);
        PracticeResponseTurnRepository.TurnWrite repeated = saveTurn(responseId);

        assertThat(first.turnId().version()).isEqualTo(7);
        assertThat(first.created()).isTrue();
        assertThat(repeated.turnId()).isEqualTo(first.turnId());
        assertThat(repeated.created()).isFalse();
    }

    private PracticeResponseTurnRepository.TurnWrite saveTurn(UUID responseId) {
        return turnRepository.save(responseId, 1, "ANSWER", "prompt", "audio", "text", 12, null, null);
    }
}
