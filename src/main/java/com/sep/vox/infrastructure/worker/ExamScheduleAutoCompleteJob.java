package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.ExamScheduleClosureService;

/**
 * Ca thi đã công bố mà đã qua giờ kết thúc thì tự chuyển COMPLETED. Đây là đường bình thường: ca
 * đóng khi hết giờ, không phải chờ tới lúc cả bài kiểm tra đóng.
 *
 * <p>Tách khỏi {@code ExamStatusAutoTransitionJob} vì job đó chỉ ghi {@code ExamStatus} và cố ý bỏ
 * qua kỳ thi tập trung, còn quét ca phải chạy cho cả hai loại bài.
 */
@Component
public class ExamScheduleAutoCompleteJob {

    private static final Logger log = LoggerFactory.getLogger(ExamScheduleAutoCompleteJob.class);

    /**
     * Trần mỗi lượt. Lần chạy đầu sau khi triển khai phải quét cả tồn đọng lịch sử (mọi ca đã hết
     * giờ từ trước vẫn đang nằm ở PUBLISHED), nên rải dần thay vì nuốt một phát.
     */
    private static final int BATCH_SIZE = 200;

    private final ExamScheduleClosureService examScheduleClosureService;

    public ExamScheduleAutoCompleteJob(ExamScheduleClosureService examScheduleClosureService) {
        this.examScheduleClosureService = examScheduleClosureService;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        var completed = examScheduleClosureService.completeEndedSchedules(Instant.now(), BATCH_SIZE);
        if (completed > 0) {
            log.info("Tự đóng {} ca thi đã qua giờ kết thúc", completed);
        }
    }
}
