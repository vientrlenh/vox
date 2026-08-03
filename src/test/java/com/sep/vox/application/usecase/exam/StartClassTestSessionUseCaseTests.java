package com.sep.vox.application.usecase.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.StartClassTestSessionCommand;
import com.sep.vox.application.port.input.usecase.exam.StartClassTestSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.CreateExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Bài kiểm tra trên lớp giờ thi trong phòng có giám khảo, nên cổng vào thi phải siết đúng như kỳ
 * thi tập trung: đã điểm danh, đã được xếp ca, ca thi đang trong giờ.
 */
class StartClassTestSessionUseCaseTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();

    private ExamCandidateRepository examCandidateRepository;
    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamSessionRepository examSessionRepository;
    private StartClassTestSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examSessionRepository = mock(ExamSessionRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new StartClassTestSessionUseCase(
            examCandidateRepository,
            mock(ExamCandidateResultRepository.class),
            examRepository,
            examScheduleRepository,
            examSessionRepository,
            userContextPort,
            mock(CreateExamSessionUseCase.class),
            mock(UpdateExamSessionStatusUseCase.class)
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(examCandidateRepository.findByExamIdAndStudentId(EXAM_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate(ExamCandidateStatus.ATTENDED)));
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(false)));
        when(examScheduleRepository.findByIdAndInSchedule(any(UUID.class), any(Instant.class)))
            .thenReturn(Optional.of(schedule()));
        when(examSessionRepository.findLatestByCandidateIdAndStatuses(any(UUID.class), anyCollection()))
            .thenReturn(Optional.of(inProgressSession()));
    }

    @Test
    void should_issue_entry_ticket_when_candidate_is_attended_and_scheduled() {
        var result = useCase.execute(new StartClassTestSessionCommand(EXAM_ID));

        assertThat(result.attemptId()).isNotNull();
        assertThat(result.scheduleEndAt()).isNotNull();
    }

    @Test
    void should_reject_when_candidate_has_not_been_checked_in() {
        when(examCandidateRepository.findByExamIdAndStudentId(EXAM_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate(ExamCandidateStatus.ASSIGNED)));

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(EXAM_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bạn chưa được điểm danh có mặt");
    }

    @Test
    void should_reject_when_candidate_is_not_assigned_to_any_schedule() {
        var candidate = candidate(ExamCandidateStatus.ATTENDED);
        candidate.setScheduleId(null);
        when(examCandidateRepository.findByExamIdAndStudentId(EXAM_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(EXAM_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bạn chưa được xếp ca thi");
    }

    @Test
    void should_reject_when_schedule_is_outside_its_time_window() {
        when(examScheduleRepository.findByIdAndInSchedule(any(UUID.class), any(Instant.class)))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(EXAM_ID)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Ca thi không hợp lệ hoặc đã hết hạn");
    }

    /** Giáo viên bật OTP thì học sinh phải đi cổng OTP, không được vào thẳng qua cổng này. */
    @Test
    void should_redirect_to_otp_flow_when_class_test_requires_otp() {
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam(true)));

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(EXAM_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("yêu cầu xác thực OTP");
    }

    @Test
    void should_reject_when_candidate_was_blocked() {
        var candidate = candidate(ExamCandidateStatus.ATTENDED);
        candidate.setBlockedAt(Instant.now());
        when(examCandidateRepository.findByExamIdAndStudentId(EXAM_ID, STUDENT_ID))
            .thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> useCase.execute(new StartClassTestSessionCommand(EXAM_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Bạn đã bị buộc kết thúc bài thi này");
    }

    private ExamCandidate candidate(ExamCandidateStatus status) {
        var candidate = new ExamCandidate();
        candidate.setId(CANDIDATE_ID);
        candidate.setExamId(EXAM_ID);
        candidate.setStudentId(STUDENT_ID);
        candidate.setScheduleId(SCHEDULE_ID);
        candidate.setAssignedPaperId(PAPER_ID);
        candidate.setStatus(status);
        return candidate;
    }

    private Exam exam(boolean requiresOtp) {
        var exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setKind(ExamKind.CLASS_TEST);
        exam.setStatus(ExamStatus.IN_PROGRESS);
        exam.setRequiresOtp(requiresOtp);
        exam.setMaxAttempt(1);
        exam.setCloseAt(Instant.now().plusSeconds(3600));
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
