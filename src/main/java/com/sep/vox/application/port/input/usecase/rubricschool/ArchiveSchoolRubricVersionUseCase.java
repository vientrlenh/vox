package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ArchiveSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ArchiveSchoolRubricVersionUseCase implements IUseCase<ArchiveSchoolRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;

    public ArchiveSchoolRubricVersionUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            AssessmentPolicyRepository assessmentPolicyRepository) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
    }

    @Override
    @Transactional
    public UUID execute(ArchiveSchoolRubricVersionCommand command) {
        // 1. Xác thực tài khoản
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Lấy phiên bản và kiểm tra quyền sở hữu bằng bảng school_user
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric này."));

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc."));

        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào phiên bản của trường khác.");
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 3. Chỉ được lưu trữ (ARCHIVE) khi đang PUBLISHED
        if (version.getStatus() != RubricStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể lưu trữ (ARCHIVE) phiên bản đang ở trạng thái PUBLISHED.");
        }

        // 3b. Chặn archive trực tiếp nếu còn Assessment Policy đang DRAFT/PUBLISHED dùng version này --
        // archive vô điều kiện sẽ để lại Policy còn hiệu lực trỏ vào một version đã ARCHIVED. Đối xứng
        // với existsOtherActiveByRubricVersionId ở chiều archive Policy (ArchiveSchoolAssessmentPolicyUseCase).
        if (assessmentPolicyRepository.existsActiveByRubricVersionId(version.getId())) {
            throw new IllegalStateException(
                    "Không thể lưu trữ phiên bản này vì còn Assessment Policy đang DRAFT/PUBLISHED sử dụng. "
                            + "Hãy lưu trữ các Assessment Policy đó trước.");
        }

        // 4. Lưu trạng thái mới
        Instant now = Instant.now();
        version.setStatus(RubricStatus.ARCHIVED);
        // Không cho effectiveTo lùi về trước effectiveFrom nếu version chưa tới ngày hiệu lực
        version.setEffectiveTo(now.isBefore(version.getEffectiveFrom()) ? version.getEffectiveFrom() : now);
        version.setUpdatedAt(now);
        version.setUpdatedBy(currentUserId);

        RubricVersion saved = rubricVersionRepository.save(version);
        return saved.getId();
    }
}