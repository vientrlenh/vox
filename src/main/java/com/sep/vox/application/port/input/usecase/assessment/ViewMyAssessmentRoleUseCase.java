package com.sep.vox.application.port.input.usecase.assessment;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.assessment.ViewMyAssessmentRoleQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.AssessmentMemberRepository;

@Service
@RequiredArgsConstructor
public class ViewMyAssessmentRoleUseCase implements IUseCase<ViewMyAssessmentRoleQuery, String> {

    private final AssessmentMemberRepository assessmentMemberRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public String execute(ViewMyAssessmentRoleQuery input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        return assessmentMemberRepository.findByAssessmentIdAndUserId(input.assessmentId(), currentUserId)
            .map(member -> member.getRole().name())
            .orElse(null);
    }
}
