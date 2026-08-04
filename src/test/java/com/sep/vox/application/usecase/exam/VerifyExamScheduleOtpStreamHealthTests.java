package com.sep.vox.application.usecase.exam;

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
import com.sep.vox.application.port.input.command.VerifyExamScheduleOtpCommand;
import com.sep.vox.application.port.input.usecase.exam.VerifyExamScheduleOtpUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.HealthCheckPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamRequiredStreamType;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ExamStreamTypePermission;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Bài có giám sát bằng stream thì phải biết service streaming còn sống TRƯỚC khi phát vé vào thi:
 * nếu để học sinh vào rồi service mới chết thì bài làm vẫn chạy nhưng không có dữ liệu giám sát nào
 * được ghi, và lúc đó không còn cách nào phân biệt được em nào gian lận.
 *
 * <p>Kiểm tra này KHÔNG phân biệt kỳ thi tập trung hay bài kiểm tra trên lớp — class test cũng có
 * tuỳ chọn bật stream, nên chỉ cần bài có {@code requiredStreamType} là phải kiểm tra.
 */
class VerifyExamScheduleOtpStreamHealthTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final String OTP = "123456";

    private ExamRepository examRepository;
    private HealthCheckPort healthCheckPort;
    private VerifyExamScheduleOtpUseCase useCase;

    @BeforeEach
    void setUp() {
        var examCandidateRepository = mock(ExamCandidateRepository.class);
        examRepository = mock(ExamRepository.class);
        var examScheduleRepository = mock(ExamScheduleRepository.class);
        var examSessionRepository = mock(ExamSessionRepository.class);
        var cacheManagerPort = mock(CacheManagerPort.class);
        var userContextPort = mock(UserContextPort.class);
        healthCheckPort = mock(HealthCheckPort.class);

        useCase = new VerifyExamScheduleOtpUseCase(
            examCandidateRepository,
            mock(ExamCandidateResultRepository.class),
            examRepository,
            examScheduleRepository,
            examSessionRepository,
            cacheManagerPort,
            userContextPort,
            mock(CreateExamSessionUseCase.class),
            mock(UpdateExamSessionStatusUseCase.class),
            healthCheckPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(examCandidateRepository.findByExamIdAndStudentId(EXAM_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate()));
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(ExamKind.CENTRALIZED, null)));
        when(examScheduleRepository.findByIdAndInSchedule(any(UUID.class), any(Instant.class)))
            .thenReturn(Optional.of(schedule()));
        when(cacheManagerPort.get(CacheKey.examScheduleOtpKey(SCHEDULE_ID))).thenReturn(OTP);
        when(examSessionRepository.findLatestByCandidateIdAndStatuses(any(UUID.class), anyCollection()))
            .thenReturn(Optional.of(inProgressSession()));
    }

    @Test
    void should_check_streaming_health_when_centralized_exam_is_monitored() {
        when(examRepository.findById(EXAM_ID))
            .thenReturn(Optional.of(exam(ExamKind.CENTRALIZED, ExamRequiredStreamType.CAMERA_AND_SCREEN)));

        useCase.execute(new VerifyExamScheduleOtpCommand(EXAM_ID, OTP));

        verify(healthCheckPort).checkStreamingOk();
    }

    /** Đây là lý do bỏ điều kiện {@code kind == CENTRALIZED}: class test cũng bật stream được. */
    @Test
    void should_check_streaming_health_when_class_test_is_monitored() {
        when(examRepository.findById(EXAM_ID))
            .thenReturn(Optional.of(exam(ExamKind.CLASS_TEST, ExamRequiredStreamType.CAMERA)));

        useCase.execute(new VerifyExamScheduleOtpCommand(EXAM_ID, OTP));

        verify(healthCheckPort).checkStreamingOk();
    }

    @Test
    void should_not_check_streaming_health_when_exam_is_not_monitored() {
        useCase.execute(new VerifyExamScheduleOtpCommand(EXAM_ID, OTP));

        verify(healthCheckPort, never()).checkStreamingOk();
    }

    @Test
    void should_refuse_entry_ticket_when_streaming_service_is_down() {
        when(examRepository.findById(EXAM_ID))
            .thenReturn(Optional.of(exam(ExamKind.CLASS_TEST, ExamRequiredStreamType.SCREEN)));
        doThrow(new ServiceUnavailableException("Streaming service hiện không hoạt động"))
            .when(healthCheckPort).checkStreamingOk();

        assertThatThrownBy(() -> useCase.execute(new VerifyExamScheduleOtpCommand(EXAM_ID, OTP)))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("Streaming service");
    }

    /** Bài không giám sát vẫn phải phát vé bình thường, không được vạ lây vì streaming chết. */
    @Test
    void should_still_issue_entry_ticket_for_unmonitored_exam_when_streaming_is_down() {
        doThrow(new ServiceUnavailableException("Streaming service hiện không hoạt động"))
            .when(healthCheckPort).checkStreamingOk();

        var result = useCase.execute(new VerifyExamScheduleOtpCommand(EXAM_ID, OTP));

        assertThat(result.attemptId()).isNotNull();
        assertThat(result.requiredStreamType()).isNull();
    }

    private ExamCandidate candidate() {
        var candidate = new ExamCandidate();
        candidate.setId(CANDIDATE_ID);
        candidate.setExamId(EXAM_ID);
        candidate.setStudentId(STUDENT_ID);
        candidate.setScheduleId(SCHEDULE_ID);
        candidate.setAssignedPaperId(PAPER_ID);
        candidate.setStatus(ExamCandidateStatus.ATTENDED);
        return candidate;
    }

    private Exam exam(ExamKind kind, ExamRequiredStreamType streamType) {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(kind);
        exam.setStatus(ExamStatus.IN_PROGRESS);
        exam.setRequiresOtp(true);
        exam.setMaxAttempt(1);
        exam.setCloseAt(Instant.now().plusSeconds(3600));
        if (streamType != null) {
            exam.setRequiredStreamType(streamType);
            exam.setStreamTypePermission(ExamStreamTypePermission.ANY);
        }
        return exam;
    }

    private ExamSchedule schedule() {
        var schedule = new ExamSchedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setExamId(EXAM_ID);
        schedule.setStartDate(Instant.now().minusSeconds(600));
        schedule.setEndDate(Instant.now().plusSeconds(3000));
        return schedule;
    }

    private ExamSession inProgressSession() {
        var session = new ExamSession();
        session.setId(UUID.randomUUID());
        session.setExamId(EXAM_ID);
        session.setCandidateId(CANDIDATE_ID);
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        return session;
    }
}
