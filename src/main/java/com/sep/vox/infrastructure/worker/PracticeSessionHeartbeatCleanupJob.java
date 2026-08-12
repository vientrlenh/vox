package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.PracticeGradingFlushService;
import com.sep.vox.application.port.input.service.PracticeSessionCleanupService;

@Component
public class PracticeSessionHeartbeatCleanupJob {

    /**
     * Chỉ quét lại phiên đóng trong vòng 24h. Nếu việc chấm hỏng thật thì quét không giới hạn
     * sẽ bắn lại cùng bộ sự kiện mỗi 5 phút, mãi mãi -- quá hạn thì thôi, và màn tổng kết nói
     * thẳng là chấm không xong thay vì quay tiếp.
     */
    private static final Duration ORPHAN_SWEEP_WINDOW = Duration.ofHours(24);

    private final PracticeSessionCleanupService cleanupService;
    private final PracticeGradingFlushService gradingFlushService;

    public PracticeSessionHeartbeatCleanupJob(
            PracticeSessionCleanupService cleanupService,
            PracticeGradingFlushService gradingFlushService) {
        this.cleanupService = cleanupService;
        this.gradingFlushService = gradingFlushService;
    }

    /**
     * Nới 3 -> 10 phút (2026-08-12). Ba phút quá ngắn cho luyện NÓI: riêng câu hỏi AI đọc đã gần
     * một phút, học sinh nghĩ rồi nói thêm một hai phút nữa. Cộng với việc trước đây không ai cập
     * nhật last_heartbeat_at, ngưỡng này giết MỌI phiên bình thường ở phút thứ 3.
     *
     * <p>Nay agents gửi nhịp mỗi 60 giây nên 10 phút rất rộng: phiên chết thật vẫn được dọn sau
     * 10 phút, còn phiên đang chạy phải mất 10 nhịp liên tiếp mới bị đụng tới.
     */
    private static final Duration STALE_AFTER = Duration.ofMinutes(10);

    @Scheduled(fixedDelayString = "${app.practice.heartbeat-cleanup-ms:300000}")
    public void cleanup() {
        cleanupService.cleanupStaleSessions(
            Instant.now().minus(STALE_AFTER)
        );
        // Sau khi đóng phiên treo mới quét mồ côi: phiên vừa bị đóng ở trên cũng đã kịp xả
        // chấm, nên vòng này chỉ còn lại đúng những lượt thật sự lọt lưới.
        gradingFlushService.sweepEndedSessions(Instant.now().minus(ORPHAN_SWEEP_WINDOW));
    }
}
