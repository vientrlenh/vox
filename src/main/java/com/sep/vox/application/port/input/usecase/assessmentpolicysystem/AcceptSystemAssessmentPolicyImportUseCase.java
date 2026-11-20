package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AcceptSystemAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptAssessmentPolicyImportResponse;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AcceptSystemAssessmentPolicyImportUseCase implements IUseCase<AcceptSystemAssessmentPolicyImportCommand, AcceptAssessmentPolicyImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptSystemAssessmentPolicyImportUseCase(
            ImportSessionRepository importSessionRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public AcceptAssessmentPolicyImportResponse execute(AcceptSystemAssessmentPolicyImportCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        var session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên làm việc Import (Session ID không hợp lệ)."));

        if (session.getType() != ImportType.ASSESSMENT_POLICY || session.getSchoolId() != null) {
            throw new IllegalArgumentException("Phiên làm việc này không phải là Import Assessment Policy hệ thống.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Chỉ có thể Accept những phiên làm việc đang ở trạng thái PREVIEWED.");
        }

        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }

        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);
        session.setUpdatedAt(Instant.now());
        session.setUpdatedBy(currentUserId);

        importSessionRepository.save(session);

        return new AcceptAssessmentPolicyImportResponse(
                session.getId(),
                session.getTotalRows(),
                0, 0, 0,
                session.getStatus().name()
        );
    }
}
