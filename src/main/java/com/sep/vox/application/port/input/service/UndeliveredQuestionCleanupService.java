package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.domain.repository.PracticeQuestionRepository;
import com.sep.vox.domain.repository.StudentQuestionExposureRepository;

/**
 * Trả câu hỏi ĐÃ CHỌN nhưng học sinh CHƯA BAO GIỜ trả lời về lại kho, khi phiên đóng.
 *
 * <p><b>Vì sao cần.</b> Chọn một câu ghi ba thứ, không phải một:
 *
 * <ol>
 *   <li>{@code practice_paper_items} -- câu đã vào đề</li>
 *   <li>{@code student_question_exposures} -- <b>dấu "đã gặp"</b>, và mọi truy vấn chọn câu đều
 *       lọc {@code exposure.id IS NULL}</li>
 *   <li>{@code practice_questions.usage_count += 1}</li>
 * </ol>
 *
 * Thứ hai mới là thứ đau: một câu chỉ được chọn rồi bỏ dở sẽ <b>biến mất vĩnh viễn</b> khỏi kho
 * của học sinh đó, dù em ấy chưa từng nhìn thấy nó.
 *
 * <p><b>Khi nào xảy ra.</b> Hai đường, và đường thứ hai là mới:
 *
 * <ul>
 *   <li>Học sinh được đẩy câu rồi đóng app không trả lời -- vốn đã có từ trước.</li>
 *   <li>Nạp trước (prefetch) phía Python: câu tiếp theo được sinh sẵn trong lúc học sinh còn
 *       đang follow-up, để giấu 10-40 giây gọi LLM. Chuỗi follow-up kết thúc bình thường thì
 *       câu đó được dùng ngay; phiên đứt giữa chừng thì nó thừa ra.</li>
 * </ul>
 *
 * <p><b>Chỉ kiểm slot CUỐI.</b> Slot ở giữa không thể chưa trả lời: nếu có,
 * {@code ResolveNextPracticeQuestionClaimService} đã trả lại chính nó (nhánh idempotent
 * {@code existsResponse}) thay vì chọn câu mới, nên không bao giờ có slot mới chèn sau nó.
 *
 * <p><b>{@code REQUIRES_NEW}.</b> Gọi từ {@code PracticeSessionClosedHandler} -- nơi đã chạy
 * trong transaction riêng và có thể ném ra ngoài. Dọn dẹp hỏng KHÔNG được phép kéo theo việc
 * ghi điểm quan tâm hay đóng phiên: phiên đã đóng là sự thật, còn đây chỉ là trả lại kho.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UndeliveredQuestionCleanupService {

    private final PracticePaperItemRepository paperItemRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;

    /**
     * The item is deleted FIRST and the number of deleted rows is used as the guard: if another next-question run has
     * just inserted a new slot in between, the question just read is no longer the last slot and the statement returns 0.
     * We must stop then -- removing the exposure of a question that is still in use would push it back into the bank
     * while the student is about to be asked that very question.
     *
     * @param paperId the paper of the session that was just closed; {@code null} means there is nothing to clean up.
     * @return {@code true} if a question was really returned to the bank.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseUndeliveredQuestion(UUID studentId, UUID sessionId, UUID paperId) {
        if (paperId == null) {
            return false;
        }
        UUID lastQuestionId = findUnansweredLastQuestionId(sessionId, paperId);
        if (lastQuestionId == null) {
            return false;
        }
        int deleted = paperItemRepository.deleteLastItemForPaper(paperId, lastQuestionId);
        if (deleted == 0) {
            return false;
        }
        studentQuestionExposureRepository.removeExposure(studentId, lastQuestionId);
        practiceQuestionRepository.decrementUsageCount(lastQuestionId);
        log.info(
            "Trả câu {} về kho: đã chọn cho phiên {} nhưng học sinh chưa trả lời.",
            lastQuestionId,
            sessionId
        );
        return true;
    }

    /** @return the last question of the paper when the student never answered it, otherwise {@code null}. */
    private UUID findUnansweredLastQuestionId(UUID sessionId, UUID paperId) {
        List<UUID> questionIds = paperItemRepository.findQuestionIdsForPaper(paperId);
        if (questionIds.isEmpty()) {
            return null;
        }
        UUID lastQuestionId = questionIds.getLast();
        if (practiceItemResponseRepository.existsResponse(sessionId, lastQuestionId)) {
            // The student ALREADY answered the last question -- nothing is left over.
            return null;
        }
        return lastQuestionId;
    }
}
