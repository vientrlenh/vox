package com.sep.vox.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewAssessmentDetailsQuery;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Locks a CLOSED DOOR: a proctor assigned to a session still can NOT read the assessment details.
 *
 * <p>This is a decision and not an omission, so it needs a test to hold it. {@code assessment(id)} is the entry to the
 * assessment management screen (the council and the school); at one point a proctor branch was added here so the proctoring
 * screen could run, and the consequence was that proctors walked straight into the assessment dashboard. The real need -- a
 * proctor reading the name of the assessment they supervise -- has been split off to {@code ViewMonitorableAssessmentUseCase}, so if
 * that branch comes back here it is a regression.
 */
class ViewAssessmentDetailsProctorAccessTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID PROCTOR_ID = UUID.randomUUID();

    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private ViewAssessmentDetailsUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);

        useCase = new ViewAssessmentDetailsUseCase(
            assessmentRepository,
            assessmentMemberRepository,
            userContextPort,
            schoolUserRepository,
            userRoleQueryRepository
        );

        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setSchoolId(SCHOOL_ID);
        // In progress: the CLOSED/RESULTS_PUBLISHED branches are not open yet, so the result speaks
        // exactly about the branch under test.
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        // AssessmentDtoMapper reads .getKind().name() directly; if it is missing, an NPE breaks the test
        // rather than making a wrong assertion.
        assessment.setKind(AssessmentKind.CENTRALIZED);
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment));

        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(any(), any(), any())).thenReturn(false);
        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(schoolUserRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(any())).thenReturn(List.of());
    }

    @Test
    void a_proctor_who_is_not_an_assessment_member_cannot_open_the_assessment_dashboard() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(PROCTOR_ID);

        assertThatThrownBy(() -> useCase.execute(new ViewAssessmentDetailsQuery(ASSESSMENT_ID)))
            .isInstanceOf(ForbiddenException.class);
    }
}
