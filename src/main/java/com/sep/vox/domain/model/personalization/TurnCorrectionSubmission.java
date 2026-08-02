package com.sep.vox.domain.model.personalization;

/** Một lỗi được client báo kèm khi nộp lượt nói, trước khi được chấm/xác nhận. */
public record TurnCorrectionSubmission(
    String category,
    String originalText,
    String correctedText,
    String explanation,
    String correctAudioUrl,
    double confidence
) {
}
