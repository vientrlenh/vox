package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.ExamHumanGradingNotificationService;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Quét bài thi đang có bài "Chờ soát điểm AI" mà chưa nhắc lần nào, rồi báo cho người phụ trách.
 *
 * <p>Chạy trong lúc bài thi CÒN DIỄN RA chứ không đợi đóng bài: một ca thi kết thúc sớm đã có kết
 * quả ngay, và bắt người chấm ngồi chờ tới lúc đóng bài là vứt đi phần lớn thời gian chấm được.
 * Với kỳ thi kéo dài nhiều ngày thì khoảng chờ đó là nhiều ngày.
 *
 * <p>Một bài thi được nhắc nhiều nhất MỘT lần: chốt nằm ở {@code exams.human_grading_notified_at},
 * do chính service đóng dấu trong cùng transaction với outbox. Truy vấn đã lọc sẵn dòng chưa nhắc
 * nên lượt quét bình thường không đọc gì -- xem
 * {@code SpringDataExamRepository.findDueForHumanGradingNotice}.
 *
 * <p>Tách khỏi {@link ExamStatusAutoTransitionJob} dù cùng nhịp: job kia chỉ đụng bài tới hạn đóng,
 * còn lượt quét này phải nhìn cả bài đang chạy. Gộp vào là buộc job kia mở rộng phạm vi quét chỉ vì
 * dùng chung cái đồng hồ.
 */
@Component
public class ExamHumanGradingNoticeBatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamHumanGradingNoticeBatch.class);

    private final ExamRepository examRepository;
    private final ExamHumanGradingNotificationService examHumanGradingNotificationService;

    public ExamHumanGradingNoticeBatch(
            ExamRepository examRepository,
            ExamHumanGradingNotificationService examHumanGradingNotificationService) {
        this.examRepository = examRepository;
        this.examHumanGradingNotificationService = examHumanGradingNotificationService;
    }

    /**
     * Không {@code @Transactional} ở đây: mỗi bài thi phải là một transaction riêng của
     * {@code publishIfPendingReview}. Bọc cả lượt trong một transaction nghĩa là một bài thi hỏng
     * kéo theo rollback đánh dấu của tất cả những bài đã xử lý xong trước nó, và lượt sau nhắc lại
     * từ đầu.
     */
    @Scheduled(fixedDelay = 60000)
    public void run() {
        var due = examRepository.findDueForHumanGradingNotice();
        if (due.isEmpty()) {
            return;
        }

        var now = Instant.now();
        var notified = 0;
        for (var exam : due) {
            try {
                examHumanGradingNotificationService.publishIfPendingReview(exam, now);
                notified++;
            } catch (Exception e) {
                // Một bài thi hỏng (mất người nhận, dữ liệu lệch) không được cắt mất phần còn lại
                // của lượt quét. Dấu chưa đóng nên bài này sẽ được thử lại ở tick sau.
                LOGGER.error("Không nhắc được chấm tay cho bài thi {}", exam.getId(), e);
            }
        }

        LOGGER.info("Quét nhắc chấm tay: {}/{} bài thi đã được báo", notified, due.size());
    }
}
