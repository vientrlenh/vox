package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.AcceptSystemAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptAssessmentPolicyImportResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptSystemAssessmentPolicyImportUseCase implements IUseCase<AcceptSystemAssessmentPolicyImportCommand, AcceptAssessmentPolicyImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public AcceptAssessmentPolicyImportResponse execute(AcceptSystemAssessmentPolicyImportCommand command) {
        requireActiveUser();
        ImportSession session = loadPreviewedSystemSession(command);

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

    private void requireActiveUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("The account is locked.");
    }

    private ImportSession loadPreviewedSystemSession(AcceptSystemAssessmentPolicyImportCommand command) {
        ImportSession session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Import session not found (invalid session ID)."));

        if (session.getType() != ImportType.ASSESSMENT_POLICY || session.getSchoolId() != null) {
            throw new IllegalArgumentException("This session is not a system Assessment Policy import.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("Only a session in the PREVIEWED status can be accepted.");
        }
        return session;
    }
}
