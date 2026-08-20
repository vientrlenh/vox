package com.sep.vox.application.usecase.assessmentpolicysystem;

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
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.CreateSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * 1 Rubric Version chỉ được gắn với đúng 1 Assessment Policy -- trước đây không có gì chặn việc cùng
 * 1 Rubric Version bị 2 Policy khác scope (ngôn ngữ/framework) dùng chung.
 */
class CreateSystemAssessmentPolicyUseCaseTests {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_VERSION_ID = UUID.randomUUID();
    private static final UUID RUBRIC_ID = UUID.randomUUID();
    private static final UUID RUBRIC_VERSION_ID = UUID.randomUUID();
    private static final UUID TARGET_BAND_ID = UUID.randomUUID();

    private AssessmentPolicyRepository assessmentPolicyRepository;
    private RubricVersionRepository rubricVersionRepository;
    private RubricRepository rubricRepository;
    private CreateSystemAssessmentPolicyUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        var frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        var frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        rubricRepository = mock(RubricRepository.class);
        var languageRepository = mock(SupportedLanguageRepository.class);
        var userRepository = mock(UserRepository.class);
        var userContextPort = mock(UserContextPort.class);

        useCase = new CreateSystemAssessmentPolicyUseCase(
                assessmentPolicyRepository, frameworkVersionRepository, frameworkResultBandRepository,
                rubricVersionRepository, rubricRepository, languageRepository, userRepository, userContextPort);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        User currentUser = mock(User.class);
        when(currentUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(currentUser));

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
        when(rubric.getOwnerType()).thenReturn(RubricOwnerType.SYSTEM);
        when(rubric.getFrameworkId()).thenReturn(FRAMEWORK_ID);
        when(rubricRepository.findById(RUBRIC_ID)).thenReturn(Optional.of(rubric));

        when(assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(assessmentPolicyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateAssessmentPolicyCommand command(UUID languageId) {
        return new CreateAssessmentPolicyCommand(
                null, FRAMEWORK_VERSION_ID, RUBRIC_VERSION_ID, languageId,
                null, null, null, TARGET_BAND_ID, null, null,
                Instant.now(), null);
    }

    @Test
    void rejects_whenRubricVersionAlreadyUsedByAnotherPolicy() {
        when(assessmentPolicyRepository.existsByRubricVersionId(RUBRIC_VERSION_ID)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(List.of(command(UUID.randomUUID()))))
                .isInstanceOf(DuplicatedException.class)
                .hasMessageContaining("đã gắn với một Assessment Policy khác");
    }

    @Test
    void rejects_whenSameBatchReusesRubricVersionAcrossDifferentScopes() {
        when(assessmentPolicyRepository.existsByRubricVersionId(RUBRIC_VERSION_ID)).thenReturn(false);
        when(assessmentPolicyRepository.findMaxVersionForScope(any(), any(), any(), any(), any(), any())).thenReturn(0);

        List<CreateAssessmentPolicyCommand> commands = List.of(
                command(UUID.randomUUID()),
                command(UUID.randomUUID()) // ngôn ngữ khác -> scope khác, nhưng cùng rubricVersionId
        );

        assertThatThrownBy(() -> useCase.execute(commands))
                .isInstanceOf(DuplicatedException.class)
                .hasMessageContaining("cùng dùng 1 Phiên bản Rubric");
    }

    @Test
    void succeeds_whenRubricVersionNotYetUsed() {
        when(assessmentPolicyRepository.existsByRubricVersionId(RUBRIC_VERSION_ID)).thenReturn(false);
        when(assessmentPolicyRepository.findMaxVersionForScope(any(), any(), any(), any(), any(), any())).thenReturn(0);

        List<UUID> ids = useCase.execute(List.of(command(UUID.randomUUID())));

        org.assertj.core.api.Assertions.assertThat(ids).hasSize(1);
    }
}
