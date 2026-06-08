package com.sep.vox.application.mapper.importfile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.sep.vox.application.common.JsonSerialization;
import com.sep.vox.application.response.input.importfile.ImportMappingEntryResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;

public final class ImportSessionResponseMapper {

    private ImportSessionResponseMapper() {
    }

    public static ImportSessionDetailsResponse toDetails(ImportSession session) {
        return new ImportSessionDetailsResponse(
            session.getId(),
            session.getSchoolId(),
            valueOf(session.getType()),
            session.getFileName(),
            JsonSerialization.toStringList(session.getOriginalHeadersJson()),
            toMappingEntries(JsonSerialization.toStringMap(session.getSuggestedMappingJson())),
            toMappingEntries(JsonSerialization.toStringMap(session.getConfirmedMappingJson())),
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

    public static ImportSessionSummaryResponse toSummary(ImportSession session) {
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

    public static PageResult<ImportSessionSummaryResponse> toSummaryPage(PageResult<ImportSession> page) {
        return new PageResult<>(
            page.content().stream()
                .map(ImportSessionResponseMapper::toSummary)
                .toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static List<ImportMappingEntryResponse> toMappingEntries(Map<String, String> mapping) {
        return mapping.entrySet()
            .stream()
            .map(entry -> new ImportMappingEntryResponse(entry.getKey(), entry.getValue()))
            .toList();
    }

    private static String valueOf(ImportType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(ImportSessionStatus status) {
        return status == null ? null : status.name();
    }

    private static String valueOf(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}
