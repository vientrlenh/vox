package com.sep.vox.application.usecase.assessmentpolicyschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CreateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * 1 Rubric Version may be attached to many Assessment Policies, even when 2 Policies sit in 2 different
 * classes (schoolClassId) of the same school.
 * The boundary after V44: many Assessment Policies with different SCOPES share one Rubric
 * Version, but each scope still has exactly one policy in force.
 */
class CreateSchoolAssessmentPolicyUseCaseTests {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_VERSION_ID = UUID.randomUUID();
    private static final UUID RUBRIC_ID = UUID.randomUUID();
    private static final UUID RUBRIC_VERSION_ID = UUID.randomUUID();
    private static final UUID TARGET_BAND_ID = UUID.randomUUID();
    private static final UUID LANGUAGE_ID = UUID.randomUUID();

    private AssessmentPolicyRepository assessmentPolicyRepository;
    private SchoolClassRepository schoolClassRepository;
    // The Rubric version status is what the tests below keep flipping, so the mock must be a field.
    private RubricVersion rubricVersion;
    private CreateSchoolAssessmentPolicyUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        FrameworkVersionRepository frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        FrameworkResultBandRepository frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        RubricVersionRepository rubricVersionRepository = mock(RubricVersionRepository.class);
        RubricRepository rubricRepository = mock(RubricRepository.class);
        SupportedLanguageRepository languageRepository = mock(SupportedLanguageRepository.class);
        GradeLevelRepository gradeLevelRepository = mock(GradeLevelRepository.class);
        SchoolGradeRepository schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        SchoolUserRepository schoolUserRepository = mock(SchoolUserRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);

        // Real guard, fake repository: no ceiling row is stubbed, so
        // findByGradeLevelIdAndFrameworkVersionId returns Optional.empty() -> the guard blocks
        // nothing. That is exactly what the tests below need, because they check the scope boundary
        // and not the band ceiling. The band ceiling has its own test suite in
        // GradeLevelBandScopeGuardServiceTests.
        GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService = new GradeLevelBandScopeGuardService(
                mock(GradeLevelBandScopeRepository.class), frameworkResultBandRepository,
                gradeLevelRepository, schoolGradeRepository, schoolClassRepository);

        useCase = new CreateSchoolAssessmentPolicyUseCase(
                assessmentPolicyRepository, frameworkVersionRepository, frameworkResultBandRepository,
                rubricVersionRepository, rubricRepository, languageRepository, gradeLevelRepository,
                schoolGradeRepository, schoolClassRepository, schoolUserRepository, userRepository, userContextPort,
                gradeLevelBandScopeGuardService);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        User currentUser = mock(User.class);
        when(currentUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(currentUser));

        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(schoolUser));

        FrameworkVersion frameworkVersion = mock(FrameworkVersion.class);
        when(frameworkVersion.getStatus()).thenReturn(FrameworkVersionStatus.PUBLISHED);
        when(frameworkVersion.getFrameworkId()).thenReturn(FRAMEWORK_ID);
        when(frameworkVersionRepository.findById(FRAMEWORK_VERSION_ID)).thenReturn(Optional.of(frameworkVersion));

        when(languageRepository.existsById(any())).thenReturn(true);

        FrameworkResultBand targetBand = mock(FrameworkResultBand.class);
        when(targetBand.getFrameworkVersionId()).thenReturn(FRAMEWORK_VERSION_ID);
        when(frameworkResultBandRepository.findById(TARGET_BAND_ID)).thenReturn(Optional.of(targetBand));

        rubricVersion = mock(RubricVersion.class);
        when(rubricVersion.getStatus()).thenReturn(RubricStatus.DRAFT);
        when(rubricVersion.getRubricId()).thenReturn(RUBRIC_ID);
        when(rubricVersionRepository.findById(RUBRIC_VERSION_ID)).thenReturn(Optional.of(rubricVersion));

        Rubric rubric = mock(Rubric.class);
        when(rubric.getOwnerType()).thenReturn(RubricOwnerType.SCHOOL);
        when(rubric.getSchoolId()).thenReturn(SCHOOL_ID);
        when(rubric.getFrameworkId()).thenReturn(FRAMEWORK_ID);
        when(rubricRepository.findById(RUBRIC_ID)).thenReturn(Optional.of(rubric));

        when(assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(assessmentPolicyRepository.findMaxVersionForScope(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(assessmentPolicyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateAssessmentPolicyCommand command(UUID schoolClassId) {
        return new CreateAssessmentPolicyCommand(
                SCHOOL_ID, FRAMEWORK_VERSION_ID, RUBRIC_VERSION_ID, LANGUAGE_ID,
                null, null, schoolClassId, TARGET_BAND_ID, null, null,
                Instant.now(), null);
    }

    private void stubSchoolClass(UUID classId) {
        SchoolClass schoolClass = mock(SchoolClass.class);
        when(schoolClass.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
    }

    @Test
    void allows_whenSameBatchReusesRubricVersionAcrossDifferentClasses() {
        // V44: a specialized class and a regular class need separate policies by Class scope but are graded with the same
        // set of criteria. Before V44 every policy had to have its own rubric version, so the school
        // had to duplicate the very same criteria set for every class.
        UUID classId1 = UUID.randomUUID();
        UUID classId2 = UUID.randomUUID();
        stubSchoolClass(classId1);
        stubSchoolClass(classId2);

        List<CreateAssessmentPolicyCommand> commands = List.of(
                command(classId1),
                command(classId2) // another class -> another scope, same rubricVersionId
        );

        assertThat(useCase.execute(commands)).hasSize(2);
    }

    @Test
    void stillRejects_whenTwoPoliciesInBatchShareTheSameScope() {
        // The constraint still in force after V44: each scope has only one policy. At grading time only ONE
        // policy is chosen for a scope, so a second one would be dead data.
        UUID classId = UUID.randomUUID();
        stubSchoolClass(classId);

        assertThatThrownBy(() -> useCase.execute(List.of(command(classId), command(classId))))
                .isInstanceOf(ResourceDuplicatedException.class)
                .hasMessageContaining("same scope");
    }

    @Test
    void allowsAttachingToAnAlreadyPublishedRubricVersion() {
        // Relaxed on 2026-08-23. The old rule blocked PUBLISHED, which meant the school had exactly one window -- before
        // the version was published -- to declare every class that would use this set of criteria. But publishing again is the
        // condition for using it in an assessment, so adding a class afterwards forced the whole Rubric to be copied.
        when(rubricVersion.getStatus()).thenReturn(RubricStatus.PUBLISHED);
        UUID classId = UUID.randomUUID();
        stubSchoolClass(classId);

        assertThat(useCase.execute(List.of(command(classId)))).hasSize(1);
    }

    @Test
    void rejects_whenRubricVersionIsArchived() {
        // The reverse direction of the same rule: an archived version must not get more policies attached,
        // otherwise students are graded with a scale the school has dropped. The old rule used `== PUBLISHED` so the
        // ARCHIVED branch slipped through -- blocking the version in use by mistake and letting the dropped one pass.
        when(rubricVersion.getStatus()).thenReturn(RubricStatus.ARCHIVED);
        UUID classId = UUID.randomUUID();
        stubSchoolClass(classId);

        assertThatThrownBy(() -> useCase.execute(List.of(command(classId))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ARCHIVED");
    }
}
