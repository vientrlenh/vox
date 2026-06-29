package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AcceptSystemRubricVersionImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptRubricVersionImportResponse;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AcceptSystemRubricVersionImportUseCase implements IUseCase<AcceptSystemRubricVersionImportCommand, AcceptRubricVersionImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptSystemRubricVersionImportUseCase(
            ImportSessionRepository importSessionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public AcceptRubricVersionImportResponse execute(AcceptSystemRubricVersionImportCommand command) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 2. Kiểm tra tính hợp lệ của phiên Import (ImportSession)
        var session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên làm việc Import (Session ID không hợp lệ)."));

        if (session.getType() != ImportType.RUBRIC_VERSION || session.getImportedEntityId() == null) {
            throw new IllegalArgumentException("Phiên làm việc này không phải là Import Rubric Version.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Chỉ có thể Accept những phiên làm việc đang ở trạng thái PREVIEWED.");
        }

        // 3. Kiểm tra bảo mật đích đến (Rubric gốc phải thuộc hệ thống)
        var rubricId = session.getImportedEntityId();
        var rubric = rubricRepository.findById(rubricId).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này thuộc trường học. System Admin không được phép can thiệp.");
        }

        //  4: CHỐT SỔ MAPPING VÀ GHI ĐÈ VÀO DATABASE
        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }

        //  5: Cập nhật trạng thái chu kỳ sang QUEUED để Background Worker bốc việc
        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);
        session.setUpdatedAt(OffsetDateTime.now());
        session.setUpdatedBy(currentUserId);

        importSessionRepository.save(session);

        // 6. Phản hồi ngay lập tức cho Frontend xoay loading chờ xử lý ngầm
        return new AcceptRubricVersionImportResponse(
                session.getId(),
                session.getTotalRows(),
                0, 0, 0,
                session.getStatus().name()
        );
    }
}