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

import com.sep.vox.application.event.ExamHumanGradingRequiredPayloadV1;
import com.sep.vox.application.port.input.service.ExamHumanGradingNotificationService;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMember;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Nhắc chấm tay khi bài thi đóng mà còn bài "Chờ soát điểm AI" ({@code PENDING_REVIEW}).
 *
 * <p>Chỉ sinh thông báo trong app: event đi trên topic riêng mà consumer mail không đăng ký,
 * nên ở đây không có gì để khẳng định về mail ngoài chính lựa chọn topic đó.
 */
class ExamHumanGradingNotificationServiceTests {

    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamMemberRepository examMemberRepository;
    private SchoolUserRepository schoolUserRepository;
    private OutboxRepository outboxRepository;
    private ExamRepository examRepository;
    private ExamHumanGradingNotificationService service;

    private UUID examId;
    private UUID schoolId;
    private final Instant now = Instant.parse("2026-09-01T03:00:00Z");
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        outboxRepository = mock(OutboxRepository.class);

        JsonSerializationPort jsonSerializationPort = mock(JsonSerializationPort.class);
        when(jsonSerializationPort.toJson(any()))
            .thenAnswer(invocation -> jsonMapper.writeValueAsString(invocation.getArgument(0)));

        examRepository = mock(ExamRepository.class);

        service = new ExamHumanGradingNotificationService(
            examRepository, examCandidateResultRepository, examMemberRepository,
            schoolUserRepository, outboxRepository, jsonSerializationPort);

        examId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
    }

    @Test
    void should_notify_the_class_test_owner_when_papers_await_human_grading() {
        var teacherId = UUID.randomUUID();
        givenPendingResults(2);
        givenChairs(teacherId);

        service.publishIfPendingReview(exam(ExamKind.CLASS_TEST), now);

        var payload = capturePayload();
        assertThat(payload.recipientIds()).containsExactly(teacherId);
        assertThat(payload.pendingCount()).isEqualTo(2);
        assertThat(payload.examKind()).isEqualTo(ExamKind.CLASS_TEST);
        assertThat(payload.examId()).isEqualTo(examId);
    }

    /** Bài trên lớp là việc của giáo viên chủ bài: school admin không dính vào. */
    @Test
    void should_not_notify_school_admins_for_a_class_test() {
        var teacherId = UUID.randomUUID();
        givenPendingResults(1);
        givenChairs(teacherId);
        givenSchoolAdmins(UUID.randomUUID(), UUID.randomUUID());

        service.publishIfPendingReview(exam(ExamKind.CLASS_TEST), now);

        assertThat(capturePayload().recipientIds()).containsExactly(teacherId);
        verify(schoolUserRepository, never()).findBySchoolIdWithRole(any(), any());
    }

    @Test
    void should_notify_both_chair_and_school_admins_for_a_centralized_exam() {
        var chairId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        givenPendingResults(5);
        givenChairs(chairId);
        givenSchoolAdmins(adminId);

        service.publishIfPendingReview(exam(ExamKind.CENTRALIZED), now);

        assertThat(capturePayload().recipientIds()).containsExactlyInAnyOrder(chairId, adminId);
    }

    /** Một người vừa là chủ tịch vừa là school admin chỉ được nhắc một lần. */
    @Test
    void should_not_repeat_a_recipient_who_is_both_chair_and_school_admin() {
        var bothRoles = UUID.randomUUID();
        givenPendingResults(1);
        givenChairs(bothRoles);
        givenSchoolAdmins(bothRoles);

        service.publishIfPendingReview(exam(ExamKind.CENTRALIZED), now);

        assertThat(capturePayload().recipientIds()).containsExactly(bothRoles);
    }

    /** AI chấm sạch cả kỳ là kết cục bình thường -- "0 bài cần chấm" chỉ là nhiễu. */
    @Test
    void should_stay_silent_when_nothing_awaits_human_grading() {
        when(examCandidateResultRepository.findByExamId(examId))
            .thenReturn(List.of(result(ExamCandidateResultStatus.RELEASED)));
        givenChairs(UUID.randomUUID());

        service.publishIfPendingReview(exam(ExamKind.CENTRALIZED), now);

        verify(outboxRepository, never()).save(any());
    }

    /** Chỉ đếm bài đang chờ soát, không đếm cả kỳ. */
    @Test
    void should_count_only_the_papers_still_awaiting_review() {
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(List.of(
            result(ExamCandidateResultStatus.PENDING_REVIEW),
            result(ExamCandidateResultStatus.RELEASED),
            result(ExamCandidateResultStatus.PENDING_REVIEW),
            result(ExamCandidateResultStatus.INVALID)));
        givenChairs(UUID.randomUUID());

        service.publishIfPendingReview(exam(ExamKind.CENTRALIZED), now);

        assertThat(capturePayload().pendingCount()).isEqualTo(2);
    }

    /** Không có ai để báo thì bài vẫn nằm trong hàng đợi chấm, chỉ là không phát event rỗng. */
    @Test
    void should_not_publish_an_event_with_no_recipients() {
        givenPendingResults(3);
        when(examMemberRepository.findByExamId(examId)).thenReturn(List.of());
        givenSchoolAdmins();

        service.publishIfPendingReview(exam(ExamKind.CENTRALIZED), now);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void should_publish_under_the_exam_grading_review_event_type() {
        givenPendingResults(1);
        givenChairs(UUID.randomUUID());

        service.publishIfPendingReview(exam(ExamKind.CENTRALIZED), now);

        var captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType())
            .isEqualTo(EventTypeConstant.EXAM_HUMAN_GRADING_REQUIRED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(examId);
    }

    /**
     * Chốt chống trùng: lượt quét chạy mỗi phút và gặp lại đúng bài thi đó ở tick sau, còn nhánh
     * đóng bài thì gọi lại lần nữa cho cùng bài. Thiếu chốt này thì mỗi bài thi bị nhắc mỗi phút
     * suốt buổi thi.
     */
    @Test
    void should_stay_silent_for_an_exam_already_notified() {
        givenPendingResults(4);
        givenChairs(UUID.randomUUID());
        var exam = exam(ExamKind.CENTRALIZED);
        exam.setHumanGradingNotifiedAt(Instant.parse("2026-08-31T02:00:00Z"));

        service.publishIfPendingReview(exam, now);

        verify(outboxRepository, never()).save(any());
        verify(examRepository, never()).save(any());
    }

    /** Đóng dấu và phát event phải cùng một transaction: thiếu dấu là nhắc lại mỗi phút. */
    @Test
    void should_stamp_the_exam_when_it_publishes() {
        givenPendingResults(1);
        givenChairs(UUID.randomUUID());
        var exam = exam(ExamKind.CENTRALIZED);

        service.publishIfPendingReview(exam, now);

        var captor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(captor.capture());
        assertThat(captor.getValue().getHumanGradingNotifiedAt()).isEqualTo(now);
    }

    /** Không phát thì cũng không được đóng dấu -- bài thi phải còn cơ hội được nhắc sau. */
    @Test
    void should_not_stamp_the_exam_when_nothing_is_published() {
        when(examCandidateResultRepository.findByExamId(examId))
            .thenReturn(List.of(result(ExamCandidateResultStatus.RELEASED)));
        givenChairs(UUID.randomUUID());

        service.publishIfPendingReview(exam(ExamKind.CENTRALIZED), now);

        verify(examRepository, never()).save(any());
    }

    // --- helpers ---------------------------------------------------------------

    private Exam exam(ExamKind kind) {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        exam.setKind(kind);
        exam.setName("Kỳ thi giữa kỳ");
        return exam;
    }

    private ExamCandidateResult result(ExamCandidateResultStatus status) {
        var result = new ExamCandidateResult();
        result.setId(UUID.randomUUID());
        result.setStatus(status);
        return result;
    }

    private void givenPendingResults(int count) {
        when(examCandidateResultRepository.findByExamId(examId)).thenReturn(
            java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> result(ExamCandidateResultStatus.PENDING_REVIEW))
                .toList());
    }

    private void givenChairs(UUID... userIds) {
        when(examMemberRepository.findByExamId(examId)).thenReturn(
            java.util.Arrays.stream(userIds).map(this::chairMember).toList());
    }

    private ExamMember chairMember(UUID userId) {
        var member = new ExamMember();
        member.setExamId(examId);
        member.setUserId(userId);
        member.setRole(ExamMemberRole.CHAIR);
        return member;
    }

    private void givenSchoolAdmins(UUID... userIds) {
        when(schoolUserRepository.findBySchoolIdWithRole(any(), any())).thenReturn(
            java.util.Arrays.stream(userIds).map(this::schoolUser).toList());
    }

    private SchoolUser schoolUser(UUID userId) {
        var schoolUser = new SchoolUser();
        schoolUser.setUserId(userId);
        schoolUser.setSchoolId(schoolId);
        return schoolUser;
    }

    private ExamHumanGradingRequiredPayloadV1 capturePayload() {
        var captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        return jsonMapper.readValue(
            captor.getValue().getPayload(), ExamHumanGradingRequiredPayloadV1.class);
    }
}
