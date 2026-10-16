package com.sep.vox.application.port.input.usecase.assessmentitemanswer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAssessmentItemAnswerTurnsQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemAnswerTurnResponse;
import com.sep.vox.domain.repository.assessment.AssessmentItemResponseTurnRepository;

@Service
public class ViewAssessmentItemAnswerTurnsUseCase implements IUseCase<ViewAssessmentItemAnswerTurnsQuery, List<AssessmentItemAnswerTurnResponse>> {

    private final AssessmentResultAccessService assessmentResultAccessService;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    public ViewAssessmentItemAnswerTurnsUseCase(
            AssessmentResultAccessService assessmentResultAccessService,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository) {
        this.assessmentResultAccessService = assessmentResultAccessService;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentItemAnswerTurnResponse> execute(ViewAssessmentItemAnswerTurnsQuery input) {
        assessmentResultAccessService.requireCandidateVisibleResponse(input.answerId());
        return assessmentItemResponseTurnRepository.findByAssessmentItemAnswerId(input.answerId()).stream()
            .map(AssessmentItemAnswerTurnResponse::toResponse)
            .toList();
    }
}
