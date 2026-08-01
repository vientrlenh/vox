package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nhắc giáo viên khi phân công sắp tới hạn.
 *
 * <p>Chống nhắc trùng bằng cột {@code reminded_at} chứ không bằng lịch chạy: job chạy
 * mỗi 15 phút và có thể chạy lại sau khi restart, nên "đã nhắc chưa" phải nằm ở dữ
 * liệu. Giữa nhiều instance thì chốt là {@code FOR UPDATE SKIP LOCKED} trong
 * {@code findDueForReminder} — {@code reminded_at} một mình chỉ chống trùng qua các
 * LƯỢT, không chống trùng giữa các tiến trình chạy song song.
 *
 * <p>Ngưỡng là {@value #REMIND_BEFORE_HOURS} giờ trước hạn: đủ sớm để còn kịp chấm,
 * đủ muộn để không nhắc những việc vừa mới giao.
 *
 * <p><strong>Vì sao lặp theo lô:</strong> câu select có {@code LIMIT} để một transaction
 * không ôm cả nghìn dòng. Nếu mỗi lượt chạy chỉ làm đúng một lô thì tồn đọng lớn sẽ
 * trôi theo tốc độ lô/15-phút, và mail "nhắc trước hạn" có thể tới SAU khi đã quá hạn —
 * đúng thứ job này sinh ra để tránh. Nên ở đây chạy tới khi hết việc, mỗi lô một
 * transaction riêng, kèm trần {@value #MAX_PASSES_PER_RUN} lô để một lượt không kéo dài
 * vô hạn nếu có dòng cứ quay lại.
 */
@Component
public class GradingDeadlineReminderJob {

    private static final Logger log = LoggerFactory.getLogger(GradingDeadlineReminderJob.class);
    private static final int REMIND_BEFORE_HOURS = 24;
    private static final int MAX_PASSES_PER_RUN = 20;

    private final GradingDeadlineReminderBatch gradingDeadlineReminderBatch;

    public GradingDeadlineReminderJob(GradingDeadlineReminderBatch gradingDeadlineReminderBatch) {
        this.gradingDeadlineReminderBatch = gradingDeadlineReminderBatch;
    }

    @Scheduled(fixedDelay = 900000)
    public void remind() {
        // Ngưỡng chốt MỘT lần cho cả lượt: tính lại mỗi lô sẽ làm cửa sổ trôi dần và
        // kéo vào những phân công chưa tới lúc nhắc.
        var threshold = Instant.now().plus(Duration.ofHours(REMIND_BEFORE_HOURS));
        var total = 0;

        for (var pass = 0; pass < MAX_PASSES_PER_RUN; pass++) {
            int reminded;
            try {
                reminded = gradingDeadlineReminderBatch.remindOnce(threshold);
            } catch (Exception e) {
                // Lô sau cũng sẽ hỏng vì cùng nguyên nhân; dừng và để lượt 15 phút sau
                // thử lại thay vì quay vòng trên một lỗi.
                log.error("Lỗi khi gửi nhắc hạn chấm, dừng lượt này", e);
                break;
            }
            if (reminded == 0) {
                break;
            }
            total += reminded;
        }

        if (total > 0) {
            log.info("Đã gửi nhắc hạn chấm cho {} phân công", total);
        }
    }
}
