package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AcceptSchoolRubricVersionImportCommand;
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
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AcceptSchoolRubricVersionImportUseCase implements IUseCase<AcceptSchoolRubricVersionImportCommand, AcceptRubricVersionImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptSchoolRubricVersionImportUseCase(
            ImportSessionRepository importSessionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort, JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public AcceptRubricVersionImportResponse execute(AcceptSchoolRubricVersionImportCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        var schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Không thuộc trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId()))
            throw new ForbiddenException("BẢO MẬT: Mã bảo mật trường học không trùng khớp.");

        var session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên làm việc Import."));

        if (session.getType() != ImportType.RUBRIC_VERSION || session.getImportedEntityId() == null
                || session.getSchoolId() == null || !session.getSchoolId().equals(command.schoolId())) {
            throw new IllegalArgumentException("Dữ liệu phiên làm việc không thuộc trường học của bạn hoặc sai cấu trúc định dạng.");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Trạng thái phiên làm việc không hợp lệ để phê duyệt.");
        }

        var rubricId = session.getImportedEntityId();
        var rubric = rubricRepository.findById(rubricId).orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bảo mật: Bộ Rubric này không thuộc sở hữu của trường học bạn.");
        }

        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }

        //  CHÌA KHÓA CỦA KIẾN TRÚC ASYNC: Chỉ đổi trạng thái và kết thúc
        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0); // Reset số lần thử để Scheduler nhận diện
        session.setUpdatedAt(OffsetDateTime.now());
        session.setUpdatedBy(currentUserId);

        importSessionRepository.save(session);

        // Trả kết quả thành công ngay lập tức để UI không bị treo
        return new AcceptRubricVersionImportResponse(
                session.getId(),
                session.getTotalRows(),
                0, 0, 0,
                session.getStatus().name()
        );
    }
}