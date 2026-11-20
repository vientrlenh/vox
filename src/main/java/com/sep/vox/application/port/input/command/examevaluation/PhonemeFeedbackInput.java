package com.sep.vox.application.port.input.command.examevaluation;

/**
 * Nhận xét phát âm ở mức ÂM VỊ. Xem {@link WordFeedbackInput} để biết vì sao màu phải do Python
 * quyết chứ không suy lại ở client.
 */
public record PhonemeFeedbackInput(
    String phoneme,
    Double accuracyScore,
    /** red | yellow | green | gray. */
    String color,
    String level,
    /** Câu giải thích ngắn cho học sinh, ví dụ "The sound /z/ is good." */
    String note
) {
}
