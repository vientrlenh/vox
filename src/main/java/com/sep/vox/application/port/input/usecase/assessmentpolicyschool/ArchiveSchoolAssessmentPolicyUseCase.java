package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ArchiveSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ArchiveSchoolAssessmentPolicyUseCase implements IUseCase<ArchiveSchoolAssessmentPolicyCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ArchiveSchoolAssessmentPolicyUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricVersionRepository rubricVersionRepository,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(ArchiveSchoolAssessmentPolicyCommand command) {
        // 1. Kiểm tra tài khoản School Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra tài khoản thuộc đúng trường học yêu cầu
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        var school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 3. Kiểm tra Assessment Policy tồn tại và thuộc đúng trường học
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        // 4. Chỉ được lưu trữ (ARCHIVE) khi đang PUBLISHED
        if (policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể lưu trữ (ARCHIVE) Assessment Policy đang ở trạng thái PUBLISHED.");
        }

        // 5. Lưu trạng thái mới
        Instant now = Instant.now();
        policy.setStatus(AssessmentPolicyStatus.ARCHIVED);
        // Không cho effectiveTo lùi về trước effectiveFrom nếu policy chưa tới ngày hiệu lực
        policy.setEffectiveTo(now.isBefore(policy.getEffectiveFrom()) ? policy.getEffectiveFrom() : now);
        policy.setUpdatedAt(now);
        policy.setUpdatedBy(currentUserId);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);

        // 6. Archive Policy thì Archive luôn Phiên bản Rubric liên kết -- NHƯNG chỉ khi không còn
        // Policy nào khác đang dùng nó.
        //
        // Chú thích cũ ở đây ("1 Rubric Version chỉ gắn với đúng 1 Assessment Policy, vĩnh viễn") mô
        // tả mô hình 1-1 của V38, đã bị V44 gỡ. Từ khi nhiều Policy dùng chung được một phiên bản,
        // Archive kèm vô điều kiện sẽ rút thang chấm khỏi chân các Policy còn hiệu lực: trường thu
        // hồi chính sách của lớp thường là chính sách lớp chuyên trỏ vào một phiên bản ARCHIVED.
        if (!assessmentPolicyRepository.existsOtherActiveByRubricVersionId(policy.getRubricVersionId(), policy.getId())) {
            rubricVersionRepository.findById(policy.getRubricVersionId()).ifPresent(rubricVersion -> {
                if (rubricVersion.getStatus() == RubricStatus.PUBLISHED) {
                    rubricVersion.setStatus(RubricStatus.ARCHIVED);
                    rubricVersion.setEffectiveTo(
                            now.isBefore(rubricVersion.getEffectiveFrom()) ? rubricVersion.getEffectiveFrom() : now);
                    rubricVersion.setUpdatedAt(now);
                    rubricVersion.setUpdatedBy(currentUserId);
                    rubricVersionRepository.save(rubricVersion);
                }
            });
        }

        return saved.getId();
    }
}