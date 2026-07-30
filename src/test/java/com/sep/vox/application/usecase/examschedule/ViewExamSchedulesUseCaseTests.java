package com.sep.vox.application.usecase.examschedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewExamSchedulesQuery;
import com.sep.vox.application.port.input.usecase.examschedule.ViewExamSchedulesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;

class ViewExamSchedulesUseCaseTests {

    private ExamRepository examRepository;
    private ExamScheduleRepository examScheduleRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private UserContextPort userContextPort;
    private ViewExamSchedulesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    private final Instant base = OffsetDateTime.of(2026, 7, 10, 9, 0, 0, 0, ZoneOffset.UTC).toInstant();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examScheduleRepository = mock(ExamScheduleRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewExamSchedulesUseCase(
            examRepository, examScheduleRepository, examMemberRepository, examScheduleProctorRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examScheduleRepository.findByExamId(examId)).thenReturn(List.of(
            schedule(base, ExamScheduleStatus.DRAFT),
            schedule(base.plus(2, ChronoUnit.DAYS), ExamScheduleStatus.PUBLISHED),
            schedule(base.plus(4, ChronoUnit.DAYS), ExamScheduleStatus.DRAFT)
        ));
    }

    @Test
    void should_return_all_schedules_when_no_filter() {
        givenTeacherChair();

        var result = useCase.execute(new ViewExamSchedulesQuery(examId, null, null, null));

        assertThat(result).hasSize(3);
    }

    @Test
    void should_filter_by_status() {
        givenTeacherChair();

        var result = useCase.execute(new ViewExamSchedulesQuery(examId, ExamScheduleStatus.DRAFT, null, null));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> "DRAFT".equals(s.status()));
    }

    @Test
    void should_filter_by_start_date_lower_bound() {
        givenTeacherChair();

        var result = useCase.execute(new ViewExamSchedulesQuery(examId, null, base.plus(2, ChronoUnit.DAYS), null));

        assertThat(result).hasSize(2);
    }

    @Test
    void should_filter_by_end_date_upper_bound() {
        givenTeacherChair();

        var result = useCase.execute(new ViewExamSchedulesQuery(examId, null, null, base.plus(2, ChronoUnit.DAYS)));

        assertThat(result).hasSize(2);
    }

    @Test
    void should_filter_by_start_and_end_window() {
        givenTeacherChair();

        var result = useCase.execute(
            new ViewExamSchedulesQuery(examId, null, base.plus(2, ChronoUnit.DAYS), base.plus(2, ChronoUnit.DAYS)));

        assertThat(result).hasSize(1);
    }

    @Test
    void should_list_school_wide_when_exam_id_null_and_school_admin() {
        givenSchoolAdmin(schoolId);
        when(examScheduleRepository.findBySchoolId(schoolId)).thenReturn(List.of(
            schedule(base, ExamScheduleStatus.DRAFT),
            schedule(base.plus(2, ChronoUnit.DAYS), ExamScheduleStatus.PUBLISHED)
        ));

        var result = useCase.execute(
            new ViewExamSchedulesQuery(null, ExamScheduleStatus.DRAFT, null, null));

        assertThat(result).hasSize(1);
        verify(examScheduleRepository).findBySchoolId(schoolId);
        verify(examScheduleRepository, never()).findByExamId(examId);
    }

    @Test
    void should_reject_school_wide_when_not_school_admin() {
        when(userContextPort.isSchoolAdmin()).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewExamSchedulesQuery(null, null, null, null)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_school_wide_when_school_admin_has_no_school() {
        givenSchoolAdmin(null);

        assertThatThrownBy(() -> useCase.execute(new ViewExamSchedulesQuery(null, null, null, null)))
            .isInstanceOf(ForbiddenException.class);
    }

    private ExamSchedule schedule(Instant startDate, ExamScheduleStatus status) {
        var s = new ExamSchedule();
        s.setId(UUID.randomUUID());
        s.setExamId(examId);
        s.setSchoolRoomId(UUID.randomUUID());
        s.setStartDate(startDate);
        s.setEndDate(startDate.plus(2, ChronoUnit.HOURS));
        s.setStatus(status);
        return s;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }

    private void givenTeacherChair() {
        when(userContextPort.isTeacher()).thenReturn(true);
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(
            examId, userId, ExamMemberRole.CHAIR
        )).thenReturn(true);
    }

    private void givenSchoolAdmin(UUID currentSchoolId) {
        when(userContextPort.isSchoolAdmin()).thenReturn(true);
        when(userContextPort.getCurrentSchoolId()).thenReturn(currentSchoolId);
    }
}
