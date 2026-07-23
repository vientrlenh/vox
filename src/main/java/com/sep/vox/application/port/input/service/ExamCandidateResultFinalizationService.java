package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

/**
 * G.3/G.4: logic dùng chung để "chốt" 1 ExamCandidateResult, tái dùng ở cả job/mutation
 * publish kết quả (G.3, chốt hàng loạt) lẫn buộc-kết-thúc phát hiện vi phạm muộn (G.4,
 * chốt ngay 1 kết quả đơn lẻ) để không viết trùng 2 chỗ.
 */
@Service
public class ExamCandidateResultFinalizationService {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamResultAppealRepository examResultAppealRepository;

    public ExamCandidateResultFinalizationService(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamResultAppealRepository examResultAppealRepository) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examResultAppealRepository = examResultAppealRepository;
    }

    /**
     * G.4 case 1: phát hiện vi phạm muộn cho 1 session đã có kết quả (RELEASED/FINAL với điểm
     * thật, hoặc bất kỳ trạng thái nào khác) - ghi đè ngay thành INVALID + totalScore = 0,
     * không đợi tới lúc kỳ thi công bố kết quả. Không làm gì nếu session chưa từng được chấm
     * (chưa có ExamCandidateResult nào) - trường hợp đó do luồng chấm bình thường tự xử lý
     * (candidate.blockedAt đã set, khi chấm xong sẽ tự ra INVALID qua G.2).
     */
    @Transactional
    public void invalidateNow(UUID sessionId, UUID actingUserId) {
        var result = examCandidateResultRepository.findBySessionId(sessionId).orElse(null);
        if (result == null) {
            return;
        }

        var now = OffsetDateTime.now();
        result.setStatus(ExamCandidateResultStatus.INVALID);
        result.setTotalScore(BigDecimal.ZERO.setScale(2));
        result.setUpdatedAt(now);
        result.setUpdatedBy(actingUserId);
        examCandidateResultRepository.save(result);

        closeOpenAppeals(result.getId(), actingUserId, now);
    }

    /**
     * Bài vừa bị vô hiệu vì phát hiện vi phạm muộn -> đóng (REJECTED) mọi đơn phúc khảo còn
     * đang xử lý của kết quả này. Nếu không đóng, đơn sẽ treo và lúc publish sẽ ép kết quả về
     * RELEASED, "hồi sinh" bài đã bị vô hiệu. REJECTED không đốt lượt phúc khảo (chỉ PUBLISHED
     * mới tính) nên không ảnh hưởng hạn mức nếu về sau kết quả được khôi phục.
     */
    private void closeOpenAppeals(UUID candidateResultId, UUID actingUserId, OffsetDateTime now) {
        if (candidateResultId == null) {
            return;
        }
        for (var appeal : examResultAppealRepository.findByCandidateResultId(candidateResultId)) {
            if (appeal.getStatus() == ExamAppealStatus.PUBLISHED
                    || appeal.getStatus() == ExamAppealStatus.REJECTED) {
                continue;
            }
            appeal.setStatus(ExamAppealStatus.REJECTED);
            appeal.setDecisionNote("Đơn phúc khảo bị đóng do bài thi đã bị vô hiệu vì phát hiện vi phạm.");
            appeal.setResolvedBy(actingUserId);
            appeal.setResolvedAt(now);
            examResultAppealRepository.save(appeal);
        }
    }

    /**
     * G.3: chốt PASSED/FAILED tại thời điểm kỳ thi RESULTS_PUBLISHED. Trả về true nếu có
     * thay đổi cần lưu (caller tự save để gộp vào 1 transaction chung).
     */
    public boolean finalizeForPublish(ExamCandidateResult result, BigDecimal passingScore) {
        if (result.getStatus() == ExamCandidateResultStatus.INVALID) {
            result.setTotalScore(BigDecimal.ZERO.setScale(2));
            result.setStatus(ExamCandidateResultStatus.FAILED);
            return true;
        }
        if (result.getStatus() == ExamCandidateResultStatus.RELEASED
                || result.getStatus() == ExamCandidateResultStatus.FINAL) {
            var passed = passingScore != null
                && result.getTotalScore() != null
                && result.getTotalScore().compareTo(passingScore) >= 0;
            result.setStatus(passed ? ExamCandidateResultStatus.PASSED : ExamCandidateResultStatus.FAILED);
            return true;
        }
        // APPEALED, RE_GRADING, RETAKE_REQUIRED, PASSED/FAILED (đã chốt trước đó) - ngoài phạm vi, giữ nguyên.
        return false;
    }
}
