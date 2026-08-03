package com.sep.vox.infrastructure.worker.personalization;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

import com.sep.vox.application.event.ExamResultsPublishedEvent;
import com.sep.vox.application.event.PracticeSessionEndedEvent;
import com.sep.vox.application.port.input.service.WeaknessSnapshotRefreshService;

@Component
public class WeaknessSnapshotRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(WeaknessSnapshotRefreshJob.class);

    private final WeaknessSnapshotRefreshService refreshService;

    public WeaknessSnapshotRefreshJob(WeaknessSnapshotRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Scheduled(
        fixedDelayString = "${app.personalization.weakness.refresh-delay-ms:60000}",
        initialDelayString = "${app.personalization.weakness.refresh-initial-delay-ms:60000}"
    )
    public void run() {
        try {
            var refreshed = refreshService.refreshStaleBatch(Instant.now());
            if (refreshed > 0) {
                log.info("Đã làm mới weakness snapshot cho {} học sinh", refreshed);
            }
        } catch (Exception exception) {
            log.warn("Không thể chạy việc nền làm mới weakness snapshot", exception);
        }
    }

    // Trước đây chỉ trông vào vòng quét mỗi phút (run() ở trên) -- một phiên luyện mới kết
    // thúc phải đợi tới lượt quét kế tiếp mới được làm mới, dù rất hiếm khi lâu. Nghe thẳng
    // event này để phiên vừa xong được cập nhật ngay, cùng kiểu onExamResultsPublished bên
    // dưới và TopicSuggestionSessionListener đang làm cho gợi ý chủ đề.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPracticeSessionEnded(PracticeSessionEndedEvent event) {
        try {
            refreshService.refreshStudents(List.of(event.studentId()), Instant.now());
            log.info(
                "Đã làm mới weakness snapshot cho học sinh {} sau khi kết thúc phiên luyện {}",
                event.studentId(),
                event.sessionId()
            );
        } catch (Exception exception) {
            log.warn(
                "Không thể làm mới weakness snapshot sau khi kết thúc phiên luyện {}",
                event.sessionId(),
                exception
            );
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamResultsPublished(ExamResultsPublishedEvent event) {
        try {
            var refreshed = refreshService.refreshExam(event.examId(), Instant.now());
            log.info(
                "Đã làm mới weakness snapshot cho {} học sinh sau khi công bố kỳ thi {}",
                refreshed,
                event.examId()
            );
        } catch (Exception exception) {
            log.warn(
                "Không thể làm mới weakness snapshot sau khi công bố kỳ thi {}",
                event.examId(),
                exception
            );
        }
    }
}
