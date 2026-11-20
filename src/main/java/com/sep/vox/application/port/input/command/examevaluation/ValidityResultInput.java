package com.sep.vox.application.port.input.command.examevaluation;

import java.util.List;
import java.util.Map;

public record ValidityResultInput(
    Boolean validForScoring,
    String action,
    String overallSeverity,
    List<Object> ruleResults,
    List<Object> flags,
    Map<String, Object> scoreCaps,
    List<Object> penalties,
    List<String> notes,
    String transcriptSource,
    Integer transcriptWordCount
) {
}
