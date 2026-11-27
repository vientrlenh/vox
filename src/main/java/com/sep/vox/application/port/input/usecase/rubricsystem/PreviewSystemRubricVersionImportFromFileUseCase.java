package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.PreviewRubricVersionImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricVersionImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PreviewSystemRubricVersionImportFromFileUseCase implements IUseCase<PreviewRubricVersionImportFromFileCommand, PreviewRubricVersionImportResponse> { // Changed: the return type is declared here

    private final RubricRepository rubricRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public PreviewRubricVersionImportResponse execute(PreviewRubricVersionImportFromFileCommand command) {
        requireActiveCurrentUser();
        requireSystemOwnedRubric(command.rubricId());

        ParseImportFileResult parsedResult = parseNonEmptyFile(command);
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        UUID generatedSessionId = saveSessionWithRows(command, parsedResult, expiresAt);

        return new PreviewRubricVersionImportResponse(
                generatedSessionId,
                command.file().fileName(),
                parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(),
                parsedResult.sampleRows(),
                parsedResult.totalRows(),
                expiresAt
        );
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Your account has been locked.");
        }
    }

    private void requireSystemOwnedRubric(UUID rubricId) {
        Rubric rubric = rubricRepository.findById(rubricId)
                .orElseThrow(() -> new ResourceNotFoundException("The requested system Rubric was not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("This Rubric belongs to a school. A System Admin cannot load a Version into it.");
        }
    }

    private ParseImportFileResult parseNonEmptyFile(PreviewRubricVersionImportFromFileCommand command) {
        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_VERSION);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty or contains no valid data row.");
        }
        return parsedResult;
    }

    private UUID saveSessionWithRows(PreviewRubricVersionImportFromFileCommand command, ParseImportFileResult parsedResult,
                                     Instant expiresAt) {
        // The session id stays null so the database generates the UUIDv7
        ImportSession session = ImportSession.previewedForEntity(
            null,
            ImportType.RUBRIC_VERSION,
            command.file().fileName(),
            jsonSerializationPort.toJson(parsedResult.originalHeaders()),
            jsonSerializationPort.toJson(parsedResult.suggestedMapping()),
            parsedResult.rows().size(),
            expiresAt,
            command.rubricId());

        ImportSession savedSession = importSessionRepository.save(session);
        UUID generatedSessionId = savedSession.getId();

        List<ImportRow> importRows = IntStream.range(0, parsedResult.rows().size())
                .mapToObj(i -> ImportRow.pending(
                    generatedSessionId,
                    i + 1,
                    jsonSerializationPort.toJson(parsedResult.rows().get(i)))).toList();

        importRowRepository.saveAll(importRows);
        return generatedSessionId;
    }
}
