package com.sep.vox.application.port.input.usecase.assessmentitemanswer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAssessmentSessionFollowupsQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentSessionFollowupResponse;
import com.sep.vox.domain.repository.assessment.AssessmentItemResponseTurnRepository;

@Service
public class ViewAssessmentSessionFollowupsUseCase
        implements IUseCase<ViewAssessmentSessionFollowupsQuery, List<AssessmentSessionFollowupResponse>> {

    private final AssessmentResultAccessService assessmentResultAccessService;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    public ViewAssessmentSessionFollowupsUseCase(
            AssessmentResultAccessService assessmentResultAccessService,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository) {
        this.assessmentResultAccessService = assessmentResultAccessService;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentSessionFollowupResponse> execute(ViewAssessmentSessionFollowupsQuery input) {
        assessmentResultAccessService.requireCandidateVisibleSession(input.sessionId());
        return assessmentItemResponseTurnRepository.countFollowupsBySessionId(input.sessionId()).stream()
            .map(row -> new AssessmentSessionFollowupResponse(
                row.assessmentItemAnswerId(),
                row.followupCount(),
                row.totalTurns()
            ))
            .toList();
    }
}
