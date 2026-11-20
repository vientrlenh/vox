package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AcceptRubricResultBandImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptRubricResultBandImportResponse;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AcceptSchoolRubricResultBandImportUseCase implements IUseCase<AcceptRubricResultBandImportCommand, AcceptRubricResultBandImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptSchoolRubricResultBandImportUseCase(
            ImportSessionRepository importSessionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort, JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public AcceptRubricResultBandImportResponse execute(AcceptRubricResultBandImportCommand command) {
        if (command.schoolId() == null) {
            throw new IllegalArgumentException("Yêu cầu không hợp lệ: Thiếu mã trường học.");
        }

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        var schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) throw new ForbiddenException("BẢO MẬT: Mã bảo mật trường học không trùng khớp.");

        var session = importSessionRepository.findById(command.sessionId()).orElseThrow(() -> new NotFoundException("Không tìm thấy phiên làm việc Import."));

        // Xác minh chéo session thuộc về đúng trường học
        if (session.getType() != ImportType.RUBRIC_RESULT_BAND || session.getImportedEntityId() == null
                || session.getSchoolId() == null || !session.getSchoolId().equals(command.schoolId())) {
            throw new IllegalArgumentException("Dữ liệu phiên làm việc không hợp lệ hoặc không thuộc trường của bạn.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Phiên làm việc phải ở trạng thái PREVIEWED mới có thể thực hiện Xác nhận.");
        }

        var version = rubricVersionRepository.findById(session.getImportedEntityId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric tương ứng."));
        var rubric = rubricRepository.findById(version.getRubricId()).orElseThrow();

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này không thuộc sở hữu của trường học bạn.");
        }

        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }

        // Đẩy trạng thái sang QUEUED
        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0);
        session.setUpdatedAt(Instant.now());
        session.setUpdatedBy(currentUserId);
        importSessionRepository.save(session);

        return new AcceptRubricResultBandImportResponse(session.getId(), session.getTotalRows(), 0, 0, 0, session.getStatus().name());
    }
}