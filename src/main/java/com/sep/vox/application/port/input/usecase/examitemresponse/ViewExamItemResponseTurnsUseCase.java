package com.sep.vox.application.port.input.usecase.examitemresponse;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.mapper.examitemresponse.ExamItemResponseResponseMapper;
import com.sep.vox.application.port.input.query.ViewExamItemResponseTurnsQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examitemresponse.ExamItemResponseTurnResponse;
import com.sep.vox.domain.repository.ExamItemResponseTurnRepository;

@Service
public class ViewExamItemResponseTurnsUseCase implements IUseCase<ViewExamItemResponseTurnsQuery, List<ExamItemResponseTurnResponse>> {

    private final ExamResultAccessService examResultAccessService;
    private final ExamItemResponseTurnRepository examItemResponseTurnRepository;

    public ViewExamItemResponseTurnsUseCase(
            ExamResultAccessService examResultAccessService,
            ExamItemResponseTurnRepository examItemResponseTurnRepository) {
        this.examResultAccessService = examResultAccessService;
        this.examItemResponseTurnRepository = examItemResponseTurnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamItemResponseTurnResponse> execute(ViewExamItemResponseTurnsQuery input) {
        examResultAccessService.requireCandidateVisibleResponse(input.answerId());
        return examItemResponseTurnRepository.findByExamItemResponseId(input.answerId()).stream()
            .map(ExamItemResponseResponseMapper::toTurnResponse)
            .toList();
    }
}
