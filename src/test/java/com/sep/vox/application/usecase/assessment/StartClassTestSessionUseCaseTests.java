package com.sep.vox.application.usecase.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.ServiceUnavailableException;
import com.sep.vox.application.port.input.command.StartClassTestSessionCommand;
import com.sep.vox.application.port.input.usecase.assessment.StartClassTestSessionUseCase;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.assessmentsession.CreateAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.application.port.output.HealthCheckPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessment.AssessmentEntryTicketResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentRequiredStreamType;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentStreamTypePermission;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Bài kiểm tra trên lớp giờ thi trong phòng có giám khảo, nên cổng vào thi phải siết đúng như kỳ
 * thi tập trung: đã điểm danh, đã được xếp ca, ca thi đang trong giờ.
 */
class StartClassTestSessionUseCaseTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();

    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentSessionRepository assessmentSessionRepository;
    private HealthCheckPort healthCheckPort;
    private StartClassTestSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        healthCheckPort = mock(HealthCheckPort.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        useCase = new StartClassTestSessionUseCase(
            assessmentCandidateRepository,
            mock(AssessmentCandidateResultRepository.class),
            assessmentRepository,
            assessmentScheduleRepository,
            assessmentSessionRepository,
            userContextPort,
            mock(CreateAssessmentSessionUseCase.class),
            mock(UpdateAssessmentSessionStatusUseCase.class),
            healthCheckPort,
            mock(SchoolSubscriptionRepository.class),
            mock(SchoolSubscriptionDebtGuardService.class)
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(assessmentCandidateRepository.findByAssessmentIdAndStudentId(ASSESSMENT_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate(AssessmentCandidateStatus.ATTENDED)));
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment(false)));
        when(assessmentScheduleRepository.findByIdAndInSchedule(any(UUID.class), any(Instant.class)))
            .thenReturn(Optional.of(schedule()));
        when(assessmentSessionRepository.findLatestByCandidateIdAndStatuses(any(UUID.class), anyCollection()))
            .thenReturn(Optional.of(inProgressSession()));
    }

    @Test
    void should_issue_entry_ticket_when_candidate_is_attended_and_scheduled() {
        AssessmentEntryTicketResponse result = useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID));

        assertThat(result.attemptId()).isNotNull();
        assertThat(result.scheduleEndAt()).isNotNull();
    }

    /**
     * The assessment app must know whether the test is proctored RIGHT FROM the ticket: if it calls
     * /streams/student/token for a test with no stream configured,
     * {@code IssueStudentStreamTokenUseCase} returns 400 and the student is left standing at the
     * door, even though every entry condition has been met.
     */
    @Test
    void should_expose_stream_config_so_client_knows_a_token_is_needed() {
        Assessment assessment = assessment(false);
        assessment.setRequiredStreamType(AssessmentRequiredStreamType.CAMERA_AND_SCREEN);
        assessment.setStreamTypePermission(AssessmentStreamTypePermission.ANY);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));

        AssessmentEntryTicketResponse result = useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID));

        assertThat(result.requiredStreamType()).isEqualTo("CAMERA_AND_SCREEN");
        assertThat(result.streamTypePermission()).isEqualTo("ANY");
    }

    @Test
    void should_expose_empty_stream_config_when_class_test_is_not_monitored() {
        AssessmentEntryTicketResponse result = useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID));

        assertThat(result.attemptId()).isNotNull();
        assertThat(result.requiredStreamType()).isNull();
        assertThat(result.streamTypePermission()).isNull();
    }

    /**
     * Bài trên lớp bật stream mà tắt OTP vẫn đi qua cổng này, nên cổng này cũng phải hỏi service
     * streaming còn sống không — nếu không thì học sinh vào thi được nhưng không có đoạn ghi giám
     * sát nào, và bài làm coi như không kiểm chứng được.
     */
    @Test
    void should_check_streaming_health_when_class_test_is_monitored() {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(monitoredAssessment()));

        useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID));

        verify(healthCheckPort).checkStreamingOk();
    }

    @Test
    void should_not_check_streaming_health_when_class_test_is_not_monitored() {
        useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID));

        verify(healthCheckPort, never()).checkStreamingOk();
    }

    @Test
    void should_refuse_entry_ticket_when_streaming_service_is_down() {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(monitoredAssessment()));
        doThrow(new ServiceUnavailableException("Streaming service hiện không hoạt động"))
            .when(healthCheckPort).checkStreamingOk();

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID)))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("Streaming service");
    }

    /** A non-proctored test must not be dragged down when streaming is dead. */
    @Test
    void should_still_issue_entry_ticket_for_unmonitored_class_test_when_streaming_is_down() {
        doThrow(new ServiceUnavailableException("Streaming service hiện không hoạt động"))
            .when(healthCheckPort).checkStreamingOk();

        AssessmentEntryTicketResponse result = useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID));

        assertThat(result.attemptId()).isNotNull();
    }

    @Test
    void should_reject_when_candidate_has_not_been_checked_in() {
        when(assessmentCandidateRepository.findByAssessmentIdAndStudentId(ASSESSMENT_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate(AssessmentCandidateStatus.ASSIGNED)));

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bạn chưa được điểm danh có mặt");
    }

    @Test
    void should_reject_when_candidate_is_not_assigned_to_any_schedule() {
        AssessmentCandidate candidate = candidate(AssessmentCandidateStatus.ATTENDED);
        candidate.setScheduleId(null);
        when(assessmentCandidateRepository.findByAssessmentIdAndStudentId(ASSESSMENT_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bạn chưa được xếp ca thi");
    }

    @Test
    void should_reject_when_schedule_is_outside_its_time_window() {
        when(assessmentScheduleRepository.findByIdAndInSchedule(any(UUID.class), any(Instant.class)))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Ca thi không hợp lệ hoặc đã hết hạn");
    }

    /** Giáo viên bật OTP thì học sinh phải đi cổng OTP, không được vào thẳng qua cổng này. */
    @Test
    void should_redirect_to_otp_flow_when_class_test_requires_otp() {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment(true)));

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("yêu cầu xác thực OTP");
    }

    @Test
    void should_reject_when_candidate_was_blocked() {
        AssessmentCandidate candidate = candidate(AssessmentCandidateStatus.ATTENDED);
        candidate.setBlockedAt(Instant.now());
        when(assessmentCandidateRepository.findByAssessmentIdAndStudentId(ASSESSMENT_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(ASSESSMENT_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bạn đã bị buộc kết thúc bài thi này");
    }

    private AssessmentCandidate candidate(AssessmentCandidateStatus status) {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(CANDIDATE_ID);
        candidate.setAssessmentId(ASSESSMENT_ID);
        candidate.setStudentId(STUDENT_ID);
        candidate.setScheduleId(SCHEDULE_ID);
        candidate.setAssignedPaperId(PAPER_ID);
        candidate.setStatus(status);
        return candidate;
    }

    private Assessment assessment(boolean requiresOtp) {
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setKind(AssessmentKind.CLASS_TEST);
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        assessment.setRequiresOtp(requiresOtp);
        assessment.setMaxAttempt(1);
        assessment.setCloseAt(Instant.now().plusSeconds(3600));
        return assessment;
    }

    private Assessment monitoredAssessment() {
        Assessment assessment = assessment(false);
        assessment.setRequiredStreamType(AssessmentRequiredStreamType.CAMERA);
        assessment.setStreamTypePermission(AssessmentStreamTypePermission.ANY);
        return assessment;
    }

    private AssessmentSchedule schedule() {
        AssessmentSchedule schedule = new AssessmentSchedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setAssessmentId(ASSESSMENT_ID);
        schedule.setStartDate(Instant.now().minusSeconds(600));
        schedule.setEndDate(Instant.now().plusSeconds(3000));
        return schedule;
    }

    private AssessmentSession inProgressSession() {
        AssessmentSession session = new AssessmentSession();
        session.setId(UUID.randomUUID());
        session.setAssessmentId(ASSESSMENT_ID);
        session.setCandidateId(CANDIDATE_ID);
        session.setStatus(AssessmentSessionStatus.IN_PROGRESS);
        return session;
    }
}
