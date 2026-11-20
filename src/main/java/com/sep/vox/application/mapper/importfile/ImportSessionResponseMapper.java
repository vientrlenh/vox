package com.sep.vox.application.mapper.importfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.importfile.ImportMappingEntryResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;

@Component
public class ImportSessionResponseMapper {

    private final JsonSerializationPort jsonSerializationPort;

    public ImportSessionResponseMapper(JsonSerializationPort jsonSerializationPort) {
        this.jsonSerializationPort = jsonSerializationPort;
    }

    public ImportSessionDetailsResponse toDetails(ImportSession session) {
        return new ImportSessionDetailsResponse(
            session.getId(),
            session.getSchoolId(),
            valueOf(session.getType()),
            session.getFileName(),
            jsonSerializationPort.toStringList(session.getOriginalHeadersJson()),
            toMappingEntries(jsonSerializationPort.toStringMap(session.getSuggestedMappingJson())),
            toMappingEntries(jsonSerializationPort.toStringMap(session.getConfirmedMappingJson())),
            session.getValidRows(),
            session.getInvalidRows(),
            session.getImportedRows(),
            session.getSkippedRows(),
            session.getTotalRows(),
            session.getFailureReason(),
            valueOf(session.getStatus()),
            session.getImportedEntityId(),
            valueOf(session.getExpiresAt()),
            valueOf(session.getCreatedAt()),
            valueOf(session.getUpdatedAt())
        );
    }

    public ImportSessionSummaryResponse toSummary(ImportSession session) {
        return new ImportSessionSummaryResponse(
            session.getId(),
            session.getSchoolId(),
            valueOf(session.getType()),
            session.getFileName(),
            session.getTotalRows(),
            session.getValidRows(),
            session.getInvalidRows(),
            session.getImportedRows(),
            session.getSkippedRows(),
            valueOf(session.getStatus()),
            valueOf(session.getExpiresAt()),
            valueOf(session.getCreatedAt()),
            valueOf(session.getUpdatedAt())
        );
    }

    public PageResult<ImportSessionSummaryResponse> toSummaryPage(PageResult<ImportSession> page) {
        return new PageResult<>(
            page.content().stream()
                .map(this::toSummary)
                .toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private List<ImportMappingEntryResponse> toMappingEntries(Map<String, String> mapping) {
        return mapping.entrySet()
            .stream()
            .map(entry -> new ImportMappingEntryResponse(entry.getKey(), entry.getValue()))
            .toList();
    }

    private String valueOf(ImportType type) {
        return type == null ? null : type.name();
    }

    private String valueOf(ImportSessionStatus status) {
        return status == null ? null : status.name();
    }

    private String valueOf(Instant dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
