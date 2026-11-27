package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.DeleteSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSchoolAssessmentPolicyUseCase implements IUseCase<DeleteSchoolAssessmentPolicyCommand, Void> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(DeleteSchoolAssessmentPolicyCommand command) {
        requireActiveSchoolAdmin(command.schoolId());
        AssessmentPolicy policy = loadPolicyOfSchool(command);

        // Only a DRAFT may be hard-deleted; the other statuses use the separate Archive function
        if (policy.getStatus() != AssessmentPolicyStatus.DRAFT) {
            throw new IllegalArgumentException("Only an Assessment Policy in the DRAFT status can be deleted. Use Archive if the Policy is already PUBLISHED.");
        }
        assessmentPolicyRepository.deleteById(policy.getId());
        return null;
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

    private AssessmentPolicy loadPolicyOfSchool(DeleteSchoolAssessmentPolicyCommand command) {
        AssessmentPolicy policy = assessmentPolicyRepository.findById(command.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment Policy not found."));
        if (policy.getSchoolId() == null || !policy.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("SECURITY: You are not allowed to modify the Assessment Policy of another school.");
        }
        return policy;
    }
}
