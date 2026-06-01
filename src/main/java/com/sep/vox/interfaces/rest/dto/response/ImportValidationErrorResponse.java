package com.sep.vox.interfaces.rest.dto.response;

import java.util.List;

import com.sep.vox.domain.dto.ImportRowErrorDto;

public record ImportValidationErrorResponse(
        String error,
        String message,
        List<ImportRowErrorDto> rowErrors) {
}
