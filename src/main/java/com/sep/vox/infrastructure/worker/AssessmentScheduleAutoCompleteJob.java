package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;

/**
 * A published session whose end time has passed automatically becomes COMPLETED. This is the normal path: a session
 * closes when its time runs out, not waiting until the whole class test closes.
 *
 * <p>Kept apart from {@code AssessmentStatusAutoTransitionJob} because that job only writes {@code AssessmentStatus} and deliberately skips
 * centralized assessments, while the session sweep must run for both kinds.
 */
@Component
public class AssessmentScheduleAutoCompleteJob {

    private static final Logger log = LoggerFactory.getLogger(AssessmentScheduleAutoCompleteJob.class);

    /**
     * Trần mỗi lượt. Lần chạy đầu sau khi triển khai phải quét cả tồn đọng lịch sử (mọi ca đã hết
     * giờ từ trước vẫn đang nằm ở PUBLISHED), nên rải dần thay vì nuốt một phát.
     */
    private static final int BATCH_SIZE = 200;

    private final AssessmentScheduleClosureService assessmentScheduleClosureService;

    public AssessmentScheduleAutoCompleteJob(AssessmentScheduleClosureService assessmentScheduleClosureService) {
        this.assessmentScheduleClosureService = assessmentScheduleClosureService;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        int completed = assessmentScheduleClosureService.completeEndedSchedules(Instant.now(), BATCH_SIZE);
        if (completed > 0) {
            log.info("Tự đóng {} ca thi đã qua giờ kết thúc", completed);
        }
    }
}
