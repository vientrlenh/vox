package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZeroScoreAssessmentResultService {

    /**
     * Only a fallback when the rubric scale cannot be looked up -- it is NO LONGER "the score of an empty submission".
     *
     * <p>Changed 2026-08-11: the score of an empty submission is now the FLOOR OF THE rubric SCALE (see {@code floorScore}). Zero
     * is only coincidentally right for a scale that starts at 0; a school that declares a 4-10 scale has 0 below the floor and no
     * grade band contains it -- exactly the submissions that should be recorded again are the ones that lost their
     * grade.
     */
    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;

    @Transactional
    public AssessmentCandidateResult releaseZeroForEmptySession(UUID sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found"));
        return releaseZeroForSession(session, Instant.now());
    }

    @Transactional
    public void ensureZeroResultsForMissingOrEmptyAttempts(UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        AssessmentPolicy policy = resolvePolicy(assessment);
        Instant now = Instant.now();
        UUID fallbackPaperId = assessmentPaperRepository.findByAssessmentId(assessmentId).stream()
            .findFirst()
            .map(paper -> paper.getId())
            .orElse(null);

        for (AssessmentCandidate candidate : assessmentCandidateRepository.findByAssessmentId(assessmentId)) {
            if (AssessmentCandidateStatus.isNonScorable(candidate.getStatus())) {
                continue;
            }
            releaseZeroForCandidate(assessmentId, candidate, policy, fallbackPaperId, now);
        }
    }

    private void releaseZeroForCandidate(
            UUID assessmentId, AssessmentCandidate candidate, AssessmentPolicy policy, UUID fallbackPaperId,
            Instant now) {
        List<AssessmentSession> sessions = assessmentSessionRepository.findByCandidateId(candidate.getId()).stream()
            .filter(session -> assessmentId.equals(session.getAssessmentId()))
            .toList();
        if (sessions.isEmpty()) {
            UUID paperId = candidate.getAssignedPaperId() == null ? fallbackPaperId : candidate.getAssignedPaperId();
            releaseZeroForCandidateWithoutSession(assessmentId, candidate, paperId, policy, now);
            return;
        }

        for (AssessmentSession session : sessions) {
            releaseZeroForEmptySessionWithoutResult(session, policy, now);
        }
    }

    private void releaseZeroForCandidateWithoutSession(
            UUID assessmentId, AssessmentCandidate candidate, UUID paperId, AssessmentPolicy policy, Instant now) {
        if (paperId == null) {
            return;
        }
        AssessmentSession session = assessmentSessionRepository.save(
            AssessmentSession.gradedWithoutAnswers(assessmentId, candidate.getId(), paperId, now));
        releaseZeroForSession(session, policy, now);
    }

    private void releaseZeroForEmptySessionWithoutResult(AssessmentSession session, AssessmentPolicy policy, Instant now) {
        if (assessmentCandidateResultRepository.findBySessionId(session.getId()).isPresent()) {
            return;
        }
        if (!assessmentItemResponseRepository.findBySessionId(session.getId()).isEmpty()) {
            return;
        }
        session.setStatus(AssessmentSessionStatus.GRADED);
        if (session.getSubmittedAt() == null) {
            session.setSubmittedAt(now);
        }
        AssessmentSession saved = assessmentSessionRepository.save(session);
        releaseZeroForSession(saved, policy, now);
    }

    private AssessmentCandidateResult releaseZeroForSession(AssessmentSession session, Instant now) {
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        return releaseZeroForSession(session, resolvePolicy(assessment), now);
    }

    private AssessmentCandidateResult releaseZeroForSession(AssessmentSession session, AssessmentPolicy policy, Instant now) {
        AssessmentCandidateResult existing = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        AssessmentCandidateResult result = existing == null ? new AssessmentCandidateResult() : existing;
        BigDecimal floorScore = floorScore(policy);
        RubricResultBand rubricBand = resolveRubricBandForZero(policy, floorScore);

        result.setAssessmentId(session.getAssessmentId());
        result.setCandidateId(session.getCandidateId());
        result.setSessionId(session.getId());
        result.setAssessmentPolicyId(policy.getId());
        result.setPolicyVersion(policy.getVersion());
        result.setRubricVersionId(policy.getRubricVersionId());
        result.setFrameworkVersionId(policy.getFrameworkVersionId());
        result.setTargetFrameworkBandId(policy.getTargetFrameworkBandId());
        result.setRubricResultBandId(rubricBand == null ? null : rubricBand.getId());
        result.setTotalScore(floorScore);
        result.setStatus(AssessmentCandidateResultStatus.RELEASED);
        if (result.getReleasedAt() == null) {
            result.setReleasedAt(now);
        }
        return assessmentCandidateResultRepository.save(result);
    }

    private AssessmentPolicy resolvePolicy(com.sep.vox.domain.model.assessment.Assessment assessment) {
        if (assessment.getAssessmentPolicyId() == null) {
            throw new ResourceNotFoundException("The assessment has no assessment policy attached");
        }
        return assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment policy not found"));
    }

    private BigDecimal floorScore(AssessmentPolicy policy) {
        if (policy.getRubricVersionId() == null) {
            return ZERO_SCORE;
        }
        return rubricVersionRepository.findById(policy.getRubricVersionId())
            .map(version -> version.getScoringScaleMin())
            .map(min -> min.setScale(2, RoundingMode.HALF_UP))
            .orElse(ZERO_SCORE);
    }

    private RubricResultBand resolveRubricBandForZero(AssessmentPolicy policy, BigDecimal floorScore) {
        if (policy.getRubricVersionId() == null) {
            return null;
        }
        List<RubricResultBand> matchingBands = findBandsContaining(policy, floorScore);
        if (matchingBands.isEmpty()) {
            log.info(
                "No result score band contains {} (rubricVersionId={}) -- saving the floor-score result without a result band.",
                floorScore, policy.getRubricVersionId()
            );
            return null;
        }
        if (matchingBands.size() > 1) {
            log.error(
                "Result score band configuration OVERLAPS at {}: matches {} bands of rubricVersionId={}."
                    + " Taking the band with the smallest order ({}). An admin needs to fix the bands.",
                floorScore, matchingBands.size(), policy.getRubricVersionId(),
                matchingBands.get(0).getCode()
            );
        }
        return matchingBands.get(0);
    }

    private List<RubricResultBand> findBandsContaining(AssessmentPolicy policy, BigDecimal floorScore) {
        return rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
            .sorted(Comparator.comparingInt(band -> band.getOrder()))
            .filter(band -> band.getScoreMin() != null
                && band.getScoreMax() != null
                && floorScore.compareTo(band.getScoreMin()) >= 0
                && floorScore.compareTo(band.getScoreMax()) <= 0)
            .toList();
    }
}
