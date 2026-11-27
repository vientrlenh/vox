package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.ArchiveSystemAssessmentPolicyCommand;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchiveSystemAssessmentPolicyUseCase implements IUseCase<ArchiveSystemAssessmentPolicyCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(ArchiveSystemAssessmentPolicyCommand command) {
        requireActiveUser();
        AssessmentPolicy policy = loadSystemPolicy(command);

        if (policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ có thể lưu trữ (ARCHIVE) Assessment Policy đang ở trạng thái PUBLISHED.");
        }

        Instant now = Instant.now();
        policy.setStatus(AssessmentPolicyStatus.ARCHIVED);
        // Do not let effectiveTo move back before effectiveFrom when the policy's effective date has not been reached yet
        policy.setEffectiveTo(now.isBefore(policy.getEffectiveFrom()) ? policy.getEffectiveFrom() : now);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);
        archiveRubricVersionIfUnused(policy, now);
        return saved.getId();
    }

    private void requireActiveUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }
    }

    private AssessmentPolicy loadSystemPolicy(ArchiveSystemAssessmentPolicyCommand command) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Assessment Policy."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể can thiệp vào Assessment Policy của trường học.");
        }
        return policy;
    }

    /**
     * Archiving a Policy also archives the linked Rubric Version -- BUT only when no other Policy is still using it.
     * See ArchiveSchoolAssessmentPolicyUseCase for the full reason: the 1-1 model that the old comment described
     * belonged to V38, and V44 let several Policies share one version.
     */
    private void archiveRubricVersionIfUnused(AssessmentPolicy policy, Instant now) {
        if (assessmentPolicyRepository.existsOtherActiveByRubricVersionId(policy.getRubricVersionId(), policy.getId())) {
            return;
        }
        rubricVersionRepository.findById(policy.getRubricVersionId()).ifPresent(rubricVersion -> {
            if (rubricVersion.getStatus() == RubricStatus.PUBLISHED) {
                rubricVersion.setStatus(RubricStatus.ARCHIVED);
                rubricVersion.setEffectiveTo(
                        now.isBefore(rubricVersion.getEffectiveFrom()) ? rubricVersion.getEffectiveFrom() : now);
                rubricVersionRepository.save(rubricVersion);
            }
        });
    }
}
