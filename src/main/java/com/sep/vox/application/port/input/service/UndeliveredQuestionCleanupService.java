package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class UndeliveredQuestionCleanupService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(UndeliveredQuestionCleanupService.class);

    private final PracticePaperItemRepository paperItemRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final StudentQuestionExposureRepository studentQuestionExposureRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;

    public UndeliveredQuestionCleanupService(
            PracticePaperItemRepository paperItemRepository,
            PracticeItemResponseRepository practiceItemResponseRepository,
            StudentQuestionExposureRepository studentQuestionExposureRepository,
            PracticeQuestionRepository practiceQuestionRepository) {
        this.paperItemRepository = paperItemRepository;
        this.practiceItemResponseRepository = practiceItemResponseRepository;
        this.studentQuestionExposureRepository = studentQuestionExposureRepository;
        this.practiceQuestionRepository = practiceQuestionRepository;
    }

    /**
     * @param paperId đề của phiên vừa đóng; {@code null} thì không có gì để dọn.
     * @return {@code true} nếu thật sự đã trả một câu về kho.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseUndeliveredQuestion(UUID studentId, UUID sessionId, UUID paperId) {
        if (paperId == null) {
            return false;
        }
        var questionIds = paperItemRepository.findQuestionIdsForPaper(paperId);
        if (questionIds.isEmpty()) {
            return false;
        }
        var lastQuestionId = questionIds.getLast();
        if (practiceItemResponseRepository.existsResponse(sessionId, lastQuestionId)) {
            // Học sinh ĐÃ trả lời câu cuối -- không có gì thừa.
            return false;
        }

        // Xoá item TRƯỚC và dùng số dòng bị xoá làm chốt: nếu một lượt next-question khác vừa
        // chèn thêm slot mới xen vào giữa, câu vừa đọc không còn là slot cuối và câu lệnh trả 0.
        // Lúc đó phải dừng -- gỡ exposure của một câu vẫn đang được dùng là đẩy nó quay lại kho
        // trong khi học sinh sắp được hỏi chính nó.
        var deleted = paperItemRepository.deleteLastItemForPaper(paperId, lastQuestionId);
        if (deleted == 0) {
            return false;
        }
        studentQuestionExposureRepository.removeExposure(studentId, lastQuestionId);
        practiceQuestionRepository.decrementUsageCount(lastQuestionId);
        LOGGER.info(
            "Trả câu {} về kho: đã chọn cho phiên {} nhưng học sinh chưa trả lời.",
            lastQuestionId,
            sessionId
        );
        return true;
    }
}
