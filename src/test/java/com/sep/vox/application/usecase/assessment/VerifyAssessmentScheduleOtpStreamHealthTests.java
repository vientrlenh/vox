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

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.exception.ServiceUnavailableException;
import com.sep.vox.application.port.input.command.assessment.VerifyAssessmentScheduleOtpCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.assessment.VerifyAssessmentScheduleOtpUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.CreateAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
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
 * An assessment proctored by stream must know the streaming service is alive BEFORE the entry ticket is issued:
 * if the student gets in and the service dies afterwards, the attempt keeps running but no proctoring data is
 * recorded, and there is then no way to tell which student cheated.
 *
 * <p>This check does NOT distinguish a centralized assessment from a class test -- a class test can also turn
 * streaming on, so any assessment with a {@code requiredStreamType} must be checked.
 */
class VerifyAssessmentScheduleOtpStreamHealthTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final String OTP = "123456";

    private AssessmentRepository assessmentRepository;
    private HealthCheckPort healthCheckPort;
    private VerifyAssessmentScheduleOtpUseCase useCase;

    @BeforeEach
    void setUp() {
        AssessmentCandidateRepository assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        AssessmentScheduleRepository assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        AssessmentSessionRepository assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        CacheManagerPort cacheManagerPort = mock(CacheManagerPort.class);
        UserContextPort userContextPort = mock(UserContextPort.class);
        healthCheckPort = mock(HealthCheckPort.class);

        useCase = new VerifyAssessmentScheduleOtpUseCase(
            assessmentCandidateRepository,
            mock(AssessmentCandidateResultRepository.class),
            assessmentRepository,
            assessmentScheduleRepository,
            assessmentSessionRepository,
            cacheManagerPort,
            userContextPort,
            mock(CreateAssessmentSessionUseCase.class),
            mock(UpdateAssessmentSessionStatusUseCase.class),
            healthCheckPort,
            mock(SchoolSubscriptionRepository.class),
            mock(SchoolSubscriptionDebtGuardService.class)
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(assessmentCandidateRepository.findByAssessmentIdAndStudentId(ASSESSMENT_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate()));
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment(AssessmentKind.CENTRALIZED, null)));
        when(assessmentScheduleRepository.findByIdAndInSchedule(any(UUID.class), any(Instant.class)))
            .thenReturn(Optional.of(schedule()));
        when(cacheManagerPort.get(CacheKey.assessmentScheduleOtpKey(SCHEDULE_ID))).thenReturn(OTP);
        when(assessmentSessionRepository.findLatestByCandidateIdAndStatuses(any(UUID.class), anyCollection()))
            .thenReturn(Optional.of(inProgressSession()));
    }

    @Test
    void should_check_streaming_health_when_centralized_assessment_is_monitored() {
        when(assessmentRepository.findById(ASSESSMENT_ID))
            .thenReturn(Optional.of(assessment(AssessmentKind.CENTRALIZED, AssessmentRequiredStreamType.CAMERA_AND_SCREEN)));

        useCase.execute(new VerifyAssessmentScheduleOtpCommand(ASSESSMENT_ID, OTP));

        verify(healthCheckPort).checkStreamingOk();
    }

    /** This is why the {@code kind == CENTRALIZED} condition was dropped: a class test can turn streaming on too. */
    @Test
    void should_check_streaming_health_when_class_test_is_monitored() {
        when(assessmentRepository.findById(ASSESSMENT_ID))
            .thenReturn(Optional.of(assessment(AssessmentKind.CLASS_TEST, AssessmentRequiredStreamType.CAMERA)));

        useCase.execute(new VerifyAssessmentScheduleOtpCommand(ASSESSMENT_ID, OTP));

        verify(healthCheckPort).checkStreamingOk();
    }

    @Test
    void should_not_check_streaming_health_when_assessment_is_not_monitored() {
        useCase.execute(new VerifyAssessmentScheduleOtpCommand(ASSESSMENT_ID, OTP));

        verify(healthCheckPort, never()).checkStreamingOk();
    }

    @Test
    void should_refuse_entry_ticket_when_streaming_service_is_down() {
        when(assessmentRepository.findById(ASSESSMENT_ID))
            .thenReturn(Optional.of(assessment(AssessmentKind.CLASS_TEST, AssessmentRequiredStreamType.SCREEN)));
        doThrow(new ServiceUnavailableException("Streaming service is not running"))
            .when(healthCheckPort).checkStreamingOk();

        assertThatThrownBy(() -> useCase.execute(new VerifyAssessmentScheduleOtpCommand(ASSESSMENT_ID, OTP)))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("Streaming service");
    }

    /**
     * A non-proctored test must still issue its ticket normally and must not be dragged down when
     * streaming is dead.
     */
    @Test
    void should_still_issue_entry_ticket_for_unmonitored_assessment_when_streaming_is_down() {
        doThrow(new ServiceUnavailableException("Streaming service is not running"))
            .when(healthCheckPort).checkStreamingOk();

        AssessmentEntryTicketResponse result = useCase.execute(new VerifyAssessmentScheduleOtpCommand(ASSESSMENT_ID, OTP));

        assertThat(result.attemptId()).isNotNull();
        assertThat(result.requiredStreamType()).isNull();
    }

    private AssessmentCandidate candidate() {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(CANDIDATE_ID);
        candidate.setAssessmentId(ASSESSMENT_ID);
        candidate.setStudentId(STUDENT_ID);
        candidate.setScheduleId(SCHEDULE_ID);
        candidate.setAssignedPaperId(PAPER_ID);
        candidate.setStatus(AssessmentCandidateStatus.ATTENDED);
        return candidate;
    }

    private Assessment assessment(AssessmentKind kind, AssessmentRequiredStreamType streamType) {
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setKind(kind);
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        assessment.setRequiresOtp(true);
        assessment.setMaxAttempt(1);
        assessment.setCloseAt(Instant.now().plusSeconds(3600));
        if (streamType != null) {
            assessment.setRequiredStreamType(streamType);
            assessment.setStreamTypePermission(AssessmentStreamTypePermission.ANY);
        }
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
