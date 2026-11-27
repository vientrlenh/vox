package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.PublishSchoolAssessmentPoliciesByRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublishSchoolAssessmentPoliciesByRubricVersionUseCase
        implements IUseCase<PublishSchoolAssessmentPoliciesByRubricVersionCommand, List<UUID>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public List<UUID> execute(PublishSchoolAssessmentPoliciesByRubricVersionCommand command) {
        requireActiveSchoolAdmin(command.schoolId());
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

    private List<AssessmentPolicy> loadDraftPolicies(PublishSchoolAssessmentPoliciesByRubricVersionCommand command) {
        RubricVersion rubricVersion = rubricVersionRepository.findById(command.rubricVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric version not found."));
        if (rubricVersion.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Assessment Policies can only be published in bulk while the linked Rubric Version is DRAFT.");
        }

        List<AssessmentPolicy> draftPolicies = assessmentPolicyRepository
                .findDraftBySchoolIdAndRubricVersionId(command.schoolId(), command.rubricVersionId());
        if (draftPolicies.isEmpty()) {
            throw new ResourceNotFoundException("No DRAFT Assessment Policy of the school is linked to this Rubric Version.");
        }
        return draftPolicies;
    }
}
