package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.PublishSystemAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishSystemAssessmentPolicyUseCase implements IUseCase<PublishSystemAssessmentPolicyCommand, UUID> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(PublishSystemAssessmentPolicyCommand command) {
        requireActiveUser();
        AssessmentPolicy policy = loadSystemPolicy(command);

        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("Only an Assessment Policy in the DRAFT status can be published (PUBLISH).");
        }

        requireLinkedVersionsUsable(policy);

        policy.setStatus(AssessmentPolicyStatus.PUBLISHED);

        AssessmentPolicy saved = assessmentPolicyRepository.save(policy);
        return saved.getId();
    }

    private void requireActiveUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }
    }

    private AssessmentPolicy loadSystemPolicy(PublishSystemAssessmentPolicyCommand command) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("Action denied: a school's Assessment Policy cannot be modified here.");
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
