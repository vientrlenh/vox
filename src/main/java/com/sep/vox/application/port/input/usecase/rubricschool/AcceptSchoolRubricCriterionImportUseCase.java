package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.AcceptRubricCriterionImportCommand; // Sử dụng Command dùng chung của bác
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptRubricCriterionImportResponse;
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
public class AcceptSchoolRubricCriterionImportUseCase implements IUseCase<AcceptRubricCriterionImportCommand, AcceptRubricCriterionImportResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptSchoolRubricCriterionImportUseCase(
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
    public AcceptRubricCriterionImportResponse execute(AcceptRubricCriterionImportCommand command) {
        // Validation tránh việc truyền dữ liệu thiếu trường cần thiết
        if (command.schoolId() == null) {
            throw new IllegalArgumentException("Yêu cầu không hợp lệ: Thiếu mã trường học đối với luồng School Admin.");
        }

        // Bước 1: Xác thực trạng thái tài khoản đang đăng nhập
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không tồn tại."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản bị khóa.");
        }

        // Bước 2: Bảo mật tài khoản School Admin
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Bạn không thuộc trường học nào để thực hiện chức năng này."));

        // Chống hack: Gửi mã trường trên URL khác với id trường thực tế của tài khoản
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên dữ liệu của trường khác.");
        }

        // Bước 3: Kiểm tra thông tin phiên làm việc (ImportSession)
        var session = importSessionRepository.findById(command.sessionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên làm việc Import tương ứng."));

        if (session.getType() != ImportType.RUBRIC_CRITERION || session.getImportedEntityId() == null) {
            throw new IllegalArgumentException("Phiên làm việc import này không hợp lệ.");
        }

        // Chốt chặn trạng thái: Chỉ cho phép xác nhận khi đã qua màn hình xem Preview thành công
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Trạng thái phiên làm việc không hợp lệ để thực hiện Xác nhận.");
        }

        // Chống hack: Cố tình truyền ngẫu nhiên một sessionId của trường khác vào URL trường mình nhằm phá hoại
        if (!session.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Phiên làm việc này không thuộc về trường của bạn.");
        }

        // Bước 4: Kiểm tra tính hợp lệ và quyền sở hữu đích đến (Bộ Rubric gốc)
        UUID versionId = session.getImportedEntityId();
        var version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric đích."));
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc cần bổ sung tiêu chí."));

        // Chống hack: Chặn tuyệt đối School Admin can thiệp dữ liệu vào bộ Rubric hệ thống hoặc trường bạn
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Bảo mật: Bạn chỉ được quyền thao tác trên các bộ Rubric thuộc sở hữu của trường mình.");
        }

        if (command.confirmedMapping() != null) {
            String mappingJson = jsonSerializationPort.toJson(command.confirmedMapping());
            session.setConfirmedMappingJson(mappingJson);
        }


        // Bước 5: Cập nhật trạng thái Session sang QUEUED để kích hoạt luồng xử lý ngầm (Async Worker)
        session.setStatus(ImportSessionStatus.QUEUED);
        session.setAttempts(0); // Reset số lần thử lại về 0 để Scheduler bốc việc lên chạy
        session.setUpdatedAt(Instant.now());
        session.setUpdatedBy(currentUserId);

        importSessionRepository.save(session);

        // Bước 6: Trả kết quả phản hồi nhanh chóng về cho giao diện (Frontend không phải chờ đợi lâu)
        return new AcceptRubricCriterionImportResponse(
                session.getId(),
                session.getTotalRows(),
                0, 0, 0,
                session.getStatus().name()
        );
    }
}