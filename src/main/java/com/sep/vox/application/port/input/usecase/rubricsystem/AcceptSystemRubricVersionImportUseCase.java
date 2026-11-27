package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptSystemRubricVersionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptRubricVersionImportResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptSystemRubricVersionImportUseCase implements IUseCase<AcceptSystemRubricVersionImportCommand, AcceptRubricVersionImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public AcceptRubricVersionImportResponse execute(AcceptSystemRubricVersionImportCommand command) {
        requireActiveCurrentUser();
        ImportSession session = loadPreviewedSession(command);
        requireSystemOwnedRubric(session);

        queueForAsyncProcessing(session, command);

        return new AcceptRubricVersionImportResponse(
                session.getId(),
                session.getTotalRows(),
                0, 0, 0,
                session.getStatus().name()
        );
    }

    private void queueForAsyncProcessing(ImportSession session, AcceptSystemRubricVersionImportCommand command) {
        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }

        // Hand over to the async worker: the scheduler picks up QUEUED sessions with zero attempts.
        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);

        importSessionRepository.save(session);
    }

    private void requireActiveCurrentUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");
    }

    private ImportSession loadPreviewedSession(AcceptSystemRubricVersionImportCommand command) {
        ImportSession session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên làm việc Import (Session ID không hợp lệ)."));

        if (session.getType() != ImportType.RUBRIC_VERSION || session.getImportedEntityId() == null) {
            throw new IllegalArgumentException("Phiên làm việc này không phải là Import Rubric Version.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("Chỉ có thể Accept những phiên làm việc đang ở trạng thái PREVIEWED.");
        }
        return session;
    }

    // Security: the parent rubric must be a SYSTEM rubric.
    private void requireSystemOwnedRubric(ImportSession session) {
        UUID rubricId = session.getImportedEntityId();
        Rubric rubric = rubricRepository.findById(rubricId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này thuộc trường học. System Admin không được phép can thiệp.");
        }
    }
}
