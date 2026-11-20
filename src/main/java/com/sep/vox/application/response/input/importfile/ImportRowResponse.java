package com.sep.vox.application.response.input.importfile;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.importfile.ImportRowStatus;

public record ImportRowResponse(
    UUID id,
    UUID sessionId,
    long rowNumber,
    List<ImportDataEntryResponse> rawData,
    List<ImportDataEntryResponse> mappedData,
    List<ImportRowErrorResponse> errors,
    ImportRowStatus status
) {
}
