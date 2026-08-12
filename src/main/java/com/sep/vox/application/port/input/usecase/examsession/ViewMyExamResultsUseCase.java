package com.sep.vox.application.port.input.usecase.examsession;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
    private final com.sep.vox.domain.repository.RubricVersionRepository rubricVersionRepository;
    private final UserContextPort userContextPort;

    public ViewMyExamResultsUseCase(
            ExamCandidateRepository examCandidateRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            RubricResultBandRepository rubricResultBandRepository,
            com.sep.vox.domain.repository.RubricVersionRepository rubricVersionRepository,
            UserContextPort userContextPort) {
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentExamResultSummaryResponse> execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = examCandidateRepository.findByStudentId(studentId);
        // Nhớ theo rubricVersionId trong đúng lượt gọi này. Một học sinh thường thi nhiều lượt
        // trên cùng một kỳ, tức cùng một rubric version -- tra lại từng dòng là hỏi DB đúng một
        // câu giống hệt nhau nhiều lần. (Vòng lặp này vốn đã N+1 sẵn với session/result/band;
        // không mở rộng thêm chỗ đó ở đây, chỉ không làm nó tệ hơn.)
        var rubricVersionCache = new HashMap<java.util.UUID, com.sep.vox.domain.model.rubric.RubricVersion>();
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
                // Query này gác hasRole('STUDENT') và chỉ quét candidate của chính người
                // gọi, nên mọi dòng ở đây đều là bài của họ — không cần kiểm chính chủ.
                var scoreVisible = ExamCandidateResultStatus.isVisibleToCandidate(result.getStatus());
                var rubricVersion = !scoreVisible || result.getRubricVersionId() == null
                    ? null
                    : rubricVersionCache.computeIfAbsent(
                        result.getRubricVersionId(),
                        id -> rubricVersionRepository.findById(id).orElse(null));

                return new StudentExamResultSummaryResponse(
                    candidate.getId(),
                    exam.getId(),
                    exam.getCode(),
                    exam.getName(),
                    exam.getKind().name(),
                    session.getId(),
                    session.getPaperId(),
                    session.getStatus().name(),
                    session.isFlagged(),
                    session.getStartedAt() == null ? null : session.getStartedAt().toString(),
                    session.getSubmittedAt() == null ? null : session.getSubmittedAt().toString(),
                    scoreVisible ? result.getTotalScore() : null,
                    rubricVersion == null ? null : rubricVersion.getScoringScaleMin(),
                    rubricVersion == null ? null : rubricVersion.getScoringScaleMax(),
                    result.getStatus().name(),
                    scoreVisible ? result.getRubricResultBandId() : null,
                    scoreVisible && rubricBand != null ? rubricBand.getCode() : null,
                    scoreVisible && rubricBand != null ? rubricBand.getName() : null
                );
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(
                (StudentExamResultSummaryResponse response) -> response.submittedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();
    }

}
