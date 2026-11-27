package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.PreviewSchoolAssessmentPolicyImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.PreviewAssessmentPolicyImportResponse;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
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
public class PreviewSchoolAssessmentPolicyImportFromFileUseCase implements IUseCase<PreviewSchoolAssessmentPolicyImportFromFileCommand, PreviewAssessmentPolicyImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final FileProcessingPort fileProcessingPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public PreviewAssessmentPolicyImportResponse execute(PreviewSchoolAssessmentPolicyImportFromFileCommand command) {
        requireActiveSchoolMember(command.schoolId());

        ParseImportFileResult parsedResult = fileProcessingPort.parse(command.file(), ImportType.ASSESSMENT_POLICY);
        if (parsedResult.rows() == null || parsedResult.rows().isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty or contains no data.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(1, ChronoUnit.DAYS);

        UUID generatedSessionId = savePreviewedSession(command, parsedResult, expiresAt);
        savePendingRows(generatedSessionId, parsedResult);

        return new PreviewAssessmentPolicyImportResponse(
                generatedSessionId, command.file().fileName(), parsedResult.originalHeaders(),
                parsedResult.suggestedMapping(), parsedResult.sampleRows(), parsedResult.totalRows(), expiresAt
        );
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Your account has been locked.");

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not assigned to any school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: You are not allowed to upload a file for another school.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school has been deactivated in the system.");
        }
    }

    private UUID savePreviewedSession(
            PreviewSchoolAssessmentPolicyImportFromFileCommand command, ParseImportFileResult parsedResult, Instant expiresAt) {
        ImportSession session = ImportSession.previewed(
            command.schoolId(),
            ImportType.ASSESSMENT_POLICY,
            command.file().fileName(),
            jsonSerializationPort.toJson(parsedResult.originalHeaders()),
            jsonSerializationPort.toJson(parsedResult.suggestedMapping()),
            parsedResult.rows().size(),
            expiresAt);

        ImportSession savedSession = importSessionRepository.save(session);
        return savedSession.getId();
    }

    private void savePendingRows(UUID generatedSessionId, ParseImportFileResult parsedResult) {
        List<ImportRow> importRows = IntStream.range(0, parsedResult.rows().size())
                .mapToObj(i -> ImportRow.pending(
                    generatedSessionId,
                    i + 1,
                    jsonSerializationPort.toJson(parsedResult.rows().get(i)))).toList();

        importRowRepository.saveAll(importRows);
    }
}
