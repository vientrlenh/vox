package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ChangeSchoolRubricVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.service.rubric.RubricScoringConsistencyValidator;
import java.time.Instant;
import java.util.UUID;

@Service
public class ChangeSchoolRubricVersionStatusUseCase implements IUseCase<ChangeSchoolRubricVersionStatusCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final FrameworkRepository frameworkRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository; // Dùng Repo mới

    public ChangeSchoolRubricVersionStatusUseCase(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            FrameworkRepository frameworkRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.frameworkRepository = frameworkRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
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

        // 3. KIỂM TRA BẢO MẬT SCHOOL OWNERSHIP (Sử dụng bảng SchoolUser mới)
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc."));

        // Lấy thông tin mapping trường học của User hiện tại
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        // Xác thực quyền can thiệp
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể chuyển trạng thái Rubric của trường khác.");
        }

        Instant now = Instant.now();

        // 4. XỬ LÝ LOGIC MÁY TRẠNG THÁI (STATE MACHINE) CHẶT CHẼ
        if (command.status() == RubricStatus.PUBLISHED) {

            if (version.getStatus() != RubricStatus.DRAFT) {
                throw new IllegalStateException("Chỉ có thể ban hành (PUBLISH) phiên bản đang ở trạng thái Nháp (DRAFT).");
            }

            // KIỂM TRA VERSION KHÔNG ĐƯỢC RỖNG -- version không có tiêu chí/thang điểm thì không dùng
            // để chấm bài được, publish xong sẽ vô dụng và không thể sửa nữa (ngoại trừ archive)
            if (rubricCriterionRepository.findByRubricVersionId(version.getId()).isEmpty()) {
                throw new IllegalStateException("Không thể ban hành phiên bản này vì chưa có tiêu chí (Criterion) nào.");
            }

            Framework framework = frameworkRepository.findById(rubric.getFrameworkId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework) liên kết."));
            if (!framework.isActive()) {
                throw new IllegalStateException("Không thể ban hành Rubric này vì Khung tiêu chuẩn (Framework) gốc đang bị vô hiệu hóa.");
            }

            // KIỂM TRA ASSESSMENT POLICY LIÊN KẾT ĐÃ ĐƯỢC PUBLISHED HAY CHƯA
            if (!assessmentPolicyRepository.existsPublishedByRubricVersionId(version.getId())) {
                throw new IllegalStateException("Không thể ban hành Rubric này vì chưa có Assessment Policy nào liên kết đang ở trạng thái PUBLISHED.");
            }

            RubricScoringConsistencyValidator.assertPublishable(
                    version.getTotalScoreMethod(),
                    version.getScoringScaleMin(),
                    version.getScoringScaleMax(),
                    rubricCriterionRepository.findByRubricVersionId(version.getId()));

        } else if (command.status() == RubricStatus.ARCHIVED) {
            throw new IllegalStateException("Hành động bị từ chối: Vui lòng dùng chức năng Lưu trữ (Archive) riêng để chuyển phiên bản sang ARCHIVED.");
        } else if (command.status() == RubricStatus.DRAFT) {
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
