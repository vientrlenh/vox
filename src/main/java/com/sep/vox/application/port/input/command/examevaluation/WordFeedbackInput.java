package com.sep.vox.application.port.input.command.examevaluation;

import java.util.List;


public record WordFeedbackInput(
    String word,
    Double accuracyScore,
    /** Điểm đã tính cả lỗi âm vị -- xem pronunciation_node_helper.py, KHÔNG suy lại được. */
    Double effectiveScore,
    String errorType,
    /** red | yellow | green | gray -- nguồn sự thật cho màu trên giao diện. */
    String color,
    String level,
    String errorNote,
    Boolean hasCriticalIssue,
    List<PhonemeFeedbackInput> phonemes
) {
}
