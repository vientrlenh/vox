package com.sep.vox.application.port.input.usecase.assessmentitemresponse;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.assessmentitemresponse.AssessmentItemResponseResponseMapper;
import com.sep.vox.application.port.input.query.ViewAssessmentItemResponseTurnsQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentitemresponse.AssessmentItemResponseTurnResponse;
import com.sep.vox.domain.repository.AssessmentItemResponseTurnRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentItemResponseTurnsUseCase implements IUseCase<ViewAssessmentItemResponseTurnsQuery, List<AssessmentItemResponseTurnResponse>> {

    private final AssessmentResultAccessService assessmentResultAccessService;
    private final AssessmentItemResponseTurnRepository assessmentItemResponseTurnRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentItemResponseTurnResponse> execute(ViewAssessmentItemResponseTurnsQuery input) {
        assessmentResultAccessService.requireCandidateVisibleResponse(input.answerId());
        return assessmentItemResponseTurnRepository.findByAssessmentItemResponseId(input.answerId()).stream()
            .map(AssessmentItemResponseResponseMapper::toTurnResponse)
            .toList();
    }
}
