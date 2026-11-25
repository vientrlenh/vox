package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessmentsession.StudentAssessmentResultSummaryResponse;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;

@Service
@RequiredArgsConstructor
public class ViewMyAssessmentResultsUseCase implements IUseCase<Void, List<StudentAssessmentResultSummaryResponse>> {

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final com.sep.vox.domain.repository.RubricVersionRepository rubricVersionRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<StudentAssessmentResultSummaryResponse> execute(Void input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByStudentId(studentId);
        // Cached by rubricVersionId within this very call. A student usually takes several attempts
        // of the same assessment, i.e. the same rubric version -- looking it up per row asks the DB the exact same
        // question many times. (This loop already had an N+1 on session/result/band; it is not widened
        // here, just not made worse.)
        Map<UUID, RubricVersion> rubricVersionCache = new HashMap<UUID, RubricVersion>();
        Map<UUID, Assessment> assessmentsById = new HashMap<>(assessmentRepository.findByIdIn(candidates.stream()
            .map(candidate -> candidate.getAssessmentId())
            .distinct()
            .toList()).stream().collect(Collectors.toMap(assessment -> assessment.getId(), assessment -> assessment)));

        return candidates.stream()
            .map(candidate -> toSummary(candidate, assessmentsById.get(candidate.getAssessmentId()), rubricVersionCache))
            .flatMap(Optional::stream)
            .sorted(Comparator.comparing(
                (StudentAssessmentResultSummaryResponse response) -> response.submittedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
            ))
            .toList();
    }

    private Optional<StudentAssessmentResultSummaryResponse> toSummary(
        AssessmentCandidate candidate, Assessment assessment, Map<UUID, RubricVersion> rubricVersionCache) {
        if (assessment == null) {
            return Optional.empty();
        }

        AssessmentSession session = assessmentSessionRepository.findLatestByCandidateId(candidate.getId()).orElse(null);
        if (session == null) {
            return Optional.empty();
        }

        AssessmentCandidateResult result = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        if (result == null) {
            return Optional.empty();
        }

        RubricResultBand rubricBand = result.getRubricResultBandId() == null
            ? null
            : rubricResultBandRepository.findById(result.getRubricResultBandId()).orElse(null);
        // This query is guarded by hasRole('STUDENT') and only scans the caller's own
        // candidates, so every row here is theirs -- no ownership check is needed.
        boolean scoreVisible = AssessmentCandidateResultStatus.isVisibleToCandidate(result.getStatus());
        RubricVersion rubricVersion = findVisibleRubricVersion(result, scoreVisible, rubricVersionCache);
        return Optional.of(buildSummary(candidate, assessment, session, result, scoreVisible, rubricBand, rubricVersion));
    }

    private RubricVersion findVisibleRubricVersion(
        AssessmentCandidateResult result, boolean scoreVisible, Map<UUID, RubricVersion> rubricVersionCache) {
        return !scoreVisible || result.getRubricVersionId() == null
            ? null
            : rubricVersionCache.computeIfAbsent(
                result.getRubricVersionId(),
                id -> rubricVersionRepository.findById(id).orElse(null));
    }

    private StudentAssessmentResultSummaryResponse buildSummary(
        AssessmentCandidate candidate, Assessment assessment, AssessmentSession session,
        AssessmentCandidateResult result, boolean scoreVisible, RubricResultBand rubricBand, RubricVersion rubricVersion) {
        return new StudentAssessmentResultSummaryResponse(
            candidate.getId(),
            assessment.getId(),
            assessment.getCode(),
            assessment.getName(),
            assessment.getKind().name(),
            session.getId(),
            session.getPaperId(),
            session.getStatus(),
            session.isFlagged(),
            session.getStartedAt(),
            session.getSubmittedAt(),
            scoreVisible ? result.getTotalScore() : null,
            rubricVersion == null ? null : rubricVersion.getScoringScaleMin(),
            rubricVersion == null ? null : rubricVersion.getScoringScaleMax(),
            result.getStatus(),
            scoreVisible ? result.getRubricResultBandId() : null,
            scoreVisible && rubricBand != null ? rubricBand.getCode() : null,
            scoreVisible && rubricBand != null ? rubricBand.getName() : null
        );
    }

}
