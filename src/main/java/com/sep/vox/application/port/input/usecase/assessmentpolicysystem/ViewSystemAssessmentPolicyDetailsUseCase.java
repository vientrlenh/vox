package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.assessmentpolicysystem.ViewSystemAssessmentPolicyDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPolicyDto;
import com.sep.vox.domain.mapper.AssessmentPolicyDtoMapper;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewSystemAssessmentPolicyDetailsUseCase implements IUseCase<ViewSystemAssessmentPolicyDetailsQuery, AssessmentPolicyDto> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public AssessmentPolicyDto execute(ViewSystemAssessmentPolicyDetailsQuery query) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account is locked.");
        }

        AssessmentPolicy policy = assessmentPolicyRepository.findById(query.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("This Assessment Policy was not found."));

        if (policy.getSchoolId() != null) {
            throw new ForbiddenException("This Assessment Policy does not belong to the System.");
        }

        // The same line as ViewSystemAssessmentPoliciesUseCase: a school can only see published template
        // policies. Without this guard, hiding drafts from the list is pointless -- a guessed id would show them.
        if (!userContextPort.isSystemAdmin() && policy.getStatus() != AssessmentPolicyStatus.PUBLISHED) {
            throw new ForbiddenException("Only a published (PUBLISHED) template policy can be viewed.");
        }

        return AssessmentPolicyDtoMapper.toDto(policy);
    }
}
