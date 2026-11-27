package com.sep.vox.application.port.input.usecase.assessmentpolicyschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.assessmentpolicyschool.ViewSchoolAssessmentPolicyDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPolicyDto;
import com.sep.vox.domain.mapper.AssessmentPolicyDtoMapper;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewSchoolAssessmentPolicyDetailsUseCase implements IUseCase<ViewSchoolAssessmentPolicyDetailsQuery, AssessmentPolicyDto> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional(readOnly = true)
    public AssessmentPolicyDto execute(ViewSchoolAssessmentPolicyDetailsQuery query) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        requireActiveUser(currentUserId);

        AssessmentPolicy policy = assessmentPolicyRepository.findById(query.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("This Assessment Policy was not found."));

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not linked to any school."));

        if (policy.getSchoolId() == null ||
                !policy.getSchoolId().equals(query.schoolId()) ||
                !schoolUser.getSchoolId().equals(policy.getSchoolId())) {
            throw new ForbiddenException("SECURITY: You are not allowed to view the Assessment Policy of another school.");
        }

        requireSchoolActive(query.schoolId());

        return AssessmentPolicyDtoMapper.toDto(policy);
    }

    private void requireActiveUser(UUID currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account is locked.");
        }
    }

    private void requireSchoolActive(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found."));
        if (!school.isActive()) {
            throw new ForbiddenException("Action denied: this school has been deactivated.");
        }
    }
}
