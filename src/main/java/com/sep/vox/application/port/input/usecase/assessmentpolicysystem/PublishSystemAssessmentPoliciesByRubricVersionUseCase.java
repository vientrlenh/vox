package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.PublishSystemAssessmentPoliciesByRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishSystemAssessmentPoliciesByRubricVersionUseCase
        implements IUseCase<PublishSystemAssessmentPoliciesByRubricVersionCommand, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public List<UUID> execute(PublishSystemAssessmentPoliciesByRubricVersionCommand command) {
        requireActiveUser();
        List<AssessmentPolicy> draftPolicies = loadDraftPolicies(command);

        // Validate and publish each policy (roll everything back if one policy is invalid)
        for (AssessmentPolicy policy : draftPolicies) {
            FrameworkVersion frameworkVersion = frameworkVersionRepository.findById(policy.getFrameworkVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("The linked Framework Version was not found."));
            if (frameworkVersion.getStatus() != FrameworkVersionStatus.PUBLISHED) {
                throw new IllegalArgumentException("Cannot publish Assessment Policy (ID: " + policy.getId()
                        + ") because the linked Framework Version is no longer PUBLISHED.");
            }

            policy.setStatus(AssessmentPolicyStatus.PUBLISHED);
        }

        return assessmentPolicyRepository.saveAll(draftPolicies)
                .stream().map(ap -> ap.getId()).toList();
    }

    private void requireActiveUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Account not found."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account has been locked.");
        }
    }

    private List<AssessmentPolicy> loadDraftPolicies(PublishSystemAssessmentPoliciesByRubricVersionCommand command) {
        RubricVersion rubricVersion = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric version not found."));
        if (rubricVersion.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Assessment Policies can only be published in bulk while the linked Rubric Version is DRAFT.");
        }

        List<AssessmentPolicy> draftPolicies = assessmentPolicyRepository
                .findDraftSystemWideByRubricVersionId(command.rubricVersionId());
        if (draftPolicies.isEmpty()) {
            throw new ResourceNotFoundException("No DRAFT system Assessment Policy is linked to this Rubric Version.");
        }
        return draftPolicies;
    }
}
