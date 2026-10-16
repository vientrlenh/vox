package com.sep.vox.application.port.input.usecase.assessmentitemanswer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.assessmentitemanswer.AssessmentItemAnswerResponseMapper;
import com.sep.vox.application.port.input.query.ViewAssessmentItemAnswerQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemAnswerDetailsResponse;
import com.sep.vox.domain.repository.assessment.AssessmentItemResponseTurnRepository;

@Service
public class ViewAssessmentItemAnswerUseCase implements IUseCase<ViewAssessmentItemAnswerQuery, AssessmentItemAnswerDetailsResponse> {

    private final AssessmentResultAccessService assessmentResultAccessService;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    public ViewAssessmentItemAnswerUseCase(
            AssessmentResultAccessService assessmentResultAccessService,
            AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository) {
        this.assessmentResultAccessService = assessmentResultAccessService;
        this.assessmentItemResponseTurnRepository = assessmentItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentItemAnswerDetailsResponse execute(ViewAssessmentItemAnswerQuery input) {
        var response = assessmentResultAccessService.requireCandidateVisibleResponse(input.answerId()).response();
        var turns = assessmentItemResponseTurnRepository.findByAssessmentItemAnswerId(input.answerId());
        return AssessmentItemAnswerResponseMapper.toDetailsResponse(response, turns);
    }
}
