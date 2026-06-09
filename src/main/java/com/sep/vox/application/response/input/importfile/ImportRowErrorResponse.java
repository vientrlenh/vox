package com.sep.vox.application.response.input.importfile;

public record ImportRowErrorResponse(
    String field,
    String message
) {
}
