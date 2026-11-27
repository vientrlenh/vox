package com.sep.vox.application.port.input.usecase.assessmentpolicysystem;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.assessmentpolicysystem.ViewSystemAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
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
public class ViewSystemAssessmentPoliciesUseCase implements IUseCase<ViewSystemAssessmentPoliciesQuery, PageResult<AssessmentPolicyDto>> {

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentPolicyDto> execute(ViewSystemAssessmentPoliciesQuery query) {
        requireActiveUser();

        String safeStatus = query.status() == null ? null : query.status().name();

        if (query.effectiveFrom() != null && query.effectiveTo() != null && query.effectiveFrom().isAfter(query.effectiveTo())) {
            throw new IllegalArgumentException("The effectiveFrom/effectiveTo period is invalid.");
        }

        safeStatus = restrictStatusForNonSystemAdmin(safeStatus);

        PageResult<AssessmentPolicy> policyPage = assessmentPolicyRepository.findAllSystemWide(safeStatus, query.languageId(), query.rubricVersionId(),
                query.effectiveFrom(), query.effectiveTo(), query.page(), query.size());
        return AssessmentPolicyDtoMapper.toDtoPage(policyPage);
    }

    private void requireActiveUser() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("The account is locked.");
        }
    }

    // A School Admin views this list to pick a template to copy, so they may only see PUBLISHED
    // ones: a system draft is work in progress that has not been reviewed. Same boundary that
    // CloneSystemRubricToSchoolUseCase applies to template rubrics -- it blocks at copy time, and
    // here the draft is hidden at listing time so the school does not pick one that would be
    // rejected anyway.
    private String restrictStatusForNonSystemAdmin(String safeStatus) {
        if (userContextPort.isSystemAdmin()) {
            return safeStatus;
        }
        if (safeStatus != null && !AssessmentPolicyStatus.PUBLISHED.name().equals(safeStatus)) {
            throw new IllegalArgumentException(
                    "Only published (PUBLISHED) template policies can be viewed.");
        }
        return AssessmentPolicyStatus.PUBLISHED.name();
    }
}
