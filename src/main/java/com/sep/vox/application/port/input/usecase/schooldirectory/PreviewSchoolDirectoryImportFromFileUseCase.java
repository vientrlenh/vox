package com.sep.vox.application.port.input.usecase.schooldirectory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.PreviewSchoolDirectoryImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooldirectory.PreviewSchoolDirectoryImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
@RequiredArgsConstructor
public class PreviewSchoolDirectoryImportFromFileUseCase
        implements IUseCase<PreviewSchoolDirectoryImportFromFileCommand, PreviewSchoolDirectoryImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public PreviewSchoolDirectoryImportResponse execute(PreviewSchoolDirectoryImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        ParseImportFileResult parsed = fileProcessingPort.parse(input.file(), ImportType.SCHOOL_DIRECTORY);
        Instant expiresAt = now.plus(SESSION_EXPIRY_DAYS, ChronoUnit.DAYS);
        ImportSession savedSession = importSessionRepository.save(createSession(input, parsed, currentUserId, now, expiresAt));
        saveRows(savedSession.getId(), parsed);

        return new PreviewSchoolDirectoryImportResponse(
            savedSession.getId(),
            safeFileName(input.file().fileName()),
            parsed.originalHeaders(),
            parsed.suggestedMapping(),
            parsed.sampleRows(),
            parsed.totalRows(),
            expiresAt.toString()
        );
    }

    private ImportSession createSession(
            PreviewSchoolDirectoryImportFromFileCommand input,
            ParseImportFileResult parsed,
            UUID currentUserId,
            Instant now,
            Instant expiresAt) {
        return ImportSession.builder()
            // system-level import: not scoped to a school
            .schoolId(null)
            .type(ImportType.SCHOOL_DIRECTORY)
            .fileName(safeFileName(input.file().fileName()))
            .originalHeadersJson(jsonSerializationPort.toJson(parsed.originalHeaders()))
            .suggestedMappingJson(jsonSerializationPort.toJson(parsed.suggestedMapping()))
            .confirmedMappingJson(null)
            .validRows(0L)
            .invalidRows(0L)
            .importedRows(0L)
            .skippedRows(0L)
            .totalRows(parsed.totalRows())
            .failureReason(null)
            .status(ImportSessionStatus.PREVIEWED)
            .importedEntityId(null)
            .expiresAt(expiresAt)
            .claimedAt(null)
            .claimedBy(null)
            .leaseExpiresAt(null)
            .attempts(0)
            .build();
    }

    private void saveRows(UUID sessionId, ParseImportFileResult parsed) {
        if (parsed.rows().isEmpty()) {
            return;
        }
        List<ImportRow> rows = new ArrayList<ImportRow>();
        long rowNumber = 1L;
        for (Map<String, String> rawRow : parsed.rows()) {
            rows.add(ImportRow.builder()
                .sessionId(sessionId)
                .rowNumber(rowNumber)
                .rawDataJson(jsonSerializationPort.toJson(rawRow))
                .mappedDataJson(null)
                .errorsJson(null)
                .status(ImportRowStatus.PENDING)
                .build());
            rowNumber++;
        }
        importRowRepository.saveAll(rows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "import-file" : fileName;
    }
}
