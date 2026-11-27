package com.sep.vox.application.port.input.command.assessmentevaluation;

import java.util.List;


public record WordFeedbackInput(
    String word,
    Double accuracyScore,
    /** The score with phoneme errors already included -- see pronunciation_node_helper.py, it can NOT be recomputed. */
    Double effectiveScore,
    String errorType,
    /** red | yellow | green | gray -- the source of truth for the color in the UI. */
    String color,
    String level,
    String errorNote,
    Boolean hasCriticalIssue,
    List<PhonemeFeedbackInput> phonemes
) {
}
