package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ChangeSchoolRubricVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ChangeSchoolRubricVersionStatusUseCase implements IUseCase<ChangeSchoolRubricVersionStatusCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final FrameworkRepository frameworkRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ChangeSchoolRubricVersionStatusUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            FrameworkRepository frameworkRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.frameworkRepository = frameworkRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(ChangeSchoolRubricVersionStatusCommand command) {
        // 1. Kiểm tra tài khoản School Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Lỗi tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 2. Lấy Version ra kiểm tra
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() == command.status()) {
            throw new IllegalStateException("Phiên bản đã đang ở trạng thái " + command.status() + ".");
        }

        // 3. Kiểm tra bảo mật School Ownership
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(rubric.getSchoolId()))) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể chuyển trạng thái Rubric của trường khác.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4. XỬ LÝ LOGIC MÁY TRẠNG THÁI (STATE MACHINE) CHẶT CHẼ
        if (command.status() == RubricStatus.PUBLISHED) {

            // Nếu muốn đổi sang PUBLISHED thì phải đang là DRAFT
            if (version.getStatus() != RubricStatus.DRAFT) {
                throw new IllegalStateException("Chỉ có thể ban hành (PUBLISH) phiên bản đang ở trạng thái Nháp (DRAFT).");
            }

            // KIỂM TRA FRAMEWORK GỐC
            Framework framework = frameworkRepository.findById(rubric.getFrameworkId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework) liên kết."));
            if (!framework.isActive()) {
                throw new IllegalStateException("Không thể ban hành Rubric này vì Khung tiêu chuẩn (Framework) gốc đang bị vô hiệu hóa.");
            }

            // Cập nhật currentVersionId cho Vỏ Rubric + Audit Log
//            rubric.setCurrentVersionId(version.getId());
//            rubric.setUpdatedAt(now);
//            rubric.setUpdatedBy(currentUserId);
//            rubricRepository.save(rubric);

        } else if (command.status() == RubricStatus.ARCHIVED) {
            // Không cho Archive bản đang là Current Version
            if (rubric.getCurrentVersionId() != null && rubric.getCurrentVersionId().equals(version.getId())) {
                throw new IllegalStateException("Không thể lưu trữ (ARCHIVE) phiên bản đang được áp dụng hiện hành.");
            }

        } else if (command.status() == RubricStatus.DRAFT) {
            // CHẶN LỖ HỔNG LÙI TRẠNG THÁI
            throw new IllegalStateException("Hành động bị từ chối: Không thể chuyển một phiên bản đã Ban hành/Lưu trữ quay ngược lại trạng thái Nháp (DRAFT). Vui lòng tạo phiên bản mới.");
        }

        // 5. Lưu xuống DB
        version.setStatus(command.status());
        version.setUpdatedAt(now);
        version.setUpdatedBy(currentUserId);

        rubricVersionRepository.save(version);
        return version.getId();
    }
}