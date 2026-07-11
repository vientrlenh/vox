package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamSessionResultQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultItemResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultSectionResponse;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;

@Service
public class ViewExamSessionResultUseCase implements IUseCase<ViewExamSessionResultQuery, ExamCandidateResultResponse> {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamSessionResultCalculator examSessionResultCalculator;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final ExamResultAccessService examResultAccessService;

    public ViewExamSessionResultUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionResultCalculator examSessionResultCalculator,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricResultBandRepository rubricResultBandRepository,
            ExamResultAccessService examResultAccessService) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionResultCalculator = examSessionResultCalculator;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.examResultAccessService = examResultAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamCandidateResultResponse execute(ViewExamSessionResultQuery input) {
        var session = examResultAccessService.getAuthorizedSession(input.sessionId());
        var result = examCandidateResultRepository.findBySessionId(session.getId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay ket qua phien thi"));
        var calculated = examSessionResultCalculator.calculate(session.getId());
        var targetBand = result.getTargetFrameworkBandId() == null
            ? null
            : frameworkResultBandRepository.findById(result.getTargetFrameworkBandId()).orElse(null);
        var rubricBand = result.getRubricResultBandId() == null
            ? null
            : rubricResultBandRepository.findById(result.getRubricResultBandId()).orElse(null);

        return new ExamCandidateResultResponse(
            result.getId(),
            result.getSessionId(),
            result.getExamId(),
            calculated.paperId(),
            result.getCandidateId(),
            result.getTotalScore(),
            result.getTargetFrameworkBandId(),
            targetBand == null ? null : targetBand.getCode(),
            targetBand == null ? null : targetBand.getLabel(),
            result.getRubricResultBandId(),
            rubricBand == null ? null : rubricBand.getCode(),
            rubricBand == null ? null : rubricBand.getName(),
            result.getStatus().name(),
            calculated.sections().stream()
                .map(section -> new ExamCandidateResultSectionResponse(section.sectionId(), section.title(), section.score()))
                .toList(),
            calculated.items().stream()
                .map(item -> new ExamCandidateResultItemResponse(
                    item.paperItemId(),
                    item.responseId(),
                    item.sectionId(),
                    item.itemScore(),
                    item.weightedScore()
                ))
                .toList()
        );
    }
}
