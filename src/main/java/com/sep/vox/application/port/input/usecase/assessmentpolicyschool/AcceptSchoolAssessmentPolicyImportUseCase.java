package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.AcceptSchoolAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptAssessmentPolicyImportResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptSchoolAssessmentPolicyImportUseCase implements IUseCase<AcceptSchoolAssessmentPolicyImportCommand, AcceptAssessmentPolicyImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public AcceptAssessmentPolicyImportResponse execute(AcceptSchoolAssessmentPolicyImportCommand command) {
        requireSchoolMember(command);
        ImportSession session = loadPreviewedSession(command);

        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }

        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);

        importSessionRepository.save(session);

        return new AcceptAssessmentPolicyImportResponse(
                session.getId(),
                session.getTotalRows(),
                0, 0, 0,
                session.getStatus().name()
        );
    }

    private void requireSchoolMember(AcceptSchoolAssessmentPolicyImportCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("The account is locked.");

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("You do not belong to any school."));
        if (!schoolUser.getSchoolId().equals(command.schoolId()))
            throw new ForbiddenException("SECURITY: The school security code does not match.");
    }

    private ImportSession loadPreviewedSession(AcceptSchoolAssessmentPolicyImportCommand command) {
        ImportSession session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Import session not found."));

        if (session.getType() != ImportType.ASSESSMENT_POLICY || session.getSchoolId() == null
                || !session.getSchoolId().equals(command.schoolId())) {
            throw new IllegalArgumentException("The session data does not belong to your school or has an invalid format.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("The session status is not valid for approval.");
        }
        return session;
    }
}
