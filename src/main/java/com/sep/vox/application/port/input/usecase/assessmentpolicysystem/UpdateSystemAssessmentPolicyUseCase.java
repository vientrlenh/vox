package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSystemAssessmentPolicyUseCase implements IUseCase<UpdateAssessmentPolicyCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public UpdateSystemAssessmentPolicyUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateAssessmentPolicyCommand command) {
        // 1. Kiểm tra tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Lấy Assessment Policy
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));

        // 3. Check quyền SYSTEM
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể can thiệp vào Assessment Policy của trường học.");
        }

        // 4. Chỉ được sửa khi đang DRAFT
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể cập nhật Assessment Policy khi đang ở trạng thái DRAFT.");
        }

        // 5. Kiểm tra Band mục tiêu / Band tối thiểu (frameworkVersionId giữ nguyên, không cho đổi)
        FrameworkResultBand targetBand = frameworkResultBandRepository.findById(command.targetFrameworkBandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Band mục tiêu (targetFrameworkBandId)."));
        FrameworkResultBand minimumBand = frameworkResultBandRepository.findById(command.minimumFrameworkBandId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Band tối thiểu (minimumFrameworkBandId)."));
        if (!targetBand.getFrameworkVersionId().equals(policy.getFrameworkVersionId())
                || !minimumBand.getFrameworkVersionId().equals(policy.getFrameworkVersionId())) {
            throw new IllegalStateException("Band mục tiêu/tối thiểu phải thuộc đúng Phiên bản Khung tiêu chuẩn của Policy này.");
        }
        if (minimumBand.getOrder() > targetBand.getOrder()) {
            throw new IllegalStateException("Band tối thiểu không được xếp hạng cao hơn Band mục tiêu.");
        }

        // 6. Kiểm tra khoảng thời gian hiệu lực
        if (command.effectiveFrom() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu hiệu lực (effectiveFrom) không được để trống.");
        }
        if (command.effectiveTo() != null && command.effectiveFrom().isAfter(command.effectiveTo())) {
            throw new IllegalArgumentException("Ngày bắt đầu hiệu lực không được sau ngày kết thúc hiệu lực.");
        }

        // 7. Cập nhật các field nghiệp vụ (không đổi frameworkVersionId/rubricVersionId/languageId/scope)
        OffsetDateTime now = OffsetDateTime.now();
        AssessmentPolicyStrictness strictness = command.strictness() != null ? command.strictness() : AssessmentPolicyStrictness.STANDARD;

        policy.setTargetFrameworkBandId(command.targetFrameworkBandId());
        policy.setMinimumFrameworkBandId(command.minimumFrameworkBandId());
        policy.setPassingScore(command.passingScore());
        policy.setStrictness(strictness);
        policy.setEffectiveFrom(command.effectiveFrom());
        policy.setEffectiveTo(command.effectiveTo());
        policy.setUpdatedAt(now);
        policy.setUpdatedBy(currentUserId);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);
        return saved.getId();
    }
}
