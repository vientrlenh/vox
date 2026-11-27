package com.sep.vox.infrastructure.worker;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.GradingDeadlineReminderPayloadV1;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * ONE batch of deadline reminders, in ONE transaction.
 *
 * <p>Split from {@link GradingDeadlineReminderJob} because the transaction boundary must sit at
 * each batch and not at the whole run: {@code findDueForReminder} has a {@code LIMIT} so that one
 * transaction does not take thousands of rows, so clearing the whole backlog needs many
 * batches -- and many batches in one transaction is exactly the same as having no LIMIT. Same mould as
 * {@code ImportJobDispatcher} + {@code ImportCommitService}.
 *
 * <p>This is also why the split is mandatory and not cosmetic: calling a {@code @Transactional}
 * method of one's own class directly goes around the proxy and loses the
 * transaction -- and without a transaction the {@code FOR UPDATE SKIP LOCKED} in the select
 * no longer holds its lock until commit, losing the protection against duplicates across instances.
 */
@Component
@RequiredArgsConstructor
public class GradingDeadlineReminderBatch {

    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

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
        List<AssessmentGradingAssignment> due = assessmentGradingAssignmentRepository.findDueForReminder(threshold);
        if (due.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now();

        Map<UUID, AssessmentCandidateResult> resultsById = loadResultsById(due);
        Map<UUID, Assessment> assessmentsById = loadAssessmentsById(resultsById);

        for (AssessmentGradingAssignment assignment : due) {
            remind(assignment, resultsById, assessmentsById, now);
        }
        return due.size();
    }

    // Load results + assessments in bulk: one batch can reach several hundred rows, and calling one
    // row at a time is an N+1 right inside the background job.
    private Map<UUID, AssessmentCandidateResult> loadResultsById(List<AssessmentGradingAssignment> due) {
        return assessmentCandidateResultRepository.findByIdIn(
                due.stream().map(assignment -> assignment.getCandidateResultId()).distinct().toList()).stream()
            .collect(Collectors.toMap(
                result -> result.getId(), Function.identity(), (left, right) -> left));
    }

    // Keep the Assessment as is instead of extracting only its name: the notification also needs the id and the assessment kind to open
    // the right grading screen, and this batch is the only place in the flow still holding the Assessment in hand.
    private Map<UUID, Assessment> loadAssessmentsById(Map<UUID, AssessmentCandidateResult> resultsById) {
        return assessmentRepository.findByIdIn(
                resultsById.values().stream().map(result -> result.getAssessmentId()).distinct().toList()).stream()
            .collect(Collectors.toMap(
                assessment -> assessment.getId(), Function.identity(), (left, right) -> left));
    }

    private void remind(AssessmentGradingAssignment assignment,
            Map<UUID, AssessmentCandidateResult> resultsById, Map<UUID, Assessment> assessmentsById, Instant now) {
        assignment.setRemindedAt(now);
        assessmentGradingAssignmentRepository.save(assignment);

        AssessmentCandidateResult result = resultsById.get(assignment.getCandidateResultId());
        Assessment assessment = result == null ? null : assessmentsById.get(result.getAssessmentId());
        GradingDeadlineReminderPayloadV1 payload = new GradingDeadlineReminderPayloadV1(
            assignment.getId(),
            assignment.getTeacherId(),
            assessment == null ? null : assessment.getName(),
            assignment.getRoundType() == null ? null : assignment.getRoundType().name(),
            assignment.getDeadlineAt(),
            result == null ? null : result.getAssessmentId(),
            assessment == null ? null : assessment.getKind()
        );
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_GRADING_ASSIGNMENT,
            assignment.getId(),
            EventTypeConstant.GRADING_DEADLINE_REMINDER,
            jsonSerializationPort.toJson(payload),
            now
        ));
    }
}
