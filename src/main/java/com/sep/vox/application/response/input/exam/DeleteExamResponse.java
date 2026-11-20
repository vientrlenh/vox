package com.sep.vox.application.response.input.exam;

public record DeleteExamResponse(
    boolean deleted,
    boolean cancelledInstead
) {
}
