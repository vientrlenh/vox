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
        requireActiveSchoolAdmin(command.schoolId());
        AssessmentPolicy policy = loadPolicyOfSchool(command);

        if (policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only an Assessment Policy in the PUBLISHED status can be archived (ARCHIVE).");
        }

        Instant now = Instant.now();
        policy.setStatus(AssessmentPolicyStatus.ARCHIVED);
        // Do not let effectiveTo move back before effectiveFrom when the policy's effective date has not been reached yet
        policy.setEffectiveTo(now.isBefore(policy.getEffectiveFrom()) ? policy.getEffectiveFrom() : now);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);
        archiveRubricVersionIfUnused(policy, now);
        return saved.getId();
    }

    private void requireActiveSchoolAdmin(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not assigned to any school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: You are not allowed to modify the Assessment Policy of another school.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school has been deactivated in the system.");
        }
    }

    private AssessmentPolicy loadPolicyOfSchool(ArchiveSchoolAssessmentPolicyCommand command) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("SECURITY: You are not allowed to modify the Assessment Policy of another school.");
        }
        return policy;
    }

    /**
     * Archiving a Policy also archives the linked Rubric Version -- BUT only when no other Policy is still using it.
     *
     * <p>The old comment here ("1 Rubric Version is tied to exactly 1 Assessment Policy, permanently")
     * described the 1-1 model of V38, which V44 removed. Since several Policies can share one version,
     * archiving unconditionally would pull the scoring scale out from under the Policies that are still
     * in effect: when a school withdraws the regular-class policy, the specialised-class policy is often
     * the one that points at an ARCHIVED version.
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
