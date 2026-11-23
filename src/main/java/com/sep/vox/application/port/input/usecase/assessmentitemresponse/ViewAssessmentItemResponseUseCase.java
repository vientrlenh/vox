package com.sep.vox.application.port.input.usecase.assessmentitemresponse;

import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentItemResponseTurn;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.assessmentitemresponse.AssessmentItemResponseResponseMapper;
import com.sep.vox.application.port.input.query.ViewAssessmentItemResponseQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseDetailsResponse;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentItemResponseUseCase implements IUseCase<ViewAssessmentItemResponseQuery, AssessmentItemResponseDetailsResponse> {

    private final AssessmentResultAccessService assessmentResultAccessService;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    @Override
    @Transactional(readOnly = true)
    public AssessmentItemResponseDetailsResponse execute(ViewAssessmentItemResponseQuery input) {
        AssessmentItemResponse response = assessmentResultAccessService.requireCandidateVisibleResponse(input.answerId()).response();
        List<AssessmentItemResponseTurn> turns = assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(input.answerId());
        return AssessmentItemResponseResponseMapper.toDetailsResponse(response, turns);
    }
}
