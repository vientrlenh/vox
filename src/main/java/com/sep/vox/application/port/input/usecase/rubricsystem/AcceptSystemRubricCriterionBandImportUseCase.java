package com.sep.vox.application.port.input.usecase.rubricsystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AcceptRubricCriterionBandImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptRubricCriterionBandImportResponse;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AcceptSystemRubricCriterionBandImportUseCase implements IUseCase<AcceptRubricCriterionBandImportCommand, AcceptRubricCriterionBandImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort  jsonSerializationPort;

    public AcceptSystemRubricCriterionBandImportUseCase(
            ImportSessionRepository importSessionRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort, JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public AcceptRubricCriterionBandImportResponse execute(AcceptRubricCriterionBandImportCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        var session = importSessionRepository.findById(command.sessionId()).orElseThrow(() -> new NotFoundException("Không tìm thấy phiên làm việc Import."));

        if (session.getType() != ImportType.RUBRIC_CRITERION_BAND || session.getImportedEntityId() == null) {
            throw new IllegalArgumentException("Phiên làm việc không hợp lệ.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Trạng thái không hợp lệ để phê duyệt.");
        }

        var criterion = rubricCriterionRepository.findById(session.getImportedEntityId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí gốc."));
        var version = rubricVersionRepository.findById(criterion.getRubricVersionId()).orElseThrow();
        var rubric = rubricRepository.findById(version.getRubricId()).orElseThrow();

        if (rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            throw new ForbiddenException("Bảo mật: Tiêu chí này thuộc về trường học.");
        }

        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }


        // Đẩy xuống hàng đợi ngầm
        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);
        session.setUpdatedAt(OffsetDateTime.now());
        session.setUpdatedBy(currentUserId);
        importSessionRepository.save(session);

        return new AcceptRubricCriterionBandImportResponse(session.getId(), session.getTotalRows(), 0, 0, 0, session.getStatus().name());
    }
}