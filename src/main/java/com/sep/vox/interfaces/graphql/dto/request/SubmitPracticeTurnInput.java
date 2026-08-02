package com.sep.vox.interfaces.graphql.dto.request;

import java.util.List;
import java.util.UUID;

public record SubmitPracticeTurnInput(
        UUID sessionId,
        UUID questionId,
        int turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        int durationSeconds,
        String wordFeedbackJson,
        Double turnScore,
        boolean questionComplete,
        List<TurnCorrectionInput> corrections) {

    public record TurnCorrectionInput(
            String category,
            String originalText,
            String correctedText,
            String explanation,
            String correctAudioUrl,
            double confidence) {
    }
}
