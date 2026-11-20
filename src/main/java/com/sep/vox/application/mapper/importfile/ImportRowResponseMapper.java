package com.sep.vox.application.mapper.importfile;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.importfile.ImportDataEntryResponse;
import com.sep.vox.application.response.input.importfile.ImportRowErrorResponse;
import com.sep.vox.application.response.input.importfile.ImportRowResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;

@Component
public class ImportRowResponseMapper {

    private final JsonSerializationPort jsonSerializationPort;

    public ImportRowResponseMapper(JsonSerializationPort jsonSerializationPort) {
        this.jsonSerializationPort = jsonSerializationPort;
    }

    public ImportRowResponse toResponse(ImportRow row) {
        return new ImportRowResponse(
            row.getId(),
            row.getSessionId(),
            row.getRowNumber(),
            toDataEntries(jsonSerializationPort.toStringMap(row.getRawDataJson())),
            toDataEntries(jsonSerializationPort.toStringMap(row.getMappedDataJson())),
            toErrors(jsonSerializationPort.toStringMapList(row.getErrorsJson())),
            valueOf(row.getStatus())
        );
    }

    public PageResult<ImportRowResponse> toResponsePage(PageResult<ImportRow> page) {
        return new PageResult<>(
            page.content().stream()
                .map(this::toResponse)
                .toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private List<ImportDataEntryResponse> toDataEntries(Map<String, String> data) {
        return data.entrySet()
            .stream()
            .map(entry -> new ImportDataEntryResponse(entry.getKey(), entry.getValue()))
            .toList();
    }

    private List<ImportRowErrorResponse> toErrors(List<Map<String, String>> errors) {
        return errors.stream()
            .map(error -> new ImportRowErrorResponse(error.get("field"), error.get("message")))
            .toList();
    }

    private String valueOf(ImportRowStatus status) {
        return status == null ? null : status.name();
    }
}
