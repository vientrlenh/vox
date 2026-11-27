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
        requireActiveSchoolAdmin(command.schoolId());
        AssessmentPolicy policy = loadPolicyOfSchool(command);

        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("An Assessment Policy can only be updated while it is DRAFT.");
        }

        requireTargetBandWithinScope(command, policy);
        Instant effectiveTo = effectiveToAfterValidation(command, policy);
        applyChanges(policy, command, effectiveTo);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);
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

    private AssessmentPolicy loadPolicyOfSchool(UpdateAssessmentPolicyCommand command) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("SECURITY: You are not allowed to modify the Assessment Policy of another school.");
        }
        return policy;
    }

    private void requireTargetBandWithinScope(UpdateAssessmentPolicyCommand command, AssessmentPolicy policy) {
        // frameworkVersionId stays the same and cannot be changed
        FrameworkResultBand targetBand = frameworkResultBandRepository.findById(command.targetFrameworkBandId())
                .orElseThrow(() -> new ResourceNotFoundException("Target band (targetFrameworkBandId) not found."));
        if (!targetBand.getFrameworkVersionId().equals(policy.getFrameworkVersionId())) {
            throw new IllegalArgumentException("The target band must belong to the Framework Version of this Policy.");
        }

        // Band ceiling per grade level. The scope cannot be edited here, so it is taken straight from the existing policy --
        // but the Band CAN be edited, so without this check a policy created within the ceiling would still be
        // pushed past the ceiling right afterwards.
        gradeLevelBandScopeGuardService.assertWithinScope(
                policy.getGradeLevelId(), policy.getSchoolGradeId(), policy.getSchoolClassId(),
                policy.getFrameworkVersionId(), targetBand);
    }

    /** Validates the effective period against the end date that will be stored (omitted keeps the stored one). */
    private Instant effectiveToAfterValidation(UpdateAssessmentPolicyCommand command, AssessmentPolicy policy) {
        if (command.effectiveFrom() == null) {
            throw new IllegalArgumentException("The effective start date (effectiveFrom) must not be empty.");
        }
        Instant effectiveTo = command.effectiveToProvided() ? command.effectiveTo() : policy.getEffectiveTo();
        if (effectiveTo != null && command.effectiveFrom().isAfter(effectiveTo)) {
            throw new IllegalArgumentException("The effective start date must not be after the effective end date.");
        }
        return effectiveTo;
    }

    /**
     * Updates the business fields (frameworkVersionId/rubricVersionId/languageId/scope do not change).
     * An omitted passingScore/strictness keeps the stored value; an explicit null clears passingScore and stores STANDARD.
     */
    private void applyChanges(AssessmentPolicy policy, UpdateAssessmentPolicyCommand command, Instant effectiveTo) {
        policy.setTargetFrameworkBandId(command.targetFrameworkBandId());
        if (command.passingScoreProvided()) {
            policy.setPassingScore(command.passingScore());
        }
        if (command.strictnessProvided()) {
            policy.setStrictness(command.strictness() != null ? command.strictness() : AssessmentPolicyStrictness.STANDARD);
        }
        policy.setEffectiveFrom(command.effectiveFrom());
        policy.setEffectiveTo(effectiveTo);
    }
}
