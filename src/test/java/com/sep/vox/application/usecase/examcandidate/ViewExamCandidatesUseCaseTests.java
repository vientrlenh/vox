package com.sep.vox.application.usecase.examcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewExamCandidatesQuery;
import com.sep.vox.application.port.input.usecase.examcandidate.ViewExamCandidatesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class ViewExamCandidatesUseCaseTests {

    private ExamRepository examRepository;
    private ExamCandidateRepository examCandidateRepository;
    private ExamMemberRepository examMemberRepository;
    private ExamScheduleProctorRepository examScheduleProctorRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private ViewExamCandidatesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examRepository = mock(ExamRepository.class);
        examCandidateRepository = mock(ExamCandidateRepository.class);
        examMemberRepository = mock(ExamMemberRepository.class);
        examScheduleProctorRepository = mock(ExamScheduleProctorRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewExamCandidatesUseCase(
            examRepository, examCandidateRepository, examMemberRepository, examScheduleProctorRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(true);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam()));
        when(examCandidateRepository.findByExamId(examId)).thenReturn(List.of(
            candidate(scheduleId, ExamCandidateStatus.ASSIGNED),
            candidate(null, ExamCandidateStatus.ABSENT),
            candidate(scheduleId, ExamCandidateStatus.ABSENT)
        ));
    }

    @Test
    void should_allow_schedule_proctor_scoped_to_their_schedule() {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId)).thenReturn(true);

        var result = useCase.execute(new ViewExamCandidatesQuery(examId, scheduleId, null));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> scheduleId.equals(c.scheduleId()));
    }

    @Test
    void should_reject_non_chair_teacher_without_scheduleId() {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);

        var query = new ViewExamCandidatesQuery(examId, null, null);
        assertThatThrownBy(() -> useCase.execute(query)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_teacher_not_proctoring_the_requested_schedule() {
        when(examMemberRepository.existsByExamIdAndUserIdAndRole(examId, userId, ExamMemberRole.CHAIR))
            .thenReturn(false);
        when(examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId)).thenReturn(false);

        var query = new ViewExamCandidatesQuery(examId, scheduleId, null);
        assertThatThrownBy(() -> useCase.execute(query)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_return_all_candidates_when_no_filter() {
        var result = useCase.execute(new ViewExamCandidatesQuery(examId, null, null));
        assertThat(result).hasSize(3);
    }

    @Test
    void should_filter_by_schedule_id() {
        var result = useCase.execute(new ViewExamCandidatesQuery(examId, scheduleId, null));
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> scheduleId.equals(c.scheduleId()));
    }

    @Test
    void should_filter_by_status() {
        var result = useCase.execute(new ViewExamCandidatesQuery(examId, null, ExamCandidateStatus.ABSENT));
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> "ABSENT".equals(c.status()));
    }

    @Test
    void should_filter_by_schedule_and_status() {
        var result = useCase.execute(new ViewExamCandidatesQuery(examId, scheduleId, ExamCandidateStatus.ABSENT));
        assertThat(result).hasSize(1);
    }

    private ExamCandidate candidate(UUID scheduleId, ExamCandidateStatus status) {
        var c = new ExamCandidate();
        c.setId(UUID.randomUUID());
        c.setExamId(examId);
        c.setStudentId(UUID.randomUUID());
        c.setScheduleId(scheduleId);
        c.setStatus(status);
        c.setAssignedAt(OffsetDateTime.now());
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }

    private Exam exam() {
        var exam = new Exam();
        exam.setId(examId);
        exam.setSchoolId(schoolId);
        return exam;
    }
}
