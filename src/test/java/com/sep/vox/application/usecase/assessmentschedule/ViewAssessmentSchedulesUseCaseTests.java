package com.sep.vox.application.usecase.assessmentschedule;

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
import com.sep.vox.application.port.input.query.ViewAssessmentSchedulesQuery;
import com.sep.vox.application.port.input.usecase.assessmentschedule.ViewAssessmentSchedulesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;

class ViewAssessmentSchedulesUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentScheduleRepository assessmentScheduleRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private UserContextPort userContextPort;
    private ViewAssessmentSchedulesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    private final Instant base = OffsetDateTime.of(2026, 7, 10, 9, 0, 0, 0, ZoneOffset.UTC).toInstant();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentScheduleRepository = mock(AssessmentScheduleRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewAssessmentSchedulesUseCase(
            assessmentRepository, assessmentScheduleRepository, assessmentMemberRepository, assessmentScheduleProctorRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
        when(assessmentScheduleRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            schedule(base, AssessmentScheduleStatus.DRAFT),
            schedule(base.plus(2, ChronoUnit.DAYS), AssessmentScheduleStatus.PUBLISHED),
            schedule(base.plus(4, ChronoUnit.DAYS), AssessmentScheduleStatus.DRAFT)
        ));
    }

    @Test
    void should_return_all_schedules_when_no_filter() {
        givenTeacherChair();

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(assessmentId, null, null, null));

        assertThat(result).hasSize(3);
    }

    @Test
    void should_filter_by_status() {
        givenTeacherChair();

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(assessmentId, AssessmentScheduleStatus.DRAFT, null, null));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(s -> s.status() == AssessmentScheduleStatus.DRAFT);
    }

    @Test
    void should_filter_by_start_date_lower_bound() {
        givenTeacherChair();

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(assessmentId, null, base.plus(2, ChronoUnit.DAYS), null));

        assertThat(result).hasSize(2);
    }

    @Test
    void should_filter_by_end_date_upper_bound() {
        givenTeacherChair();

        List<AssessmentScheduleDto> result = useCase.execute(new ViewAssessmentSchedulesQuery(assessmentId, null, null, base.plus(2, ChronoUnit.DAYS)));

        assertThat(result).hasSize(2);
    }

    @Test
    void should_filter_by_start_and_end_window() {
        givenTeacherChair();

        List<AssessmentScheduleDto> result = useCase.execute(
            new ViewAssessmentSchedulesQuery(assessmentId, null, base.plus(2, ChronoUnit.DAYS), base.plus(2, ChronoUnit.DAYS)));

        assertThat(result).hasSize(1);
    }

    @Test
    void should_list_school_wide_when_assessment_id_null_and_school_admin() {
        givenSchoolAdmin(schoolId);
        when(assessmentScheduleRepository.findBySchoolId(schoolId)).thenReturn(List.of(
            schedule(base, AssessmentScheduleStatus.DRAFT),
            schedule(base.plus(2, ChronoUnit.DAYS), AssessmentScheduleStatus.PUBLISHED)
        ));

        List<AssessmentScheduleDto> result = useCase.execute(
            new ViewAssessmentSchedulesQuery(null, AssessmentScheduleStatus.DRAFT, null, null));

        assertThat(result).hasSize(1);
        verify(assessmentScheduleRepository).findBySchoolId(schoolId);
        verify(assessmentScheduleRepository, never()).findByAssessmentId(assessmentId);
    }

    @Test
    void should_reject_school_wide_when_not_school_admin() {
        when(userContextPort.isSchoolAdmin()).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new ViewAssessmentSchedulesQuery(null, null, null, null)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_school_wide_when_school_admin_has_no_school() {
        givenSchoolAdmin(null);

        assertThatThrownBy(() -> useCase.execute(new ViewAssessmentSchedulesQuery(null, null, null, null)))
            .isInstanceOf(ForbiddenException.class);
    }

    private AssessmentSchedule schedule(Instant startDate, AssessmentScheduleStatus status) {
        AssessmentSchedule s = new AssessmentSchedule();
        s.setId(UUID.randomUUID());
        s.setAssessmentId(assessmentId);
        s.setSchoolRoomId(UUID.randomUUID());
        s.setStartDate(startDate);
        s.setEndDate(startDate.plus(2, ChronoUnit.HOURS));
        s.setStatus(status);
        return s;
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }

    private void givenTeacherChair() {
        when(userContextPort.isTeacher()).thenReturn(true);
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(
            assessmentId, userId, AssessmentMemberRole.CHAIR
        )).thenReturn(true);
    }

    private void givenSchoolAdmin(UUID currentSchoolId) {
        when(userContextPort.isSchoolAdmin()).thenReturn(true);
        when(userContextPort.getCurrentSchoolId()).thenReturn(currentSchoolId);
    }
}
