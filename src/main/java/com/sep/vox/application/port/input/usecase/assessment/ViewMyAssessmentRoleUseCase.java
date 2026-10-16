package com.sep.vox.application.port.input.usecase.assessment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyAssessmentRoleQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;

@Service
public class ViewMyAssessmentRoleUseCase implements IUseCase<ViewMyAssessmentRoleQuery, String> {

    private final AssessmentMemberRepository assessmentMemberRepository;
    private final UserContextPort userContextPort;

    public ViewMyAssessmentRoleUseCase(AssessmentMemberRepository assessmentMemberRepository, UserContextPort userContextPort) {
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public String execute(ViewMyAssessmentRoleQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        return assessmentMemberRepository.findByAssessmentIdAndUserId(input.assessmentId(), currentUserId)
            .map(member -> member.getRole().name())
            .orElse(null);
    }
}
