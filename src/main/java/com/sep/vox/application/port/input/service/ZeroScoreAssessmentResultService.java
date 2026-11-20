package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
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
public class ZeroScoreAssessmentResultService {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(ZeroScoreAssessmentResultService.class);

    /**
     * Chỉ còn là mức lùi khi không tra được thang của rubric -- KHÔNG còn là "điểm của bài trống".
     *
     * <p>Sửa 2026-08-11: điểm bài trống nay là SÀN CỦA THANG rubric (xem {@code floorScore}). Số 0
     * chỉ tình cờ đúng với thang bắt đầu từ 0; trường khai thang 4-10 thì 0 nằm dưới sàn và không
     * dải xếp loại nào chứa nó -- đúng những bài đáng ra phải được ghi nhận lại là những bài mất
     * xếp loại.
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

    public ZeroScoreAssessmentResultService(
            AssessmentRepository assessmentRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentItemResponseRepository assessmentItemResponseRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentItemResponseRepository = assessmentItemResponseRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
    }

    @Transactional
    public AssessmentCandidateResult releaseZeroForEmptySession(UUID sessionId) {
        AssessmentSession session = assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        return releaseZeroForSession(session, Instant.now());
    }

    @Transactional
    public void ensureZeroResultsForMissingOrEmptyAttempts(UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
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
            List<AssessmentSession> sessions = assessmentSessionRepository.findByCandidateId(candidate.getId()).stream()
                .filter(session -> assessmentId.equals(session.getAssessmentId()))
                .toList();
            if (sessions.isEmpty()) {
                UUID paperId = candidate.getAssignedPaperId() == null ? fallbackPaperId : candidate.getAssignedPaperId();
                if (paperId == null) {
                    continue;
                }
                AssessmentSession session = assessmentSessionRepository.save(new AssessmentSession(
                    assessmentId,
                    candidate.getId(),
                    paperId,
                    now,
                    now,
                    AssessmentSessionStatus.GRADED,
                    false,
                    null
                ));
                releaseZeroForSession(session, policy, now);
                continue;
            }

            for (AssessmentSession session : sessions) {
                if (assessmentCandidateResultRepository.findBySessionId(session.getId()).isPresent()) {
                    continue;
                }
                if (!assessmentItemResponseRepository.findBySessionId(session.getId()).isEmpty()) {
                    continue;
                }
                session.setStatus(AssessmentSessionStatus.GRADED);
                if (session.getSubmittedAt() == null) {
                    session.setSubmittedAt(now);
                }
                AssessmentSession saved = assessmentSessionRepository.save(session);
                releaseZeroForSession(saved, policy, now);
            }
        }
    }

    private AssessmentCandidateResult releaseZeroForSession(AssessmentSession session, Instant now) {
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
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
        if (existing == null) {
            result.setCreatedAt(now);
            result.setCreatedBy(null);
        }
        result.setUpdatedAt(now);
        result.setUpdatedBy(null);
        return assessmentCandidateResultRepository.save(result);
    }

    private AssessmentPolicy resolvePolicy(com.sep.vox.domain.model.assessment.Assessment assessment) {
        if (assessment.getAssessmentPolicyId() == null) {
            throw new NotFoundException("Bài kiểm tra chưa gắn assessment policy");
        }
        return assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy assessment policy"));
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
        List<RubricResultBand> matchingBands = rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
            .sorted(Comparator.comparingInt(band -> band.getOrder()))
            .filter(band -> band.getScoreMin() != null
                && band.getScoreMax() != null
                && floorScore.compareTo(band.getScoreMin()) >= 0
                && floorScore.compareTo(band.getScoreMax()) <= 0)
            .toList();
        if (matchingBands.isEmpty()) {
            LOGGER.info(
                "Không có dải điểm kết quả nào chứa {} (rubricVersionId={}) -- lưu kết quả điểm sàn không kèm xếp loại.",
                floorScore, policy.getRubricVersionId()
            );
            return null;
        }
        if (matchingBands.size() > 1) {
            LOGGER.error(
                "Cấu hình dải điểm CHỒNG LẤN tại {}: khớp {} dải của rubricVersionId={}."
                    + " Lấy dải order nhỏ nhất ({}). Quản trị cần sửa lại dải.",
                floorScore, matchingBands.size(), policy.getRubricVersionId(),
                matchingBands.get(0).getCode()
            );
        }
        return matchingBands.get(0);
    }
}
