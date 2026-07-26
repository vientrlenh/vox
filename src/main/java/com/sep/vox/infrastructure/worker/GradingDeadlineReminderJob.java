package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.GradingDeadlineReminderEvent;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;

/**
 * Nhắc giáo viên khi phân công sắp tới hạn.
 *
 * <p>Chống nhắc trùng bằng cột {@code reminded_at} chứ không bằng lịch chạy: job chạy
 * mỗi 15 phút và có thể chạy lại sau khi restart, nên "đã nhắc chưa" phải nằm ở dữ
 * liệu. Đánh dấu {@code reminded_at} TRƯỚC khi phát sự kiện — listener gửi mail chạy
 * sau commit, nếu đánh dấu sau thì một lần rollback là gửi lại từ đầu.
 *
 * <p>Ngưỡng là {@value #REMIND_BEFORE_HOURS} giờ trước hạn: đủ sớm để còn kịp chấm,
 * đủ muộn để không nhắc những việc vừa mới giao.
 */
@Component
public class GradingDeadlineReminderJob {

    private static final Logger log = LoggerFactory.getLogger(GradingDeadlineReminderJob.class);
    private static final int REMIND_BEFORE_HOURS = 24;

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final EventPublisherPort eventPublisherPort;

    public GradingDeadlineReminderJob(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            EventPublisherPort eventPublisherPort) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Scheduled(fixedDelay = 900000)
    @Transactional
    public void remind() {
        var now = OffsetDateTime.now();
        var threshold = now.plus(Duration.ofHours(REMIND_BEFORE_HOURS));
        var due = examGradingAssignmentRepository.findDueForReminder(threshold);
        if (due.isEmpty()) {
            return;
        }

        // Nạp kết quả + kỳ thi theo lô: job này có thể quét hàng trăm dòng một lần,
        // gọi lẻ từng dòng là N+1 ngay trong background.
        var resultsById = examCandidateResultRepository.findByIdIn(
                due.stream().map(assignment -> assignment.getCandidateResultId()).distinct().toList()).stream()
            .collect(java.util.stream.Collectors.toMap(
                result -> result.getId(), java.util.function.Function.identity(), (left, right) -> left));
        var examNamesById = examRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getExamId()).distinct().toList()).stream()
            .collect(java.util.stream.Collectors.toMap(
                exam -> exam.getId(), exam -> exam.getName(), (left, right) -> left));

        for (var assignment : due) {
            assignment.setRemindedAt(now);
            examGradingAssignmentRepository.save(assignment);

            var result = resultsById.get(assignment.getCandidateResultId());
            var examName = result == null ? null : examNamesById.get(result.getExamId());
            eventPublisherPort.publish(new GradingDeadlineReminderEvent(
                assignment.getId(),
                assignment.getTeacherId(),
                examName,
                assignment.getRoundType() == null ? null : assignment.getRoundType().name(),
                assignment.getDeadlineAt()
            ));
        }
        log.info("Đã gửi nhắc hạn chấm cho {} phân công", due.size());
    }
}
