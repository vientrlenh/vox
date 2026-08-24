package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ArchiveSystemAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ArchiveSystemAssessmentPolicyUseCase implements IUseCase<ArchiveSystemAssessmentPolicyCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public ArchiveSystemAssessmentPolicyUseCase(
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricVersionRepository rubricVersionRepository,
            UserRepository userRepository,
            UserContextPort userContextPort) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(ArchiveSystemAssessmentPolicyCommand command) {
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
            throw new ForbiddenException("Hành động bị từ chối: Không thể can thiệp vào Assessment Policy của trường học.");
        }

        // 3. Chỉ được lưu trữ (ARCHIVE) khi đang PUBLISHED
        if (policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể lưu trữ (ARCHIVE) Assessment Policy đang ở trạng thái PUBLISHED.");
        }

        // 4. Lưu trạng thái mới
        Instant now = Instant.now();
        policy.setStatus(AssessmentPolicyStatus.ARCHIVED);
        // Không cho effectiveTo lùi về trước effectiveFrom nếu policy chưa tới ngày hiệu lực
        policy.setEffectiveTo(now.isBefore(policy.getEffectiveFrom()) ? policy.getEffectiveFrom() : now);
        policy.setUpdatedAt(now);
        policy.setUpdatedBy(currentUserId);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);

        // 5. Archive Policy thì Archive luôn Phiên bản Rubric liên kết -- NHƯNG chỉ khi không còn
        // Policy nào khác đang dùng nó. Xem lý do đầy đủ ở ArchiveSchoolAssessmentPolicyUseCase:
        // mô hình 1-1 mà chú thích cũ mô tả là của V38, V44 đã cho nhiều Policy dùng chung một
        // phiên bản.
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