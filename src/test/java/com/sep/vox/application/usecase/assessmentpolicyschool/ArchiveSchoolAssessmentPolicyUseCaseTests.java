package com.sep.vox.application.usecase.assessmentpolicyschool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.assessmentpolicyschool.ArchiveSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.ArchiveSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Archiving a Policy also archives the Rubric Version -- but only when no other Policy
 * is still using that version.
 *
 * <p>This constraint was born together with relaxing the rule for attaching a Policy to a PUBLISHED version: when
 * several Policies can share one version (V44), an unconditional archive would pull the grading scale out from under
 * the Policies still in force.
 */
class ArchiveSchoolAssessmentPolicyUseCaseTests {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID RUBRIC_VERSION_ID = UUID.randomUUID();

    private AssessmentPolicyRepository assessmentPolicyRepository;
    private RubricVersionRepository rubricVersionRepository;
    private RubricVersion rubricVersion;
    private ArchiveSchoolAssessmentPolicyUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        SchoolUserRepository schoolUserRepository = mock(SchoolUserRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        useCase = new ArchiveSchoolAssessmentPolicyUseCase(
                assessmentPolicyRepository, rubricVersionRepository, schoolRepository,
                schoolUserRepository, userRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        User currentUser = mock(User.class);
        when(currentUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(currentUser));

        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(schoolUser));

        School school = mock(School.class);
        when(school.isActive()).thenReturn(true);
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));

        AssessmentPolicy policy = mock(AssessmentPolicy.class);
        when(policy.getId()).thenReturn(POLICY_ID);
        when(policy.getSchoolId()).thenReturn(SCHOOL_ID);
        when(policy.getStatus()).thenReturn(AssessmentPolicyStatus.PUBLISHED);
        when(policy.getRubricVersionId()).thenReturn(RUBRIC_VERSION_ID);
        when(policy.getEffectiveFrom()).thenReturn(Instant.now().minusSeconds(3600));
        when(assessmentPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(assessmentPolicyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        rubricVersion = mock(RubricVersion.class);
        when(rubricVersion.getStatus()).thenReturn(RubricStatus.PUBLISHED);
        when(rubricVersion.getEffectiveFrom()).thenReturn(Instant.now().minusSeconds(3600));
        when(rubricVersionRepository.findById(RUBRIC_VERSION_ID)).thenReturn(Optional.of(rubricVersion));
    }

    private ArchiveSchoolAssessmentPolicyCommand command() {
        return new ArchiveSchoolAssessmentPolicyCommand(SCHOOL_ID, POLICY_ID);
    }

    @Test
    void archivesTheRubricVersion_whenNoOtherPolicyStillUsesIt() {
        when(assessmentPolicyRepository.existsOtherActiveByRubricVersionId(RUBRIC_VERSION_ID, POLICY_ID))
                .thenReturn(false);

        useCase.execute(command());

        verify(rubricVersion).setStatus(RubricStatus.ARCHIVED);
        verify(rubricVersionRepository).save(rubricVersion);
    }

    @Test
    void keepsTheRubricVersion_whenAnotherPolicyStillUsesIt() {
        // A regular class and a specialized class share one Rubric version. Reclaiming the regular class's policy
        // while archiving the version too would leave the specialized class's policy pointing at a scale that was dropped -- failing
        // silently, because nothing blocks it at grading time.
        when(assessmentPolicyRepository.existsOtherActiveByRubricVersionId(RUBRIC_VERSION_ID, POLICY_ID))
                .thenReturn(true);

        useCase.execute(command());

        verify(rubricVersion, never()).setStatus(any());
        verify(rubricVersionRepository, never()).save(any());
    }

    @Test
    void archivesThePolicyItself_regardlessOfTheCascade() {
        when(assessmentPolicyRepository.existsOtherActiveByRubricVersionId(eq(RUBRIC_VERSION_ID), any()))
                .thenReturn(true);

        useCase.execute(command());

        verify(assessmentPolicyRepository).save(any(AssessmentPolicy.class));
    }
}
