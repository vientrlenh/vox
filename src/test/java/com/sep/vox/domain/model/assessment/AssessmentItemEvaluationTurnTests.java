package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentItemEvaluationTurnTests {

    @Test
    void recordedLeavesTheIdNullAndSetsTheGivenValues() {
        UUID evaluationId = UUID.randomUUID();

        AssessmentItemEvaluationTurn turn = AssessmentItemEvaluationTurn.recorded(
                evaluationId, 2, TurnType.FOLLOWUP, "Prompt", "audio.mp3", "Hello", 1, 12, 0.8, "{}", "[]");

        assertThat(turn.getId()).isNull();
        assertThat(turn.getEvaluationId()).isEqualTo(evaluationId);
        assertThat(turn.getTurnOrder()).isEqualTo(2);
        assertThat(turn.getTurnType()).isEqualTo(TurnType.FOLLOWUP);
        assertThat(turn.getPromptText()).isEqualTo("Prompt");
        assertThat(turn.getAudioUrl()).isEqualTo("audio.mp3");
        assertThat(turn.getTranscript()).isEqualTo("Hello");
        assertThat(turn.getWordCount()).isEqualTo(1);
        assertThat(turn.getDurationSeconds()).isEqualTo(12);
        assertThat(turn.getAsrConfidence()).isEqualTo(0.8);
        assertThat(turn.getPronunciationOverallJson()).isEqualTo("{}");
        assertThat(turn.getWordFeedbackJson()).isEqualTo("[]");
    }

    @Test
    void recordedLeavesTheIdNullSoPersistenceCanGenerateIt() {
        AssessmentItemEvaluationTurn turn = AssessmentItemEvaluationTurn.recorded(
                UUID.randomUUID(), 1, TurnType.MAIN, null, "", "", 0, null, null, null, null);

        assertThat(turn.getId()).isNull();
    }
}
