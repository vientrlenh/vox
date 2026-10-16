package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessmentsession.StudentAssessmentResultSummaryResponse;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class ViewMyAssessmentResultsUseCase implements IUseCase<Void, List<StudentAssessmentResultSummaryResponse>> {

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final com.sep.vox.domain.repository.RubricVersionRepository rubricVersionRepository;
    private final UserContextPort userContextPort;

    public ViewMyAssessmentResultsUseCase(
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentRepository assessmentRepository,
            RubricResultBandRepository rubricResultBandRepository,
            com.sep.vox.domain.repository.RubricVersionRepository rubricVersionRepository,
            UserContextPort userContextPort) {
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentRepository = assessmentRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAssessmentResultSummaryResponse> execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var candidates = assessmentCandidateRepository.findByStudentId(studentId);
        // Nhớ theo rubricVersionId trong đúng lượt gọi này. Một học sinh thường thi nhiều lượt
        // trên cùng một kỳ, tức cùng một rubric version -- tra lại từng dòng là hỏi DB đúng một
        // câu giống hệt nhau nhiều lần. (Vòng lặp này vốn đã N+1 sẵn với session/result/band;
        // không mở rộng thêm chỗ đó ở đây, chỉ không làm nó tệ hơn.)
        var rubricVersionCache = new HashMap<java.util.UUID, com.sep.vox.domain.model.rubric.RubricVersion>();
        var assessmentsById = new HashMap<>(assessmentRepository.findByIdIn(candidates.stream()
            .map(candidate -> candidate.getAssessmentId())
            .distinct()
            .toList()).stream().collect(java.util.stream.Collectors.toMap(assessment -> assessment.getId(), assessment -> assessment)));

        return candidates.stream()
            .map(candidate -> {
                var assessment = assessmentsById.get(candidate.getAssessmentId());
                if (assessment == null) {
                    return null;
                }

                var session = assessmentSessionRepository.findLatestByCandidateId(candidate.getId()).orElse(null);
                if (session == null) {
                    return null;
                }

                var result = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
                if (result == null) {
                    return null;
                }

                var rubricBand = result.getRubricResultBandId() == null
                    ? null
                    : rubricResultBandRepository.findById(result.getRubricResultBandId()).orElse(null);
                // Query này gác hasRole('STUDENT') và chỉ quét candidate của chính người
                // gọi, nên mọi dòng ở đây đều là bài của họ — không cần kiểm chính chủ.
                var scoreVisible = AssessmentCandidateResultStatus.isVisibleToCandidate(result.getStatus());
                var rubricVersion = !scoreVisible || result.getRubricVersionId() == null
                    ? null
                    : rubricVersionCache.computeIfAbsent(
                        result.getRubricVersionId(),
                        id -> rubricVersionRepository.findById(id).orElse(null));

                return new StudentAssessmentResultSummaryResponse(
                    candidate.getId(),
                    assessment.getId(),
                    assessment.getCode(),
                    assessment.getName(),
                    assessment.getKind().name(),
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
                (StudentAssessmentResultSummaryResponse response) -> response.submittedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();
    }

}
