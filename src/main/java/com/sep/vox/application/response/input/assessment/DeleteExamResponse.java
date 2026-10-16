package com.sep.vox.application.response.input.assessment;

public record DeleteExamResponse(
    boolean deleted,
    boolean cancelledInstead
) {
}
