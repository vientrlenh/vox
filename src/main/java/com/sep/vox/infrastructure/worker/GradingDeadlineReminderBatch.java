package com.sep.vox.infrastructure.worker;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
     * {@code reminded_at} and the outbox row are in ONE transaction, so the write order
     * no longer matters: either both commit, or both are lost and the next batch reminds
     * again. Previously the mail was sent after commit so the marker had to be set first, and the trade-off was that an SMTP
     * error lost the reminder of that assignment for good.
     *
     * @return the number of assignments processed; {@code 0} means the backlog is empty
     */
    @Transactional
    public int remindOnce(Instant threshold) {
        List<ExamGradingAssignment> due = examGradingAssignmentRepository.findDueForReminder(threshold);
        if (due.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now();

        // Load results + exams in bulk: one batch can reach several hundred rows, and calling one
        // row at a time is an N+1 right inside the background job.
        Map<UUID, ExamCandidateResult> resultsById = examCandidateResultRepository.findByIdIn(
                due.stream().map(assignment -> assignment.getCandidateResultId()).distinct().toList()).stream()
            .collect(Collectors.toMap(
                result -> result.getId(), Function.identity(), (left, right) -> left));
        // Keep the Exam as is instead of extracting only its name: the notification also needs the id and the exam kind to open
        // the right grading screen, and this batch is the only place in the flow still holding the Exam in hand.
        Map<UUID, Exam> examsById = examRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getExamId()).distinct().toList()).stream()
            .collect(Collectors.toMap(
                exam -> exam.getId(), Function.identity(), (left, right) -> left));

        for (ExamGradingAssignment assignment : due) {
            assignment.setRemindedAt(now);
            examGradingAssignmentRepository.save(assignment);

            ExamCandidateResult result = resultsById.get(assignment.getCandidateResultId());
            Exam exam = result == null ? null : examsById.get(result.getExamId());
            GradingDeadlineReminderPayloadV1 payload = new GradingDeadlineReminderPayloadV1(
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
