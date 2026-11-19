package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.GradingAssignmentOpenedPayloadV1;
import com.sep.vox.application.port.input.service.GradingAssignmentNotificationService;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.FullName;

import tools.jackson.databind.json.JsonMapper;

/**
 * Báo cho giáo viên vừa được giao một vòng chấm.
 *
 * <p>Phần đáng giá nhất ở đây là luật ẩn danh: kỳ thi tập trung chấm mù, nên tên học sinh
 * không được lọt vào thông báo -- mà thông báo thì nằm lại vĩnh viễn trong cột payload.
 */
class GradingAssignmentNotificationServiceTests {

    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamRepository examRepository;
    private UserRepository userRepository;
    private OutboxRepository outboxRepository;
    private GradingAssignmentNotificationService service;

    private final Instant now = Instant.parse("2026-09-01T03:00:00Z");
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private UUID examId;
    private UUID candidateResultId;
    private UUID candidateId;
    private UUID studentId;
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examRepository = mock(ExamRepository.class);
        userRepository = mock(UserRepository.class);
        outboxRepository = mock(OutboxRepository.class);

        JsonSerializationPort jsonSerializationPort = mock(JsonSerializationPort.class);
        when(jsonSerializationPort.toJson(any()))
            .thenAnswer(call -> jsonMapper.writeValueAsString(call.getArgument(0)));

        service = new GradingAssignmentNotificationService(
            examCandidateResultRepository, examCandidateRepository, examRepository,
            userRepository, outboxRepository, jsonSerializationPort);

        examId = UUID.randomUUID();
        candidateResultId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
    }

    @Test
    void should_notify_the_assigned_teacher_with_the_round_and_exam() {
        given(ExamKind.CLASS_TEST, "Trần Quang Thiên");

        service.publishAssigned(List.of(assignment(GradingRoundType.SPOT_CHECK)), now);

        GradingAssignmentOpenedPayloadV1 payload = capturePayload();
        assertThat(payload.teacherId()).isEqualTo(teacherId);
        assertThat(payload.examId()).isEqualTo(examId);
        assertThat(payload.roundType()).isEqualTo("SPOT_CHECK");
        assertThat(payload.examName()).isEqualTo("Kiểm tra 15 phút");
    }

    /** Bài kiểm tra trên lớp không chấm mù: giáo viên vốn đã thấy tên ở hàng đợi chấm. */
    @Test
    void should_name_the_candidate_for_a_class_test() {
        given(ExamKind.CLASS_TEST, "Trần Quang Thiên");

        service.publishAssigned(List.of(assignment(GradingRoundType.INITIAL)), now);

        assertThat(capturePayload().candidateLabel()).isEqualTo("Trần Quang Thiên");
    }

    /**
     * The most important case of this class. A centralized exam is graded BLIND -- see
     * GradingStudentIdentityQueryTests, where studentName must be null. The notification goes to
     * exactly the person grading, so leaking the name here breaks that mechanism outright, and the
     * payload is stored permanently.
     */
    @Test
    void should_never_leak_the_candidate_name_for_a_centralized_exam() {
        given(ExamKind.CENTRALIZED, "Trần Quang Thiên");

        service.publishAssigned(List.of(assignment(GradingRoundType.SPOT_CHECK)), now);

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        String rawPayload = captor.getValue().getPayload();

        assertThat(rawPayload).doesNotContain("Trần Quang Thiên");
        GradingAssignmentOpenedPayloadV1 payload = jsonMapper.readValue(rawPayload, GradingAssignmentOpenedPayloadV1.class);
        // The 8-character hex submission code is the only thing that identifies a submission on the
        // anonymous screen.
        assertThat(payload.candidateLabel())
            .isEqualTo(candidateResultId.toString().replace("-", "").substring(0, 8).toUpperCase());
    }

    /** Không tra tên cho kỳ thi tập trung: thứ không nạp lên thì không lọt vào payload được. */
    @Test
    void should_not_even_look_up_names_for_a_centralized_exam() {
        given(ExamKind.CENTRALIZED, "Trần Quang Thiên");

        service.publishAssigned(List.of(assignment(GradingRoundType.INITIAL)), now);

        verify(examCandidateRepository, never()).findByIdIn(any());
        verify(userRepository, never()).findByIdIn(any());
    }

    /** Học sinh không tra được tên vẫn phải nhận diện được bài -- lui về mã bài. */
    @Test
    void should_fall_back_to_the_result_code_when_the_name_is_missing() {
        given(ExamKind.CLASS_TEST, null);

        service.publishAssigned(List.of(assignment(GradingRoundType.INITIAL)), now);

        assertThat(capturePayload().candidateLabel())
            .isEqualTo(candidateResultId.toString().replace("-", "").substring(0, 8).toUpperCase());
    }

    /**
     * Auto-assign spreads a whole class in a single call: each assignment gets its own
     * notification.
     */
    @Test
    void should_publish_one_event_per_assignment() {
        given(ExamKind.CLASS_TEST, "Trần Quang Thiên");
        UUID second = UUID.randomUUID();
        when(examCandidateResultRepository.findByIdIn(any()))
            .thenReturn(List.of(result(candidateResultId), result(second)));

        service.publishAssigned(
            List.of(assignment(GradingRoundType.INITIAL), assignmentFor(second)), now);

        verify(outboxRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void should_publish_nothing_for_an_empty_batch() {
        service.publishAssigned(List.of(), now);
        service.publishAssigned(null, now);

        verify(outboxRepository, never()).save(any());
    }

    // --- helpers ---------------------------------------------------------------

    private void given(ExamKind kind, String studentName) {
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setKind(kind);
        exam.setName(kind == ExamKind.CLASS_TEST ? "Kiểm tra 15 phút" : "Kỳ thi giữa kỳ");

        when(examCandidateResultRepository.findByIdIn(any()))
            .thenReturn(List.of(result(candidateResultId)));
        when(examRepository.findByIdIn(any())).thenReturn(List.of(exam));

        ExamCandidate candidate = new ExamCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        when(examCandidateRepository.findByIdIn(any())).thenReturn(List.of(candidate));

        if (studentName != null) {
            User user = new User();
            user.setId(studentId);
            user.setFullName(new FullName(studentName));
            when(userRepository.findByIdIn(any())).thenReturn(List.of(user));
        } else {
            when(userRepository.findByIdIn(any())).thenReturn(List.of());
        }
    }

    private ExamCandidateResult result(UUID id) {
        ExamCandidateResult result = new ExamCandidateResult();
        result.setId(id);
        result.setExamId(examId);
        result.setCandidateId(candidateId);
        return result;
    }

    private ExamGradingAssignment assignment(GradingRoundType roundType) {
        return ExamGradingAssignment.open(
            candidateResultId, teacherId, roundType, null, BigDecimal.ZERO,
            now, UUID.randomUUID(), null);
    }

    private ExamGradingAssignment assignmentFor(UUID resultId) {
        return ExamGradingAssignment.open(
            resultId, teacherId, GradingRoundType.INITIAL, null, BigDecimal.ZERO,
            now, UUID.randomUUID(), null);
    }

    private GradingAssignmentOpenedPayloadV1 capturePayload() {
        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        return jsonMapper.readValue(
            captor.getValue().getPayload(), GradingAssignmentOpenedPayloadV1.class);
    }
}
