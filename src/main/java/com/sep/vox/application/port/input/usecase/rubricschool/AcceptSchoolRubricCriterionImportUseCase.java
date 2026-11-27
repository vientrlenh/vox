package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptRubricCriterionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptRubricCriterionImportResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptSchoolRubricCriterionImportUseCase implements IUseCase<AcceptRubricCriterionImportCommand, AcceptRubricCriterionImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public AcceptRubricCriterionImportResponse execute(AcceptRubricCriterionImportCommand command) {
        if (command.schoolId() == null) {
            throw new IllegalArgumentException("Invalid request: the school id is missing for the School Admin flow.");
        }

        requireActiveSchoolMember(command.schoolId());
        ImportSession session = loadPreviewedSessionOfSchool(command);
        requireSchoolOwnedRubric(session, command.schoolId());

        queueForAsyncProcessing(session, command);

        return new AcceptRubricCriterionImportResponse(
                session.getId(),
                session.getTotalRows(),
                0, 0, 0,
                session.getStatus().name()
        );
    }

    private void queueForAsyncProcessing(ImportSession session, AcceptRubricCriterionImportCommand command) {
        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }

        // Hand over to the async worker: the scheduler picks up QUEUED sessions with zero attempts.
        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);

        importSessionRepository.save(session);
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account is locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("You do not belong to any school, so you cannot use this function."));

        // Security: the school id in the URL must match the caller's real school.
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("You are not allowed to operate on the data of another school.");
        }
    }

    private ImportSession loadPreviewedSessionOfSchool(AcceptRubricCriterionImportCommand command) {
        ImportSession session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("The matching import session was not found."));

        if (session.getType() != ImportType.RUBRIC_CRITERION || session.getImportedEntityId() == null) {
            throw new IllegalArgumentException("This import session is not valid.");
        }

        // Confirmation is only allowed after the preview step succeeded.
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("The session status does not allow confirming.");
        }

        // Security: a session id of another school must not be accepted through this school's URL.
        if (!session.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("This session does not belong to your school.");
        }
        return session;
    }

    private void requireSchoolOwnedRubric(ImportSession session, UUID schoolId) {
        RubricVersion version = rubricVersionRepository.findById(session.getImportedEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("The target Rubric version was not found."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("The parent Rubric to add criteria to was not found."));

        // Security: a school admin must never change system rubrics or another school's rubrics.
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Security: you may only operate on Rubrics owned by your school.");
        }
    }
}