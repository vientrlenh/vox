package com.sep.vox.application.usecase.rubricschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CloneSystemRubricToSchoolCommand;
import com.sep.vox.application.port.input.command.CloneSystemRubricToSchoolCommand.PolicyToClone;
import com.sep.vox.application.port.input.command.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CreateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CloneSystemRubricToSchoolUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.service.rubric.RubricCloneService;

/**
 * Sao bộ tiêu chí mẫu kèm luôn chính sách chấm mẫu.
 *
 * <p>Việc tạo chính sách uỷ hết cho {@code CreateSchoolAssessmentPolicyUseCase}, nên các test ở đây
 * kiểm đúng phần use case này chịu trách nhiệm: lệnh dựng ra có sao đúng thông số của bản mẫu không,
 * phạm vi có theo luật kế thừa Khối không, và bản mẫu lạ có bị chặn không.
 */
class CloneSystemRubricToSchoolUseCaseTests {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID SOURCE_RUBRIC_ID = UUID.randomUUID();
    private static final UUID SOURCE_VERSION_ID = UUID.randomUUID();
    private static final UUID CLONED_VERSION_ID = UUID.randomUUID();
    private static final UUID TEMPLATE_POLICY_ID = UUID.randomUUID();
    private static final UUID LANGUAGE_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_VERSION_ID = UUID.randomUUID();
    private static final UUID TARGET_BAND_ID = UUID.randomUUID();

    private AssessmentPolicyRepository assessmentPolicyRepository;
    private CreateSchoolAssessmentPolicyUseCase createSchoolAssessmentPolicyUseCase;
    private AssessmentPolicy template;
    private CloneSystemRubricToSchoolUseCase useCase;

    @BeforeEach
    void setUp() {
        var rubricRepository = mock(RubricRepository.class);
        var rubricVersionRepository = mock(RubricVersionRepository.class);
        var rubricCriterionRepository = mock(RubricCriterionRepository.class);
        var rubricCloneService = mock(RubricCloneService.class);
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        createSchoolAssessmentPolicyUseCase = mock(CreateSchoolAssessmentPolicyUseCase.class);
        var schoolUserRepository = mock(SchoolUserRepository.class);
        var userRepository = mock(UserRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new CloneSystemRubricToSchoolUseCase(
                rubricRepository, rubricVersionRepository, rubricCriterionRepository, rubricCloneService,
                assessmentPolicyRepository, createSchoolAssessmentPolicyUseCase,
                schoolUserRepository, userRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        User currentUser = mock(User.class);
        when(currentUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(currentUser));

        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(schoolUser));

        RubricVersion sourceVersion = mock(RubricVersion.class);
        when(sourceVersion.getId()).thenReturn(SOURCE_VERSION_ID);
        when(sourceVersion.getRubricId()).thenReturn(SOURCE_RUBRIC_ID);
        when(sourceVersion.getStatus()).thenReturn(RubricStatus.PUBLISHED);
        when(sourceVersion.getTotalScoreMethod()).thenReturn(RubricTotalScoreMethod.SUM);
        when(rubricVersionRepository.findById(SOURCE_VERSION_ID)).thenReturn(Optional.of(sourceVersion));

        Rubric sourceRubric = mock(Rubric.class);
        when(sourceRubric.getOwnerType()).thenReturn(RubricOwnerType.SYSTEM);
        when(sourceRubric.getLanguageId()).thenReturn(LANGUAGE_ID);
        when(sourceRubric.getFrameworkId()).thenReturn(FRAMEWORK_ID);
        when(rubricRepository.findById(SOURCE_RUBRIC_ID)).thenReturn(Optional.of(sourceRubric));

        when(rubricCriterionRepository.findByRubricVersionId(SOURCE_VERSION_ID))
                .thenReturn(List.of(mock(RubricCriterion.class)));
        when(rubricRepository.existsByOwnerTypeAndSchoolIdAndLanguageIdAndFrameworkIdAndCode(
                any(), any(), any(), any(), any())).thenReturn(false);

        RubricVersion clonedVersion = mock(RubricVersion.class);
        when(clonedVersion.getId()).thenReturn(CLONED_VERSION_ID);
        when(rubricCloneService.cloneToSchoolAsDraft(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(clonedVersion);

        template = mock(AssessmentPolicy.class);
        when(template.getId()).thenReturn(TEMPLATE_POLICY_ID);
        when(template.getStatus()).thenReturn(AssessmentPolicyStatus.PUBLISHED);
        when(template.getLanguageId()).thenReturn(LANGUAGE_ID);
        when(template.getFrameworkVersionId()).thenReturn(FRAMEWORK_VERSION_ID);
        when(template.getTargetFrameworkBandId()).thenReturn(TARGET_BAND_ID);
        when(template.getPassingScore()).thenReturn(new BigDecimal("5.00"));
        when(template.getStrictness()).thenReturn(AssessmentPolicyStrictness.STANDARD);
        when(assessmentPolicyRepository.findPublishedSystemWideByRubricVersionId(SOURCE_VERSION_ID))
                .thenReturn(List.of(template));
    }

    private CloneSystemRubricToSchoolCommand command(List<PolicyToClone> policies) {
        return new CloneSystemRubricToSchoolCommand(
                SCHOOL_ID, SOURCE_VERSION_ID, "ENG-K10", "Bộ tiêu chí Khối 10", null, null, policies);
    }

    private PolicyToClone choice(UUID gradeLevelId, UUID schoolGradeId, UUID schoolClassId) {
        return new PolicyToClone(TEMPLATE_POLICY_ID, gradeLevelId, schoolGradeId, schoolClassId,
                Instant.parse("2026-09-01T00:00:00Z"), null);
    }

    @SuppressWarnings("unchecked")
    private List<CreateAssessmentPolicyCommand> capturePolicyCommands() {
        ArgumentCaptor<List<CreateAssessmentPolicyCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(createSchoolAssessmentPolicyUseCase).execute(captor.capture());
        return captor.getValue();
    }

    // Giữ nguyên hành vi cũ: nút "sao bộ tiêu chí" đang có vẫn gửi lên đúng như trước.
    @Test
    void clonesRubricOnly_whenNoPolicyIsChosen() {
        assertThat(useCase.execute(command(List.of()))).isEqualTo(CLONED_VERSION_ID);

        verify(createSchoolAssessmentPolicyUseCase, never()).execute(any());
    }

    @Test
    void copiesTemplateParametersOntoTheClonedRubricVersion() {
        UUID classId = UUID.randomUUID();

        useCase.execute(command(List.of(choice(null, null, classId))));

        var commands = capturePolicyCommands();
        assertThat(commands).hasSize(1);
        var created = commands.get(0);
        // Thông số chấm đi theo bản mẫu...
        assertThat(created.frameworkVersionId()).isEqualTo(FRAMEWORK_VERSION_ID);
        assertThat(created.languageId()).isEqualTo(LANGUAGE_ID);
        assertThat(created.targetFrameworkBandId()).isEqualTo(TARGET_BAND_ID);
        assertThat(created.passingScore()).isEqualByComparingTo("5.00");
        assertThat(created.strictness()).isEqualTo(AssessmentPolicyStrictness.STANDARD);
        // ...nhưng trỏ vào BẢN SAO, không phải phiên bản của hệ thống.
        assertThat(created.rubricVersionId()).isEqualTo(CLONED_VERSION_ID);
        assertThat(created.schoolId()).isEqualTo(SCHOOL_ID);
        assertThat(created.schoolClassId()).isEqualTo(classId);
    }

    // Lý do phạm vi phải đi theo TỪNG chính sách: hai bản mẫu khác bậc mục tiêu dùng chung một phiên
    // bản Rubric chỉ cùng tồn tại được khi chúng nằm ở hai phạm vi khác nhau.
    @Test
    void carriesADistinctScopePerChosenPolicy() {
        UUID otherTemplateId = UUID.randomUUID();
        AssessmentPolicy otherTemplate = mock(AssessmentPolicy.class);
        when(otherTemplate.getId()).thenReturn(otherTemplateId);
        when(otherTemplate.getStatus()).thenReturn(AssessmentPolicyStatus.PUBLISHED);
        when(otherTemplate.getLanguageId()).thenReturn(LANGUAGE_ID);
        when(otherTemplate.getFrameworkVersionId()).thenReturn(FRAMEWORK_VERSION_ID);
        when(otherTemplate.getTargetFrameworkBandId()).thenReturn(UUID.randomUUID());
        when(otherTemplate.getStrictness()).thenReturn(AssessmentPolicyStrictness.STRICT);
        when(assessmentPolicyRepository.findPublishedSystemWideByRubricVersionId(SOURCE_VERSION_ID))
                .thenReturn(List.of(template, otherTemplate));

        UUID gradeId = UUID.randomUUID();
        UUID specialisedClassId = UUID.randomUUID();
        useCase.execute(command(List.of(
                choice(null, gradeId, null),
                new PolicyToClone(otherTemplateId, null, null, specialisedClassId,
                        Instant.parse("2026-09-01T00:00:00Z"), null))));

        var commands = capturePolicyCommands();
        assertThat(commands).hasSize(2);
        assertThat(commands.get(0).schoolGradeId()).isEqualTo(gradeId);
        assertThat(commands.get(0).schoolClassId()).isNull();
        assertThat(commands.get(1).schoolClassId()).isEqualTo(specialisedClassId);
        assertThat(commands.get(1).schoolGradeId()).isNull();
    }

    @Test
    void inheritsTheGradeLevelWhenTheTemplatePinsOne() {
        UUID templateGradeLevelId = UUID.randomUUID();
        when(template.getGradeLevelId()).thenReturn(templateGradeLevelId);

        useCase.execute(command(List.of(choice(null, null, null))));

        assertThat(capturePolicyCommands().get(0).gradeLevelId()).isEqualTo(templateGradeLevelId);
    }

    @Test
    void rejects_whenCallerOverridesTheScopeOfATemplateThatPinsAGradeLevel() {
        when(template.getGradeLevelId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(command(List.of(choice(null, null, UUID.randomUUID())))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("giữ nguyên khối");
    }

    // Bản mẫu của phiên bản KHÁC mang thông số soạn cho bộ tiêu chí khác, nên gắn nó vào bản sao này
    // là chấm bằng một thang không khớp.
    @Test
    void rejects_whenTheChosenTemplateBelongsToAnotherRubricVersion() {
        assertThatThrownBy(() -> useCase.execute(command(List.of(
                new PolicyToClone(UUID.randomUUID(), null, null, UUID.randomUUID(),
                        Instant.parse("2026-09-01T00:00:00Z"), null)))))
                .isInstanceOf(NotFoundException.class);

        verify(createSchoolAssessmentPolicyUseCase, never()).execute(any());
    }
}
