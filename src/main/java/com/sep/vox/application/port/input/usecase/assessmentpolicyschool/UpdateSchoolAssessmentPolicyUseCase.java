package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.UpdateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
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
public class UpdateSchoolAssessmentPolicyUseCase implements IUseCase<UpdateAssessmentPolicyCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService;

    @Override
    @Transactional
    public UUID execute(UpdateAssessmentPolicyCommand command) {
        // 1. Check the School Admin account
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Check that the account belongs to the requested school
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

        // 3. Check that the Assessment Policy exists and belongs to the school
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào Assessment Policy của trường khác.");
        }

        // 4. It can only be edited while it is DRAFT
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể cập nhật Assessment Policy khi đang ở trạng thái DRAFT.");
        }

        // 5. Check the target Band (frameworkVersionId stays the same and cannot be changed)
        FrameworkResultBand targetBand = frameworkResultBandRepository.findById(command.targetFrameworkBandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Band mục tiêu (targetFrameworkBandId)."));
        if (!targetBand.getFrameworkVersionId().equals(policy.getFrameworkVersionId())) {
            throw new IllegalArgumentException("Band mục tiêu phải thuộc đúng Phiên bản Khung tiêu chuẩn của Policy này.");
        }

        // 5b. Band ceiling per grade level. The scope cannot be edited here, so it is taken straight from the existing policy --
        //     but the Band CAN be edited, so without this check a policy created within the ceiling would still be
        //     pushed past the ceiling right afterwards.
        gradeLevelBandScopeGuardService.assertWithinScope(
                policy.getGradeLevelId(), policy.getSchoolGradeId(), policy.getSchoolClassId(),
                policy.getFrameworkVersionId(), targetBand);

        // 6. Validate the effective period against the end date that will be stored (omitted keeps the stored one)
        if (command.effectiveFrom() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu hiệu lực (effectiveFrom) không được để trống.");
        }
        Instant effectiveTo = command.effectiveToProvided() ? command.effectiveTo() : policy.getEffectiveTo();
        if (effectiveTo != null && command.effectiveFrom().isAfter(effectiveTo)) {
            throw new IllegalArgumentException("Ngày bắt đầu hiệu lực không được sau ngày kết thúc hiệu lực.");
        }

        // 7. Update the business fields (frameworkVersionId/rubricVersionId/languageId/scope do not change)
        // An omitted passingScore/strictness keeps the stored value; an explicit null clears passingScore and stores STANDARD.
        policy.setTargetFrameworkBandId(command.targetFrameworkBandId());
        if (command.passingScoreProvided()) {
            policy.setPassingScore(command.passingScore());
        }
        if (command.strictnessProvided()) {
            policy.setStrictness(command.strictness() != null ? command.strictness() : AssessmentPolicyStrictness.STANDARD);
        }
        policy.setEffectiveFrom(command.effectiveFrom());
        policy.setEffectiveTo(effectiveTo);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);
        return saved.getId();
    }
}
