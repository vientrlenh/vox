package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.PreviewSchoolRubricVersionImportCommand;
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
public class PreviewSchoolRubricVersionImportFromFileUseCase implements IUseCase<PreviewSchoolRubricVersionImportCommand, PreviewRubricVersionImportResponse> {

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
    public PreviewRubricVersionImportResponse execute(PreviewSchoolRubricVersionImportCommand command) {
        requireActiveSchoolMember(command.schoolId());
        requireSchoolOwnedRubric(command);

        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.RUBRIC_VERSION);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty or contains no data.");
        }

        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        // Hard-bind the school id to the import session.
        UUID generatedSessionId = saveSessionWithRows(command.schoolId(), ImportType.RUBRIC_VERSION,
                command.file().fileName(), command.rubricId(), parsedResult, expiresAt);

        return new PreviewRubricVersionImportResponse(
                generatedSessionId, command.file().fileName(), parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(), parsedResult.sampleRows(), parsedResult.totalRows(), expiresAt
        );
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Your account has been locked.");

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Does not belong to any school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) throw new ForbiddenException("Intrusion into the data of another school was denied.");
    }

    private void requireSchoolOwnedRubric(PreviewSchoolRubricVersionImportCommand command) {
        Rubric rubric = rubricRepository.findById(command.rubricId()).orElseThrow(() -> new ResourceNotFoundException("The requested Rubric was not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || rubric.getSchoolId() == null || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("This Rubric is not managed by your school.");
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