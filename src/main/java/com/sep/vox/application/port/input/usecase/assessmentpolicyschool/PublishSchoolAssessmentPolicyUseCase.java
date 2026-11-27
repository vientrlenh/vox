package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.PublishSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishSchoolAssessmentPolicyUseCase implements IUseCase<PublishSchoolAssessmentPolicyCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(PublishSchoolAssessmentPolicyCommand command) {
        requireActiveSchoolAdmin(command.schoolId());
        AssessmentPolicy policy = loadPolicyOfSchool(command);

        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("Only an Assessment Policy in the DRAFT status can be published (PUBLISH).");
        }

        requireLinkedVersionsUsable(policy);

        policy.setStatus(AssessmentPolicyStatus.PUBLISHED);

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

    private AssessmentPolicy loadPolicyOfSchool(PublishSchoolAssessmentPolicyCommand command) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("SECURITY: You are not allowed to modify the Assessment Policy of another school.");
        }
        return policy;
    }

    private void requireLinkedVersionsUsable(AssessmentPolicy policy) {
        FrameworkVersion frameworkVersion = frameworkVersionRepository.findById(policy.getFrameworkVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("The linked Framework Version was not found."));
        if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Cannot publish this Assessment Policy because the linked Framework Version is no longer PUBLISHED.");
        }

        // The linked Rubric Version is published AFTER this Assessment Policy is published, so only its existence is checked
        rubricVersionRepository.findById(policy.getRubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("The linked rubric version was not found."));
    }
}
