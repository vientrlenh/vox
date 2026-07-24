package com.sep.vox.application.port.input.usecase.examsession;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.examsession.StudentExamResultSummaryResponse;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;

@Service
public class ViewMyExamResultsUseCase implements IUseCase<Void, List<StudentExamResultSummaryResponse>> {

    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamResultsUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            RubricResultBandRepository rubricResultBandRepository,
            UserContextPort userContextPort) {
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentExamResultSummaryResponse> execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = examCandidateRepository.findByStudentId(studentId);
        var examsById = new HashMap<>(examRepository.findByIdIn(candidates.stream()
            .map(candidate -> candidate.getExamId())
            .distinct()
            .toList()).stream().collect(java.util.stream.Collectors.toMap(exam -> exam.getId(), exam -> exam)));

        return candidates.stream()
            .map(candidate -> {
                var exam = examsById.get(candidate.getExamId());
                if (exam == null) {
                    return null;
                }

                var session = examSessionRepository.findLatestByCandidateId(candidate.getId()).orElse(null);
                if (session == null) {
                    return null;
                }

                var result = examCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
                if (result == null) {
                    return null;
                }

                var rubricBand = result.getRubricResultBandId() == null
                    ? null
                    : rubricResultBandRepository.findById(result.getRubricResultBandId()).orElse(null);

                return new StudentExamResultSummaryResponse(
                    candidate.getId(),
                    exam.getId(),
                    exam.getCode(),
                    exam.getName(),
                    session.getId(),
                    session.getPaperId(),
                    session.getStatus().name(),
                    session.isFlagged(),
                    session.getStartedAt() == null ? null : session.getStartedAt().toString(),
                    session.getSubmittedAt() == null ? null : session.getSubmittedAt().toString(),
                    isScoreVisible(session, result.getStatus()) ? result.getTotalScore() : null,
                    result.getStatus().name(),
                    isScoreVisible(session, result.getStatus()) ? result.getRubricResultBandId() : null,
                    isScoreVisible(session, result.getStatus()) && rubricBand != null ? rubricBand.getCode() : null,
                    isScoreVisible(session, result.getStatus()) && rubricBand != null ? rubricBand.getName() : null
                );
            })
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(
                StudentExamResultSummaryResponse::submittedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();
    }

    private boolean isScoreVisible(com.sep.vox.domain.model.exam.ExamSession session, ExamCandidateResultStatus status) {
        if (!session.isFlagged()) {
            return true;
        }
        return status == ExamCandidateResultStatus.FINAL || status == ExamCandidateResultStatus.RELEASED;
    }
}
