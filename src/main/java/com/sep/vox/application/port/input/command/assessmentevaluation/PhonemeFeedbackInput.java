package com.sep.vox.application.port.input.command.assessmentevaluation;

/**
 * Pronunciation feedback at the PHONEME level. See {@link WordFeedbackInput} for why the color must be decided
 * by Python and not recomputed on the client.
 */
public record PhonemeFeedbackInput(
    String phoneme,
    Double accuracyScore,
    /** red | yellow | green | gray. */
    String color,
    String level,
    /** A short explanation for the student, for example "The sound /z/ is good." */
    String note
) {
}
