package com.sep.vox.application.exception;

import java.util.List;

import com.sep.vox.domain.dto.ImportRowErrorDto;

public class ImportValidationException extends RuntimeException {

    private final List<ImportRowErrorDto> errors;

    public ImportValidationException(List<ImportRowErrorDto> errors) {
        super("Dữ liệu import không hợp lệ");
        this.errors = List.copyOf(errors);
    }

    public List<ImportRowErrorDto> getErrors() {
        return errors;
    }
}
