package com.sep.vox.interfaces.rest.dto.response;

import java.util.List;

import com.sep.vox.application.response.input.schoolclass.ImportRowErrorDto;

public record ImportValidationErrorResponse(
        String error,
        String message,
        List<ImportRowErrorDto> rowErrors) {
}
