package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.ArchiveSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchiveSchoolAssessmentPolicyUseCase implements IUseCase<ArchiveSchoolAssessmentPolicyCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

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
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được phân bổ vào trường học nào."));
        if (!schoolUser.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        School school = schoolRepository.findById(command.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }

        // 3. Kiểm tra Assessment Policy tồn tại và thuộc đúng trường học
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        // 4. Chỉ được lưu trữ (ARCHIVE) khi đang PUBLISHED
        if (policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ có thể lưu trữ (ARCHIVE) Assessment Policy đang ở trạng thái PUBLISHED.");
        }

        // 5. Lưu trạng thái mới
        Instant now = Instant.now();
        policy.setStatus(AssessmentPolicyStatus.ARCHIVED);
        // Do not let effectiveTo move back before effectiveFrom when the policy's effective date has not been reached yet
        policy.setEffectiveTo(now.isBefore(policy.getEffectiveFrom()) ? policy.getEffectiveFrom() : now);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);

        // 6. Archiving a Policy also archives the linked Rubric Version -- BUT only when no other
        // Policy is still using it.
        //
        // The old comment here ("1 Rubric Version is tied to exactly 1 Assessment Policy, permanently")
        // described the 1-1 model of V38, which V44 removed. Since several Policies can share one version,
        // archiving unconditionally would pull the scoring scale out from under the Policies that are still
        // in effect: when a school withdraws the regular-class policy, the specialised-class policy is often
        // the one that points at an ARCHIVED version.
        if (!assessmentPolicyRepository.existsOtherActiveByRubricVersionId(policy.getRubricVersionId(), policy.getId())) {
            rubricVersionRepository.findById(policy.getRubricVersionId()).ifPresent(rubricVersion -> {
                if (rubricVersion.getStatus() == RubricStatus.PUBLISHED) {
                    rubricVersion.setStatus(RubricStatus.ARCHIVED);
                    rubricVersion.setEffectiveTo(
                            now.isBefore(rubricVersion.getEffectiveFrom()) ? rubricVersion.getEffectiveFrom() : now);
                    rubricVersionRepository.save(rubricVersion);
                }
            });
        }

        return saved.getId();
    }
}