package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.PreviewRubricResultBandImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewRubricResultBandImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
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
public class PreviewSchoolRubricResultBandImportFromFileUseCase implements IUseCase<PreviewRubricResultBandImportCommand, PreviewRubricResultBandImportResponse> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public PreviewRubricResultBandImportResponse execute(PreviewRubricResultBandImportCommand command) {
        if (command.schoolId() == null) {
            throw new IllegalArgumentException("Invalid request: the school id is missing for the School Admin flow.");
        }

        requireActiveSchoolMember(command.schoolId());
        requireSchoolOwnedRubricVersion(command);

        ParseImportFileResult parsedResult = parseNonEmptyFile(command);

        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        UUID generatedSessionId = saveSessionWithRows(command.schoolId(), ImportType.RUBRIC_RESULT_BAND,
                command.file().fileName(), command.rubricVersionId(), parsedResult, expiresAt);

        return new PreviewRubricResultBandImportResponse(
                generatedSessionId,
                command.file().fileName(),
                parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(),
                parsedResult.sampleRows(),
                parsedResult.totalRows(),
                expiresAt
        );
    }

    private ParseImportFileResult parseNonEmptyFile(PreviewRubricResultBandImportCommand command) {
        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_RESULT_BAND);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty or contains no valid data.");
        }
        return parsedResult;
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("The account is locked.");

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("You do not belong to any school, so you cannot use this function."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("You are not allowed to operate on the data of another school.");
        }
    }

    private void requireSchoolOwnedRubricVersion(PreviewRubricResultBandImportCommand command) {
        RubricVersion version = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("The requested Rubric version was not found."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("The matching parent Rubric was not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Security: this Rubric is not owned by your school.");
        }
    }

    private UUID saveSessionWithRows(UUID schoolId, ImportType importType, String fileName, UUID importedEntityId,
                                     ParseImportFileResult parsedResult, Instant expiresAt) {
        ImportSession session = ImportSession.previewedForEntity(
            schoolId,
            importType,
            fileName,
            jsonSerializationPort.toJson(parsedResult.originalHeaders()),
            jsonSerializationPort.toJson(parsedResult.suggestedMapping()),
            parsedResult.rows().size(),
            expiresAt,
            importedEntityId);
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