package com.sep.vox.application.usecase.assessmentcandidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.assessmentcandidate.ViewAssessmentCandidatesQuery;
import com.sep.vox.application.port.input.usecase.assessmentcandidate.ViewAssessmentCandidatesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

class ViewAssessmentCandidatesUseCaseTests {

    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private UserContextPort userContextPort;
    private ViewAssessmentCandidatesUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewAssessmentCandidatesUseCase(
            assessmentRepository, assessmentCandidateRepository, assessmentMemberRepository, assessmentScheduleProctorRepository,
            schoolUserRepository, userRoleQueryRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId)).thenReturn(List.of());
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
        when(assessmentCandidateRepository.findByAssessmentId(assessmentId)).thenReturn(List.of(
            candidate(scheduleId, AssessmentCandidateStatus.ASSIGNED),
            candidate(null, AssessmentCandidateStatus.ABSENT),
            candidate(scheduleId, AssessmentCandidateStatus.ABSENT)
        ));
    }

    @Test
    void should_allow_schedule_proctor_scoped_to_their_schedule() {
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId)).thenReturn(true);

        List<AssessmentCandidateDto> result = useCase.execute(new ViewAssessmentCandidatesQuery(assessmentId, scheduleId, null));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> scheduleId.equals(c.scheduleId()));
    }

    @Test
    void should_reject_non_chair_teacher_without_scheduleId() {
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);

        ViewAssessmentCandidatesQuery query = new ViewAssessmentCandidatesQuery(assessmentId, null, null);
        assertThatThrownBy(() -> useCase.execute(query)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_teacher_not_proctoring_the_requested_schedule() {
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId)).thenReturn(false);

        ViewAssessmentCandidatesQuery query = new ViewAssessmentCandidatesQuery(assessmentId, scheduleId, null);
        assertThatThrownBy(() -> useCase.execute(query)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_return_all_candidates_when_no_filter() {
        List<AssessmentCandidateDto> result = useCase.execute(new ViewAssessmentCandidatesQuery(assessmentId, null, null));
        assertThat(result).hasSize(3);
    }

    @Test
    void should_filter_by_schedule_id() {
        List<AssessmentCandidateDto> result = useCase.execute(new ViewAssessmentCandidatesQuery(assessmentId, scheduleId, null));
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> scheduleId.equals(c.scheduleId()));
    }

    @Test
    void should_filter_by_status() {
        List<AssessmentCandidateDto> result = useCase.execute(new ViewAssessmentCandidatesQuery(assessmentId, null, AssessmentCandidateStatus.ABSENT));
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.status() == AssessmentCandidateStatus.ABSENT);
    }

    @Test
    void should_filter_by_schedule_and_status() {
        List<AssessmentCandidateDto> result = useCase.execute(new ViewAssessmentCandidatesQuery(assessmentId, scheduleId, AssessmentCandidateStatus.ABSENT));
        assertThat(result).hasSize(1);
    }

    private AssessmentCandidate candidate(UUID scheduleId, AssessmentCandidateStatus status) {
        AssessmentCandidate c = new AssessmentCandidate();
        c.setId(UUID.randomUUID());
        c.setAssessmentId(assessmentId);
        c.setStudentId(UUID.randomUUID());
        c.setScheduleId(scheduleId);
        c.setStatus(status);
        c.setAssignedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }
}
