package com.sep.vox.application.usecase.assessmentpolicyschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.CloneSystemAssessmentPolicyToSchoolCommand;
import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CloneSystemAssessmentPolicyToSchoolUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.service.rubric.RubricCloneService;

class CloneSystemAssessmentPolicyToSchoolUseCaseTests {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID SOURCE_POLICY_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_VERSION_ID = UUID.randomUUID();
    private static final UUID LANGUAGE_ID = UUID.randomUUID();
    private static final UUID SOURCE_RUBRIC_ID = UUID.randomUUID();
    private static final UUID SOURCE_RUBRIC_VERSION_ID = UUID.randomUUID();
    private static final UUID CLONED_RUBRIC_VERSION_ID = UUID.randomUUID();
    private static final UUID TARGET_BAND_ID = UUID.randomUUID();
    private static final UUID GRADE_LEVEL_ID = UUID.randomUUID();

    private AssessmentPolicyRepository assessmentPolicyRepository;
    private RubricRepository rubricRepository;
    private RubricCloneService rubricCloneService;
    private AssessmentPolicy sourcePolicy;
    private CloneSystemAssessmentPolicyToSchoolUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        var frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        var frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        rubricRepository = mock(RubricRepository.class);
        var rubricVersionRepository = mock(RubricVersionRepository.class);
        var rubricCriterionRepository = mock(RubricCriterionRepository.class);
        rubricCloneService = mock(RubricCloneService.class);
        var gradeLevelRepository = mock(GradeLevelRepository.class);
        var schoolGradeRepository = mock(SchoolGradeRepository.class);
        var schoolClassRepository = mock(SchoolClassRepository.class);
        var schoolUserRepository = mock(SchoolUserRepository.class);
        var userRepository = mock(UserRepository.class);
        var userContextPort = mock(UserContextPort.class);

        // Guard thật + repository trần giả rỗng -> không có trần nào được khai nên guard không chặn.
        var bandScopeGuard = new GradeLevelBandScopeGuardService(
                mock(GradeLevelBandScopeRepository.class), frameworkResultBandRepository,
                gradeLevelRepository, schoolGradeRepository, schoolClassRepository);

        useCase = new CloneSystemAssessmentPolicyToSchoolUseCase(
                assessmentPolicyRepository, frameworkVersionRepository, frameworkResultBandRepository,
                rubricRepository, rubricVersionRepository, rubricCriterionRepository, rubricCloneService,
                gradeLevelRepository, schoolGradeRepository, schoolClassRepository, schoolUserRepository,
                userRepository, userContextPort, bandScopeGuard);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        User currentUser = mock(User.class);
        when(currentUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(currentUser));

        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(schoolUser));

        sourcePolicy = mock(AssessmentPolicy.class);
        when(sourcePolicy.getSchoolId()).thenReturn(null);
        when(sourcePolicy.getStatus()).thenReturn(AssessmentPolicyStatus.PUBLISHED);
        when(sourcePolicy.getGradeLevelId()).thenReturn(GRADE_LEVEL_ID);
        when(sourcePolicy.getLanguageId()).thenReturn(LANGUAGE_ID);
        when(sourcePolicy.getFrameworkVersionId()).thenReturn(FRAMEWORK_VERSION_ID);
        when(sourcePolicy.getRubricVersionId()).thenReturn(SOURCE_RUBRIC_VERSION_ID);
        when(sourcePolicy.getTargetFrameworkBandId()).thenReturn(TARGET_BAND_ID);
        when(assessmentPolicyRepository.findById(SOURCE_POLICY_ID)).thenReturn(Optional.of(sourcePolicy));

        FrameworkVersion frameworkVersion = mock(FrameworkVersion.class);
        when(frameworkVersion.getStatus()).thenReturn(FrameworkVersionStatus.PUBLISHED);
        when(frameworkVersionRepository.findById(FRAMEWORK_VERSION_ID)).thenReturn(Optional.of(frameworkVersion));

        RubricVersion sourceVersion = mock(RubricVersion.class);
        when(sourceVersion.getId()).thenReturn(SOURCE_RUBRIC_VERSION_ID);
        when(sourceVersion.getRubricId()).thenReturn(SOURCE_RUBRIC_ID);
        when(rubricVersionRepository.findById(SOURCE_RUBRIC_VERSION_ID)).thenReturn(Optional.of(sourceVersion));

        Rubric sourceRubric = mock(Rubric.class);
        when(sourceRubric.getOwnerType()).thenReturn(RubricOwnerType.SYSTEM);
        when(sourceRubric.getLanguageId()).thenReturn(LANGUAGE_ID);
        when(sourceRubric.getFrameworkId()).thenReturn(FRAMEWORK_ID);
        when(rubricRepository.findById(SOURCE_RUBRIC_ID)).thenReturn(Optional.of(sourceRubric));

        when(rubricCriterionRepository.findByRubricVersionId(SOURCE_RUBRIC_VERSION_ID))
                .thenReturn(List.of(mock(RubricCriterion.class)));

        FrameworkResultBand targetBand = mock(FrameworkResultBand.class);
        when(frameworkResultBandRepository.findById(TARGET_BAND_ID)).thenReturn(Optional.of(targetBand));

        RubricVersion clonedVersion = mock(RubricVersion.class);
        when(clonedVersion.getId()).thenReturn(CLONED_RUBRIC_VERSION_ID);
        when(rubricCloneService.cloneToSchoolAsDraft(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(clonedVersion);

        when(rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
                any(), any(), any(), any(), any())).thenReturn(false);
        when(assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(
                any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(assessmentPolicyRepository.findMaxVersionForScope(any(), any(), any(), any(), any(), any()))
                .thenReturn(0);
        when(assessmentPolicyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CloneSystemAssessmentPolicyToSchoolCommand command(
            UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {
        return new CloneSystemAssessmentPolicyToSchoolCommand(
                SCHOOL_ID, SOURCE_POLICY_ID, "ENG-K10", "Tiếng Anh khối 10", null, null,
                gradeLevelId, schoolGradeId, schoolClassId, Instant.now(), null);
    }

    @Test
    void clonesRubricAndCreatesDraftPolicyInheritingTheTemplateGradeLevel() {
        useCase.execute(command(null, null, null));

        var saved = org.mockito.ArgumentCaptor.forClass(AssessmentPolicy.class);
        verify(assessmentPolicyRepository).save(saved.capture());

        assertThat(saved.getValue().getSchoolId()).isEqualTo(SCHOOL_ID);
        // Khối đi theo bản mẫu -- đây là thứ khiến chính sách vẫn đúng khi trường mở niên khóa mới.
        assertThat(saved.getValue().getGradeLevelId()).isEqualTo(GRADE_LEVEL_ID);
        // Trỏ vào BẢN SAO rubric, không phải rubric SYSTEM của bản mẫu.
        assertThat(saved.getValue().getRubricVersionId()).isEqualTo(CLONED_RUBRIC_VERSION_ID);
        assertThat(saved.getValue().getStatus()).isEqualTo(AssessmentPolicyStatus.DRAFT);
        assertThat(saved.getValue().getTargetFrameworkBandId()).isEqualTo(TARGET_BAND_ID);
    }

    @Test
    void rejects_whenSourceIsNotASystemPolicy() {
        when(sourcePolicy.getSchoolId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(command(null, null, null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("chính sách mẫu của hệ thống");
    }

    @Test
    void rejects_whenSourceIsNotPublished() {
        when(sourcePolicy.getStatus()).thenReturn(AssessmentPolicyStatus.DRAFT);

        assertThatThrownBy(() -> useCase.execute(command(null, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");
    }

    @Test
    void rejects_whenCallerOverridesScopeOfATemplateThatAlreadyHasAGradeLevel() {
        assertThatThrownBy(() -> useCase.execute(command(UUID.randomUUID(), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("giữ nguyên khối đó");
    }

    @Test
    void rejects_whenTemplateHasNoGradeLevelAndCallerPicksNoScope() {
        when(sourcePolicy.getGradeLevelId()).thenReturn(null);

        assertThatThrownBy(() -> useCase.execute(command(null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đúng 1 phạm vi");
    }

    @Test
    void rejects_whenScopeAlreadyHasAnActivePolicy() {
        when(assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(
                any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command(null, null, null)))
                .isInstanceOf(DuplicatedException.class)
                .hasMessageContaining("còn hiệu lực");
    }

    @Test
    void doesNotCloneRubric_whenRubricCodeIsAlreadyTakenInTheSchool() {
        when(rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
                eq(RubricOwnerType.SCHOOL.toString()), eq(SCHOOL_ID), any(), any(), eq("ENG-K10")))
                .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command(null, null, null)))
                .isInstanceOf(DuplicatedException.class);

        // Kiểm sớm để không tạo rubric mồ côi rồi mới hỏng ở bước sau.
        verify(rubricCloneService, never())
                .cloneToSchoolAsDraft(any(), any(), any(), any(), any(), any(), any(), any());
        verify(assessmentPolicyRepository, never()).save(any());
    }
}
