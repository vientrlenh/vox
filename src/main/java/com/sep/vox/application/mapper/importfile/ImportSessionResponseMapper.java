package com.sep.vox.application.mapper.importfile;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.importfile.ImportMappingEntryResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSession;

@Service
@RequiredArgsConstructor
public class ImportSessionResponseMapper {

    private final JsonSerializationPort jsonSerializationPort;

    public ImportSessionDetailsResponse toDetails(ImportSession session) {
        return new ImportSessionDetailsResponse(
            session.getId(),
            session.getSchoolId(),
            session.getType(),
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
            session.getStatus(),
            session.getImportedEntityId(),
            session.getExpiresAt(),
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }

    public ImportSessionSummaryResponse toSummary(ImportSession session) {
        return new ImportSessionSummaryResponse(
            session.getId(),
            session.getSchoolId(),
            session.getType(),
            session.getFileName(),
            session.getTotalRows(),
            session.getValidRows(),
            session.getInvalidRows(),
            session.getImportedRows(),
            session.getSkippedRows(),
            session.getStatus(),
            session.getExpiresAt(),
            session.getCreatedAt(),
            session.getUpdatedAt()
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
}
