package com.sep.vox.application.usecase.assessmentpolicyschool;

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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.CreateAssessmentPolicyCommand;
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
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * 1 Rubric Version chỉ được gắn với đúng 1 Assessment Policy -- kể cả khi 2 Policy nằm ở 2 lớp
 * (schoolClassId) khác nhau trong cùng trường.
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
    private CreateSchoolAssessmentPolicyUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        var frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        var frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        var rubricVersionRepository = mock(RubricVersionRepository.class);
        var rubricRepository = mock(RubricRepository.class);
        var languageRepository = mock(SupportedLanguageRepository.class);
        var schoolGradeLevelRepository = mock(SchoolGradeLevelRepository.class);
        var schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        var schoolUserRepository = mock(SchoolUserRepository.class);
        var userRepository = mock(UserRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new CreateSchoolAssessmentPolicyUseCase(
                assessmentPolicyRepository, frameworkVersionRepository, frameworkResultBandRepository,
                rubricVersionRepository, rubricRepository, languageRepository, schoolGradeLevelRepository,
                schoolGradeRepository, schoolClassRepository, schoolUserRepository, userRepository, userContextPort);

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

        RubricVersion rubricVersion = mock(RubricVersion.class);
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
    void rejects_whenRubricVersionAlreadyUsedByAnotherPolicy() {
        when(assessmentPolicyRepository.existsByRubricVersionId(RUBRIC_VERSION_ID)).thenReturn(true);
        UUID classId = UUID.randomUUID();
        stubSchoolClass(classId);

        assertThatThrownBy(() -> useCase.execute(List.of(command(classId))))
                .isInstanceOf(DuplicatedException.class)
                .hasMessageContaining("đã gắn với một Assessment Policy khác");
    }

    @Test
    void rejects_whenSameBatchReusesRubricVersionAcrossDifferentClasses() {
        when(assessmentPolicyRepository.existsByRubricVersionId(RUBRIC_VERSION_ID)).thenReturn(false);
        UUID classId1 = UUID.randomUUID();
        UUID classId2 = UUID.randomUUID();
        stubSchoolClass(classId1);
        stubSchoolClass(classId2);

        List<CreateAssessmentPolicyCommand> commands = List.of(
                command(classId1),
                command(classId2) // lớp khác -> scope khác, nhưng cùng rubricVersionId
        );

        assertThatThrownBy(() -> useCase.execute(commands))
                .isInstanceOf(DuplicatedException.class)
                .hasMessageContaining("cùng dùng 1 Phiên bản Rubric");
    }
}
