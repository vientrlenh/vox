package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DeleteSystemAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeleteSystemAssessmentPolicyUseCase implements IUseCase<DeleteSystemAssessmentPolicyCommand, Void> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public DeleteSystemAssessmentPolicyUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteSystemAssessmentPolicyCommand command) {
        // 1. Kiểm tra tài khoản System Admin
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        // 2. Kiểm tra Assessment Policy tồn tại và thuộc phạm vi toàn hệ thống
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Không thể can thiệp vào Assessment Policy của trường học.");
        }

        // 3. Xử lý theo trạng thái (DRAFT -> xóa cứng, PUBLISHED -> lưu trữ)
        if (policy.getStatus() == AssessmentPolicyStatus.DRAFT) {
            assessmentPolicyRepository.deleteById(policy.getId());
        } else if (policy.getStatus() == AssessmentPolicyStatus.PUBLISHED) {
            OffsetDateTime now = OffsetDateTime.now();
            policy.setStatus(AssessmentPolicyStatus.ARCHIVED);
            // Không cho effectiveTo lùi về trước effectiveFrom nếu policy chưa tới ngày hiệu lực
            policy.setEffectiveTo(now.isBefore(policy.getEffectiveFrom()) ? policy.getEffectiveFrom() : now);
            policy.setUpdatedAt(now);
            policy.setUpdatedBy(currentUserId);
            assessmentPolicyRepository.save(policy);
        } else {
            throw new IllegalStateException("Assessment Policy đã được lưu trữ (ARCHIVED) từ trước.");
        }
        return null;
    }
}
