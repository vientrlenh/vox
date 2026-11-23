package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

/**
 * G.3/G.4: shared logic to "finalize" one AssessmentCandidateResult, reused by both the job/mutation
 * that publishes results (G.3, bulk finalize) and the forced end on a late-detected violation (G.4,
 * finalize a single result right away) so that it is not written twice.
 */
@Service
@RequiredArgsConstructor
public class AssessmentCandidateResultFinalizationService {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;

    /**
     * G.4 case 1: a violation detected late for a session that already has a result (RELEASED/FINAL with a real
     * score, or any other status) - overwritten right away to INVALID + totalScore = 0,
     * without waiting until the assessment publishes its results. Does nothing if the session has never been graded
     * (no AssessmentCandidateResult yet) - that case is handled by the normal grading flow itself
     * (candidate.blockedAt is already set, so when grading finishes it ends up INVALID through G.2).
     */
    @Transactional
    public void invalidateNow(UUID sessionId, UUID actingUserId) {
        AssessmentCandidateResult result = assessmentCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        if (result == null) {
            return;
        }

        Instant now = Instant.now();
        result.setStatus(AssessmentCandidateResultStatus.INVALID);
        result.setTotalScore(BigDecimal.ZERO.setScale(2));
        assessmentCandidateResultRepository.save(result);

        closeOpenAppeals(result.getId(), actingUserId, now);
    }

    /**
     * Bài vừa bị vô hiệu vì phát hiện vi phạm muộn -> đóng (REJECTED) mọi đơn phúc khảo còn
     * đang xử lý của kết quả này. Nếu không đóng, đơn sẽ treo và lúc publish sẽ ép kết quả về
     * RELEASED, "hồi sinh" bài đã bị vô hiệu. REJECTED không đốt lượt phúc khảo (chỉ PUBLISHED
     * mới tính) nên không ảnh hưởng hạn mức nếu về sau kết quả được khôi phục.
     */
    private void closeOpenAppeals(UUID candidateResultId, UUID actingUserId, Instant now) {
        if (candidateResultId == null) {
            return;
        }
        for (AssessmentResultAppeal appeal : assessmentResultAppealRepository.findByCandidateResultId(candidateResultId)) {
            if (appeal.getStatus() == AssessmentAppealStatus.PUBLISHED
                    || appeal.getStatus() == AssessmentAppealStatus.REJECTED) {
                continue;
            }
            appeal.setStatus(AssessmentAppealStatus.REJECTED);
            appeal.setDecisionNote("Đơn phúc khảo bị đóng do bài thi đã bị vô hiệu vì phát hiện vi phạm.");
            appeal.setResolvedBy(actingUserId);
            appeal.setResolvedAt(now);
            assessmentResultAppealRepository.save(appeal);
        }
    }

    /**
     * G.3: finalize at the moment the assessment is RESULTS_PUBLISHED. Returns true if there is a change that needs saving
     * (the caller saves, so everything joins one shared transaction).
     *
     * INVALID always jumps to FAILED automatically (score forced to 0) whether or not there is a passingScore -
     * a result that has been invalidated (violation/system error) cannot "pass".
     *
     * RELEASED/FINAL: if the policy HAS a passingScore, PASSED/FAILED is decided automatically from the score as before. If it has
     * NO passingScore (no threshold to compare against automatically), it is parked at FINAL - the school
     * decides PASSED/FAILED manually afterwards through DecideAssessmentCandidateResultOutcomeUseCase.
     */
    public boolean finalizeForPublish(AssessmentCandidateResult result, BigDecimal passingScore) {
        if (result.getStatus() == AssessmentCandidateResultStatus.INVALID) {
            result.setTotalScore(BigDecimal.ZERO.setScale(2));
            result.setStatus(AssessmentCandidateResultStatus.FAILED);
            return true;
        }
        if (result.getStatus() == AssessmentCandidateResultStatus.RELEASED
                || result.getStatus() == AssessmentCandidateResultStatus.FINAL) {
            if (passingScore == null) {
                if (result.getStatus() == AssessmentCandidateResultStatus.FINAL) {
                    return false;
                }
                result.setStatus(AssessmentCandidateResultStatus.FINAL);
                return true;
            }
            boolean passed = result.getTotalScore() != null
                && result.getTotalScore().compareTo(passingScore) >= 0;
            result.setStatus(passed ? AssessmentCandidateResultStatus.PASSED : AssessmentCandidateResultStatus.FAILED);
            return true;
        }
        // APPEALED, RE_GRADING, RETAKE_REQUIRED, PASSED/FAILED (đã chốt trước đó) - ngoài phạm vi, giữ nguyên.
        return false;
    }
}
