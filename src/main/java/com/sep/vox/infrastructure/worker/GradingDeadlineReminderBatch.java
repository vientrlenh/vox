package com.sep.vox.infrastructure.worker;

import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.GradingDeadlineReminderPayloadV1;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.OutboxRepository;

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
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public GradingDeadlineReminderBatch(
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamRepository examRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examRepository = examRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    /**
     * {@code reminded_at} và dòng outbox cùng nằm trong MỘT transaction, nên thứ tự ghi
     * không còn quan trọng: hoặc cả hai cùng commit, hoặc cả hai cùng mất và lô sau nhắc
     * lại. Trước đây mail gửi sau commit nên phải đánh dấu trước, và đổi lại là một lỗi
     * SMTP làm mất luôn lượt nhắc của phân công đó.
     *
     * @return số phân công đã xử lý; {@code 0} nghĩa là hết tồn đọng
     */
    @Transactional
    public int remindOnce(Instant threshold) {
        var due = examGradingAssignmentRepository.findDueForReminder(threshold);
        if (due.isEmpty()) {
            return 0;
        }
        var now = Instant.now();

        // Nạp kết quả + kỳ thi theo lô: một lô có thể tới vài trăm dòng, gọi lẻ từng
        // dòng là N+1 ngay trong background.
        var resultsById = examCandidateResultRepository.findByIdIn(
                due.stream().map(assignment -> assignment.getCandidateResultId()).distinct().toList()).stream()
            .collect(Collectors.toMap(
                result -> result.getId(), Function.identity(), (left, right) -> left));
        // Giữ nguyên Exam thay vì chỉ rút lấy tên: thông báo còn cần id và loại bài để mở
        // đúng màn hình chấm, mà lô này là chỗ duy nhất trong luồng còn cầm Exam trong tay.
        var examsById = examRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getExamId()).distinct().toList()).stream()
            .collect(Collectors.toMap(
                exam -> exam.getId(), Function.identity(), (left, right) -> left));

        for (var assignment : due) {
            assignment.setRemindedAt(now);
            examGradingAssignmentRepository.save(assignment);

            var result = resultsById.get(assignment.getCandidateResultId());
            var exam = result == null ? null : examsById.get(result.getExamId());
            var payload = new GradingDeadlineReminderPayloadV1(
                assignment.getId(),
                assignment.getTeacherId(),
                exam == null ? null : exam.getName(),
                assignment.getRoundType() == null ? null : assignment.getRoundType().name(),
                assignment.getDeadlineAt(),
                result == null ? null : result.getExamId(),
                exam == null ? null : exam.getKind()
            );
            outboxRepository.save(Outbox.create(
                AggregateTypeConstant.EXAM_GRADING_ASSIGNMENT,
                assignment.getId(),
                EventTypeConstant.GRADING_DEADLINE_REMINDER,
                jsonSerializationPort.toJson(payload),
                now
            ));
        }
        return due.size();
    }
}
