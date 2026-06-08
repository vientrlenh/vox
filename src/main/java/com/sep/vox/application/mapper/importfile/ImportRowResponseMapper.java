package com.sep.vox.application.mapper.importfile;

import java.util.List;
import java.util.Map;

import com.sep.vox.application.common.JsonSerialization;
import com.sep.vox.application.response.input.importfile.ImportDataEntryResponse;
import com.sep.vox.application.response.input.importfile.ImportRowErrorResponse;
import com.sep.vox.application.response.input.importfile.ImportRowResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;

public final class ImportRowResponseMapper {

    private ImportRowResponseMapper() {
    }

    public static ImportRowResponse toResponse(ImportRow row) {
        return new ImportRowResponse(
            row.getId(),
            row.getSessionId(),
            row.getRowNumber(),
            toDataEntries(JsonSerialization.toStringMap(row.getRawDataJson())),
            toDataEntries(JsonSerialization.toStringMap(row.getMappedDataJson())),
            toErrors(JsonSerialization.toStringMapList(row.getErrorsJson())),
            valueOf(row.getStatus())
        );
    }

    public static PageResult<ImportRowResponse> toResponsePage(PageResult<ImportRow> page) {
        return new PageResult<>(
            page.content().stream()
                .map(ImportRowResponseMapper::toResponse)
                .toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static List<ImportDataEntryResponse> toDataEntries(Map<String, String> data) {
        return data.entrySet()
            .stream()
            .map(entry -> new ImportDataEntryResponse(entry.getKey(), entry.getValue()))
            .toList();
    }

    private static List<ImportRowErrorResponse> toErrors(List<Map<String, String>> errors) {
        return errors.stream()
            .map(error -> new ImportRowErrorResponse(error.get("field"), error.get("message")))
            .toList();
    }

    private static String valueOf(ImportRowStatus status) {
        return status == null ? null : status.name();
    }
}
