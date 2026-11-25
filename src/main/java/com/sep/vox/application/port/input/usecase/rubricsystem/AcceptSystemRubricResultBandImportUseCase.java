package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptRubricResultBandImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptRubricResultBandImportResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptSystemRubricResultBandImportUseCase implements IUseCase<AcceptRubricResultBandImportCommand, AcceptRubricResultBandImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public AcceptRubricResultBandImportResponse execute(AcceptRubricResultBandImportCommand command) {
        requireActiveCurrentUser();
        ImportSession session = loadPreviewedSession(command);
        requireSystemOwnedRubric(session);

        queueForAsyncProcessing(session, command);

        return new AcceptRubricResultBandImportResponse(
                session.getId(),
                session.getTotalRows(),
                0, 0, 0,
                session.getStatus().name()
        );
    }

    private void queueForAsyncProcessing(ImportSession session, AcceptRubricResultBandImportCommand command) {
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
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa.");
        }
    }

    private ImportSession loadPreviewedSession(AcceptRubricResultBandImportCommand command) {
        ImportSession session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên làm việc Import tương ứng."));

        if (session.getType() != ImportType.RUBRIC_RESULT_BAND || session.getImportedEntityId() == null) {
            throw new IllegalArgumentException("Dữ liệu cấu trúc phiên làm việc không đúng định dạng Import X xếp loại.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("Phiên làm việc phải ở trạng thái PREVIEWED mới có thể thực hiện Xác nhận.");
        }
        return session;
    }

    // Security: the target rubric version must belong to a SYSTEM rubric.
    private void requireSystemOwnedRubric(ImportSession session) {
        UUID versionId = session.getImportedEntityId();
        RubricVersion version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phiên bản Rubric tương ứng."));
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric đích thuộc về trường học. System Admin không có quyền can thiệp.");
        }
    }
}
