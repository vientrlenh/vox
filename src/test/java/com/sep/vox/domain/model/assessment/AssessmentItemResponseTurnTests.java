package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentItemResponseTurnTests {

    @Test
    void answeredSetsTheGivenValuesWithoutIdOrCreationTime() {
        UUID responseId = UUID.randomUUID();
        Instant answeredAt = Instant.parse("2026-01-01T00:00:00Z");

        AssessmentItemResponseTurn turn = AssessmentItemResponseTurn.answered(
                responseId, 3, TurnType.FOLLOWUP, "Prompt", "audio.mp3", "Hello", 9, 1, answeredAt);

        assertThat(turn.getAssessmentItemResponseId()).isEqualTo(responseId);
        assertThat(turn.getTurnOrder()).isEqualTo(3);
        assertThat(turn.getTurnType()).isEqualTo(TurnType.FOLLOWUP);
        assertThat(turn.getPromptText()).isEqualTo("Prompt");
        assertThat(turn.getAudioUrl()).isEqualTo("audio.mp3");
        assertThat(turn.getTranscript()).isEqualTo("Hello");
        assertThat(turn.getDurationSeconds()).isEqualTo(9);
        assertThat(turn.getWordCount()).isEqualTo(1);
        assertThat(turn.getAnsweredAt()).isEqualTo(answeredAt);
        assertThat(turn.getId()).isNull();
        assertThat(turn.getCreatedAt()).isNull();
    }
}
