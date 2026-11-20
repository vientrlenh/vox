package com.sep.vox.application.port.input.usecase.examitemresponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.examitemresponse.ExamItemResponseResponseMapper;
import com.sep.vox.application.port.input.query.ViewExamItemResponseQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseDetailsResponse;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;

@Service
public class ViewExamItemResponseUseCase implements IUseCase<ViewExamItemResponseQuery, ExamItemResponseDetailsResponse> {

    private final ExamResultAccessService examResultAccessService;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;

    public ViewExamItemResponseUseCase(
            ExamResultAccessService examResultAccessService,
            ExamItemResponseTurnRepository examItemResponseTurnRepository) {
        this.examResultAccessService = examResultAccessService;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamItemResponseDetailsResponse execute(ViewExamItemResponseQuery input) {
        var response = examResultAccessService.requireCandidateVisibleResponse(input.answerId()).response();
        var turns = examItemResponseTurnRepository.findByExamItemResponseId(input.answerId());
        return ExamItemResponseResponseMapper.toDetailsResponse(response, turns);
    }
}
