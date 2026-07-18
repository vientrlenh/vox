package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamSessionResultQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultItemResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultSectionResponse;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
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
    private final UserContextPort userContextPort;
    private final ExamCandidateRepository examCandidateRepository;

    public ViewExamSessionResultUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionResultCalculator examSessionResultCalculator,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricResultBandRepository rubricResultBandRepository,
            ExamResultAccessService examResultAccessService,
            UserContextPort userContextPort,
            ExamCandidateRepository examCandidateRepository) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionResultCalculator = examSessionResultCalculator;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.examResultAccessService = examResultAccessService;
        this.userContextPort = userContextPort;
        this.examCandidateRepository = examCandidateRepository;
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
        var scoreVisible = isScoreVisibleToCurrentUser(session, result.getStatus());

        return new ExamCandidateResultResponse(
            result.getId(),
            result.getSessionId(),
            result.getExamId(),
            calculated.paperId(),
            result.getCandidateId(),
            session.isFlagged(),
            session.getFlagReason(),
            scoreVisible,
            scoreVisible ? result.getTotalScore() : null,
            scoreVisible ? result.getTargetFrameworkBandId() : null,
            scoreVisible && targetBand != null ? targetBand.getCode() : null,
            scoreVisible && targetBand != null ? targetBand.getLabel() : null,
            scoreVisible ? result.getRubricResultBandId() : null,
            scoreVisible && rubricBand != null ? rubricBand.getCode() : null,
            scoreVisible && rubricBand != null ? rubricBand.getName() : null,
            result.getStatus().name(),
            scoreVisible ? calculated.sections().stream()
                .map(section -> new ExamCandidateResultSectionResponse(section.sectionId(), section.title(), section.score()))
                .toList() : java.util.List.of(),
            scoreVisible ? calculated.items().stream()
                .map(item -> new ExamCandidateResultItemResponse(
                    item.paperItemId(),
                    item.responseId(),
                    item.sectionId(),
                    item.itemScore(),
                    item.weightedScore()
                ))
                .toList() : java.util.List.of()
        );
    }

    private boolean isScoreVisibleToCurrentUser(com.sep.vox.domain.model.exam.ExamSession session, ExamCandidateResultStatus status) {
        if (!session.isFlagged()) {
            return true;
        }
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var isStudentOwner = examCandidateRepository.findById(session.getCandidateId())
            .map(candidate -> candidate.getStudentId().equals(currentUserId))
            .orElse(false);
        if (!isStudentOwner) {
            return true;
        }
        return status == ExamCandidateResultStatus.FINAL || status == ExamCandidateResultStatus.RELEASED;
    }
}
