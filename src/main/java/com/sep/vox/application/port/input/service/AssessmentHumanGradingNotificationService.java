package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.event.AssessmentHumanGradingRequiredPayloadV1;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Tells the person in charge that an assessment has just closed with attempts still in "Waiting for AI score review"
 * ({@code PENDING_REVIEW}) that need manual grading.
 *
 * <p>Reminds RIGHT DURING THE ASSESSMENT, without waiting for it to close: a session that ends early already has results, and
 * making the grader wait until closing wastes most of the time available for grading. The periodic
 * sweep ({@code AssessmentHumanGradingNoticeBatch}) is the main path; the two closing branches still call it
 * again as a safety net for an assessment that closes before the sweep gets to run.
 *
 * <p>That is why {@code humanGradingNotifiedAt} is the part that CANNOT be dropped: the trigger is no longer a
 * single status transition but a sweep every minute, while the number of attempts waiting for grading
 * only keeps growing throughout the session. Without that column every assessment would be reminded again every minute until it closes.
 *
 * <p>In return, the number in the notification is the number AT THE TIME OF THE REMINDER, not the final total: attempts graded
 * afterwards do not produce a new notification. This is a deliberate choice -- the recipient needs to know "there is work, go
 * in", and the exact number is always shown by the grading queue anyway.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentHumanGradingNotificationService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    /**
     * {@code REQUIRED} and not {@code MANDATORY}: the two callers are different in nature --
     * {@code UpdateAssessmentStatusUseCase} is already inside its own transaction, while
     * {@code AssessmentStatusAutoTransitionJob} only has {@code @Scheduled} and so carries no
     * transaction. MANDATORY would throw straight away on the second path, which is the path that closes most
     * class tests.
     */
    @Transactional
    public void publishIfPendingReview(Assessment assessment, Instant now) {
        if (assessment == null || isAlreadyNotified(assessment)) {
            return;
        }

        long pendingCount = countPendingReview(assessment);

        // No attempt waiting for grading is a NORMAL outcome, not an error: if the AI graded the whole
        // session, nobody has anything to do, and a "0 attempts to grade" notification is only noise.
        if (pendingCount == 0) {
            return;
        }

        List<UUID> recipientIds = recipientsOf(assessment);
        if (recipientIds.isEmpty()) {
            warnNoRecipients(assessment, pendingCount);
            return;
        }

        publishAndStamp(assessment, recipientIds, (int) pendingCount, now);
    }

    private void warnNoRecipients(Assessment assessment, long pendingCount) {
        // A class test without a CHAIR, or a school with no active admin left.
        // The attempts stay in the grading queue, there is just nobody to alert.
        log.warn("Assessment {} has {} submissions awaiting manual grading but no notification recipient could be found",
            assessment.getId(), pendingCount);
    }

    /**
     * Already notified: stop. This is the de-duplication guard for BOTH entry points: the periodic sweep
     * meets the same assessment again on the next tick, and the closing branch calls again for the same assessment.
     */
    private boolean isAlreadyNotified(Assessment assessment) {
        return assessment.getHumanGradingNotifiedAt() != null;
    }

    private long countPendingReview(Assessment assessment) {
        return assessmentCandidateResultRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(result -> result.getStatus() == AssessmentCandidateResultStatus.PENDING_REVIEW)
            .count();
    }

    private void publishAndStamp(Assessment assessment, List<UUID> recipientIds, int pendingCount, Instant now) {
        String payload = jsonSerializationPort.toJson(new AssessmentHumanGradingRequiredPayloadV1(
            recipientIds,
            assessment.getId(),
            assessment.getName(),
            assessment.getKind(),
            pendingCount
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT, assessment.getId(),
            EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED, payload, now
        ));

        // Stamp in the SAME transaction as the outbox row: the two must live or die together.
        // Stamping without publishing is silence forever; publishing without stamping is a reminder every minute.
        assessment.setHumanGradingNotifiedAt(now);
        assessmentRepository.save(assessment);

        log.info("Assessment {} has {} submissions awaiting manual grading -- notifying {} people",
            assessment.getId(), pendingCount, recipientIds.size());
    }

    /**
     * Class test: the CHAIR is the teacher who made the paper, and also the only one who grades it -- see
     * {@code ClassTestGradingAssignmentService.findClassTestChairId}, where every assignment is
     * opened under their name.
     *
     * <p>Centralized assessment: the CHAIR (chairperson of the council, responsible for the assessment) PLUS
     * school admin (who actually hands out the grading work through AssignGradingUseCase). Both are merged because
     * missing either leaves a case where nobody is notified: an assessment with no chair assigned, or a chair who left.
     * {@code distinct} because one person may be both chair and school admin.
     */
    private List<UUID> recipientsOf(Assessment assessment) {
        Stream<UUID> chairIds = assessmentMemberRepository.findByAssessmentId(assessment.getId()).stream()
            .filter(member -> member.getRole() == AssessmentMemberRole.CHAIR)
            .map(member -> member.getUserId());

        if (assessment.getKind() == AssessmentKind.CLASS_TEST) {
            return chairIds.filter(userId -> userId != null).distinct().toList();
        }

        Stream<UUID> schoolAdminIds = schoolUserRepository
            .findBySchoolIdWithRole(assessment.getSchoolId(), RoleConstant.SCHOOL_ADMIN_ROLE)
            .stream()
            .map(schoolUser -> schoolUser.getUserId());

        return Stream.concat(chairIds, schoolAdminIds)
            .filter(userId -> userId != null)
            .distinct()
            .toList();
    }
}
