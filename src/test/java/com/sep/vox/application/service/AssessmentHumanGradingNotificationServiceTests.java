package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.AssessmentHumanGradingRequiredPayloadV1;
import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * The manual-grading reminder when an assessment closes while it still has "Awaiting AI score review" answers ({@code PENDING_REVIEW}).
 *
 * <p>Only produces an in-app notification: the event goes on its own topic that the mail consumer does not subscribe to,
 * so there is nothing to assert about mail here beyond that topic choice itself.
 */
class AssessmentHumanGradingNotificationServiceTests {

    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private OutboxRepository outboxRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentHumanGradingNotificationService service;

    private UUID assessmentId;
    private UUID schoolId;
    private final Instant now = Instant.parse("2026-09-01T03:00:00Z");
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        outboxRepository = mock(OutboxRepository.class);

        JsonSerializationPort jsonSerializationPort = mock(JsonSerializationPort.class);
        when(jsonSerializationPort.toJson(any()))
            .thenAnswer(invocation -> jsonMapper.writeValueAsString(invocation.getArgument(0)));

        assessmentRepository = mock(AssessmentRepository.class);

        service = new AssessmentHumanGradingNotificationService(
            assessmentRepository, assessmentCandidateResultRepository, assessmentMemberRepository,
            schoolUserRepository, outboxRepository, jsonSerializationPort);

        assessmentId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
    }

    @Test
    void should_notify_the_class_test_owner_when_papers_await_human_grading() {
        UUID teacherId = UUID.randomUUID();
        givenPendingResults(2);
        givenChairs(teacherId);

        service.publishIfPendingReview(assessment(AssessmentKind.CLASS_TEST), now);

        AssessmentHumanGradingRequiredPayloadV1 payload = capturePayload();
        assertThat(payload.recipientIds()).containsExactly(teacherId);
        assertThat(payload.pendingCount()).isEqualTo(2);
        assertThat(payload.assessmentKind()).isEqualTo(AssessmentKind.CLASS_TEST);
        assertThat(payload.assessmentId()).isEqualTo(assessmentId);
    }

    /** A class test is the job of the teacher who owns it: the school admin is not involved. */
    @Test
    void should_not_notify_school_admins_for_a_class_test() {
        UUID teacherId = UUID.randomUUID();
        givenPendingResults(1);
        givenChairs(teacherId);
        givenSchoolAdmins(UUID.randomUUID(), UUID.randomUUID());

        service.publishIfPendingReview(assessment(AssessmentKind.CLASS_TEST), now);

        assertThat(capturePayload().recipientIds()).containsExactly(teacherId);
        verify(schoolUserRepository, never()).findBySchoolIdWithRole(any(), any());
    }

    @Test
    void should_notify_both_chair_and_school_admins_for_a_centralized_assessment() {
        UUID chairId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        givenPendingResults(5);
        givenChairs(chairId);
        givenSchoolAdmins(adminId);

        service.publishIfPendingReview(assessment(AssessmentKind.CENTRALIZED), now);

        assertThat(capturePayload().recipientIds()).containsExactlyInAnyOrder(chairId, adminId);
    }

    /** A person who is both the chair and a school admin is reminded only once. */
    @Test
    void should_not_repeat_a_recipient_who_is_both_chair_and_school_admin() {
        UUID bothRoles = UUID.randomUUID();
        givenPendingResults(1);
        givenChairs(bothRoles);
        givenSchoolAdmins(bothRoles);

        service.publishIfPendingReview(assessment(AssessmentKind.CENTRALIZED), now);

        assertThat(capturePayload().recipientIds()).containsExactly(bothRoles);
    }

    /** The AI grading a whole assessment cleanly is the normal outcome -- "0 answers to grade" is just noise. */
    @Test
    void should_stay_silent_when_nothing_awaits_human_grading() {
        when(assessmentCandidateResultRepository.findByAssessmentId(assessmentId))
            .thenReturn(List.of(result(AssessmentCandidateResultStatus.RELEASED)));
        givenChairs(UUID.randomUUID());

        service.publishIfPendingReview(assessment(AssessmentKind.CENTRALIZED), now);

        verify(outboxRepository, never()).save(any());
    }

    /** Only counts answers awaiting review, not the whole assessment. */
    @Test
    void should_count_only_the_papers_still_awaiting_review() {
        when(assessmentCandidateResultRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            result(AssessmentCandidateResultStatus.PENDING_REVIEW),
            result(AssessmentCandidateResultStatus.RELEASED),
            result(AssessmentCandidateResultStatus.PENDING_REVIEW),
            result(AssessmentCandidateResultStatus.INVALID)));
        givenChairs(UUID.randomUUID());

        service.publishIfPendingReview(assessment(AssessmentKind.CENTRALIZED), now);

        assertThat(capturePayload().pendingCount()).isEqualTo(2);
    }

    /** With nobody to notify the answers still stay in the grading queue, only no empty event is published. */
    @Test
    void should_not_publish_an_event_with_no_recipients() {
        givenPendingResults(3);
        when(assessmentMemberRepository.findByAssessmentId(assessmentId)).thenReturn(List.of());
        givenSchoolAdmins();

        service.publishIfPendingReview(assessment(AssessmentKind.CENTRALIZED), now);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void should_publish_under_the_assessment_grading_review_event_type() {
        givenPendingResults(1);
        givenChairs(UUID.randomUUID());

        service.publishIfPendingReview(assessment(AssessmentKind.CENTRALIZED), now);

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType())
            .isEqualTo(EventTypeConstant.ASSESSMENT_HUMAN_GRADING_REQUIRED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(assessmentId);
    }

    /**
     * De-duplication guard: the sweep runs every minute and meets the same assessment again on the next
     * tick, and the close-assessment branch calls again for the same assessment. Without this guard, every assessment
     * would be reminded every minute for the whole assessment session.
     */
    @Test
    void should_stay_silent_for_an_assessment_already_notified() {
        givenPendingResults(4);
        givenChairs(UUID.randomUUID());
        Assessment assessment = assessment(AssessmentKind.CENTRALIZED);
        assessment.setHumanGradingNotifiedAt(Instant.parse("2026-08-31T02:00:00Z"));

        service.publishIfPendingReview(assessment, now);

        verify(outboxRepository, never()).save(any());
        verify(assessmentRepository, never()).save(any());
    }

    /**
     * Stamping and publishing the event must happen in the same transaction: without the stamp, the
     * reminder repeats every minute.
     */
    @Test
    void should_stamp_the_assessment_when_it_publishes() {
        givenPendingResults(1);
        givenChairs(UUID.randomUUID());
        Assessment assessment = assessment(AssessmentKind.CENTRALIZED);

        service.publishIfPendingReview(assessment, now);

        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository).save(captor.capture());
        assertThat(captor.getValue().getHumanGradingNotifiedAt()).isEqualTo(now);
    }

    /** If it was not published it must not be stamped either -- the assessment must still have a chance to be reminded later. */
    @Test
    void should_not_stamp_the_assessment_when_nothing_is_published() {
        when(assessmentCandidateResultRepository.findByAssessmentId(assessmentId))
            .thenReturn(List.of(result(AssessmentCandidateResultStatus.RELEASED)));
        givenChairs(UUID.randomUUID());

        service.publishIfPendingReview(assessment(AssessmentKind.CENTRALIZED), now);

        verify(assessmentRepository, never()).save(any());
    }

    // --- helpers ---------------------------------------------------------------

    private Assessment assessment(AssessmentKind kind) {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(kind);
        assessment.setName("Kỳ thi giữa kỳ");
        return assessment;
    }

    private AssessmentCandidateResult result(AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(UUID.randomUUID());
        result.setStatus(status);
        return result;
    }

    private void givenPendingResults(int count) {
        when(assessmentCandidateResultRepository.findByAssessmentId(assessmentId)).thenReturn(
            java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> result(AssessmentCandidateResultStatus.PENDING_REVIEW))
                .toList());
    }

    private void givenChairs(UUID... userIds) {
        when(assessmentMemberRepository.findByAssessmentId(assessmentId)).thenReturn(
            java.util.Arrays.stream(userIds).map(this::chairMember).toList());
    }

    private AssessmentMember chairMember(UUID userId) {
        AssessmentMember member = new AssessmentMember();
        member.setAssessmentId(assessmentId);
        member.setUserId(userId);
        member.setRole(AssessmentMemberRole.CHAIR);
        return member;
    }

    private void givenSchoolAdmins(UUID... userIds) {
        when(schoolUserRepository.findBySchoolIdWithRole(any(), any())).thenReturn(
            java.util.Arrays.stream(userIds).map(this::schoolUser).toList());
    }

    private SchoolUser schoolUser(UUID userId) {
        SchoolUser schoolUser = new SchoolUser();
        schoolUser.setUserId(userId);
        schoolUser.setSchoolId(schoolId);
        return schoolUser;
    }

    private AssessmentHumanGradingRequiredPayloadV1 capturePayload() {
        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        return jsonMapper.readValue(
            captor.getValue().getPayload(), AssessmentHumanGradingRequiredPayloadV1.class);
    }
}
