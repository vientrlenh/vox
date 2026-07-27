package com.sep.vox.infrastructure.worker;

import java.time.OffsetDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.GradingDeadlineReminderEvent;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * MỘT lô nhắc hạn, trong MỘT transaction.
 *
 * <p>Tách khỏi {@link GradingDeadlineReminderJob} vì ranh giới transaction phải nằm ở
 * từng lô chứ không ở cả lượt chạy: {@code findDueForReminder} có {@code LIMIT} để một
 * transaction không ôm cả nghìn dòng, nên muốn xử lý hết tồn đọng thì phải chạy nhiều
 * lô — mà nhiều lô trong một transaction thì đúng bằng không có LIMIT. Cùng khuôn với
 * {@code ImportJobDispatcher} + {@code ImportCommitService}.
 *
 * <p>Đây cũng là lý do việc tách là bắt buộc chứ không phải cho đẹp: gọi thẳng một
 * method {@code @Transactional} của chính mình sẽ đi tắt qua proxy và mất luôn
 * transaction — mà mất transaction thì {@code FOR UPDATE SKIP LOCKED} trong câu select
 * không còn giữ khoá tới lúc commit, tức mất luôn phần chống trùng giữa các instance.
 */
@Component
public class GradingDeadlineReminderBatch {

    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamRepository examRepository;
    private final EventPublisherPort eventPublisherPort;

    public GradingDeadlineReminderBatch(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            EventPublisherPort eventPublisherPort) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.eventPublisherPort = eventPublisherPort;
    }

    /**
     * Đánh dấu {@code reminded_at} TRƯỚC khi phát sự kiện — listener gửi mail chạy sau
     * commit, nếu đánh dấu sau thì một lần rollback là gửi lại từ đầu.
     *
     * @return số phân công đã xử lý; {@code 0} nghĩa là hết tồn đọng
     */
    @Transactional
    public int remindOnce(OffsetDateTime threshold) {
        var due = examGradingAssignmentRepository.findDueForReminder(threshold);
        if (due.isEmpty()) {
            return 0;
        }
        var now = OffsetDateTime.now();

        // Nạp kết quả + kỳ thi theo lô: một lô có thể tới vài trăm dòng, gọi lẻ từng
        // dòng là N+1 ngay trong background.
        var resultsById = examCandidateResultRepository.findByIdIn(
                due.stream().map(assignment -> assignment.getCandidateResultId()).distinct().toList()).stream()
            .collect(Collectors.toMap(
                result -> result.getId(), Function.identity(), (left, right) -> left));
        var examNamesById = examRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getExamId()).distinct().toList()).stream()
            .collect(Collectors.toMap(
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
        return due.size();
    }
}
