package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;

/**
 * G.3/G.4: logic dùng chung để "chốt" 1 AssessmentCandidateResult, tái dùng ở cả job/mutation
 * publish kết quả (G.3, chốt hàng loạt) lẫn buộc-kết-thúc phát hiện vi phạm muộn (G.4,
 * chốt ngay 1 kết quả đơn lẻ) để không viết trùng 2 chỗ.
 */
@Service
public class AssessmentCandidateResultFinalizationService {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;

    public AssessmentCandidateResultFinalizationService(
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentResultAppealRepository assessmentResultAppealRepository) {
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
    }

    /**
     * G.4 case 1: phát hiện vi phạm muộn cho 1 session đã có kết quả (RELEASED/FINAL với điểm
     * thật, hoặc bất kỳ trạng thái nào khác) - ghi đè ngay thành INVALID + totalScore = 0,
     * không đợi tới lúc kỳ thi công bố kết quả. Không làm gì nếu session chưa từng được chấm
     * (chưa có AssessmentCandidateResult nào) - trường hợp đó do luồng chấm bình thường tự xử lý
     * (candidate.blockedAt đã set, khi chấm xong sẽ tự ra INVALID qua G.2).
     */
    @Transactional
    public void invalidateNow(UUID sessionId, UUID actingUserId) {
        var result = assessmentCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        if (result == null) {
            return;
        }

        var now = Instant.now();
        result.setStatus(AssessmentCandidateResultStatus.INVALID);
        result.setTotalScore(BigDecimal.ZERO.setScale(2));
        result.setUpdatedAt(now);
        result.setUpdatedBy(actingUserId);
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
        for (var appeal : assessmentResultAppealRepository.findByCandidateResultId(candidateResultId)) {
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
     * G.3: chốt tại thời điểm kỳ thi RESULTS_PUBLISHED. Trả về true nếu có thay đổi cần lưu
     * (caller tự save để gộp vào 1 transaction chung).
     *
     * INVALID luôn tự động nhảy sang FAILED (điểm ép về 0) bất kể có passingScore hay không -
     * kết quả đã bị vô hiệu (vi phạm/lỗi hệ thống) thì không thể "đậu".
     *
     * RELEASED/FINAL: nếu policy CÓ passingScore, tự chốt PASSED/FAILED theo điểm như cũ. Nếu
     * KHÔNG có passingScore (không có ngưỡng để tự so sánh), chốt tạm ở FINAL - nhà trường tự
     * quyết định PASSED/FAILED thủ công sau đó qua DecideAssessmentCandidateResultOutcomeUseCase.
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
            var passed = result.getTotalScore() != null
                && result.getTotalScore().compareTo(passingScore) >= 0;
            result.setStatus(passed ? AssessmentCandidateResultStatus.PASSED : AssessmentCandidateResultStatus.FAILED);
            return true;
        }
        // APPEALED, RE_GRADING, RETAKE_REQUIRED, PASSED/FAILED (đã chốt trước đó) - ngoài phạm vi, giữ nguyên.
        return false;
    }
}
