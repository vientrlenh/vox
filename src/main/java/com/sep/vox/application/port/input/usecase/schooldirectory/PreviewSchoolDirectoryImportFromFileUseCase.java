package com.sep.vox.application.port.input.usecase.schooldirectory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

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
public class PreviewSchoolDirectoryImportFromFileUseCase
        implements IUseCase<PreviewSchoolDirectoryImportFromFileCommand, PreviewSchoolDirectoryImportResponse> {

    private static final int SESSION_EXPIRY_DAYS = 1;

    private final FileProcessingPort fileProcessingPort;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public PreviewSchoolDirectoryImportFromFileUseCase(
            FileProcessingPort fileProcessingPort,
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort) {
        this.fileProcessingPort = fileProcessingPort;
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public PreviewSchoolDirectoryImportResponse execute(PreviewSchoolDirectoryImportFromFileCommand input) {
        if (input == null || input.file() == null) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var parsed = fileProcessingPort.parse(input.file(), ImportType.SCHOOL_DIRECTORY);
        var expiresAt = now.plusDays(SESSION_EXPIRY_DAYS);
        var savedSession = importSessionRepository.save(createSession(input, parsed, currentUserId, now, expiresAt));
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
            OffsetDateTime now,
            OffsetDateTime expiresAt) {
        return new ImportSession(
            null, // system-level import: not scoped to a school
            ImportType.SCHOOL_DIRECTORY,
            safeFileName(input.file().fileName()),
            jsonSerializationPort.toJson(parsed.originalHeaders()),
            jsonSerializationPort.toJson(parsed.suggestedMapping()),
            null,
            0L,
            0L,
            0L,
            0L,
            parsed.totalRows(),
            null,
            ImportSessionStatus.PREVIEWED,
            null,
            expiresAt, 
            null, 
            null, 
            null, 
            0, 
            now,
            now,
            currentUserId,
            currentUserId
        );
    }

    private void saveRows(UUID sessionId, ParseImportFileResult parsed) {
        if (parsed.rows().isEmpty()) {
            return;
        }
        var rows = new ArrayList<ImportRow>();
        var rowNumber = 1L;
        for (Map<String, String> rawRow : parsed.rows()) {
            rows.add(new ImportRow(
                sessionId,
                rowNumber,
                jsonSerializationPort.toJson(rawRow),
                null,
                null,
                ImportRowStatus.PENDING
            ));
            rowNumber++;
        }
        importRowRepository.saveAll(rows);
    }

    private String safeFileName(String fileName) {
        return fileName == null || fileName.isBlank() ? "import-file" : fileName;
    }
}
