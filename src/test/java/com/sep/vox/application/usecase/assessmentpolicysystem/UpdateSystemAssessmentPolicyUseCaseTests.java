package com.sep.vox.application.usecase.assessmentpolicysystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.command.assessmentpolicysystem.UpdateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.UpdateSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * An omitted passingScore, strictness or effectiveTo keeps the stored value; an explicit null clears
 * passingScore and effectiveTo and stores STANDARD for strictness.
 */
@ExtendWith(MockitoExtension.class)
class UpdateSystemAssessmentPolicyUseCaseTests {

    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID BAND_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_VERSION_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final Instant EFFECTIVE_FROM = Instant.parse("2026-07-27T17:00:00Z");
    private static final Instant STORED_EFFECTIVE_TO = Instant.parse("2027-07-27T17:00:00Z");

    @Mock
    private AssessmentPolicyRepository assessmentPolicyRepository;
    @Mock
    private FrameworkResultBandRepository frameworkResultBandRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserContextPort userContextPort;
    @Mock
    private User admin;
    @Mock
    private FrameworkResultBand band;

    @InjectMocks
    private UpdateSystemAssessmentPolicyUseCase useCase;

    private AssessmentPolicy policy;

    @BeforeEach
    void setUp() {
        policy = AssessmentPolicy.builder()
            .frameworkVersionId(FRAMEWORK_VERSION_ID)
            .status(AssessmentPolicyStatus.DRAFT)
            .passingScore(new BigDecimal("6.5"))
            .strictness(AssessmentPolicyStrictness.STRICT)
            .effectiveTo(STORED_EFFECTIVE_TO)
            .build();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(admin.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(assessmentPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
    }

    private void stubBandAndSave() {
        when(frameworkResultBandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        when(band.getFrameworkVersionId()).thenReturn(FRAMEWORK_VERSION_ID);
        when(assessmentPolicyRepository.save(any())).thenReturn(policy);
    }

    @Test
    void should_keep_stored_values_when_the_three_fields_are_omitted() {
        stubBandAndSave();

        useCase.execute(new UpdateAssessmentPolicyCommand(
            null, POLICY_ID, BAND_ID, null, false, null, false, EFFECTIVE_FROM, null, false));

        assertThat(policy.getPassingScore()).isEqualByComparingTo("6.5");
        assertThat(policy.getStrictness()).isEqualTo(AssessmentPolicyStrictness.STRICT);
        assertThat(policy.getEffectiveTo()).isEqualTo(STORED_EFFECTIVE_TO);
    }

    @Test
    void should_clear_passing_score_and_effective_to_and_store_standard_on_explicit_null() {
        stubBandAndSave();

        useCase.execute(new UpdateAssessmentPolicyCommand(
            null, POLICY_ID, BAND_ID, null, true, null, true, EFFECTIVE_FROM, null, true));

        assertThat(policy.getPassingScore()).isNull();
        assertThat(policy.getStrictness()).isEqualTo(AssessmentPolicyStrictness.STANDARD);
        assertThat(policy.getEffectiveTo()).isNull();
    }

    @Test
    void should_store_the_given_values() {
        stubBandAndSave();
        Instant newEffectiveTo = Instant.parse("2028-01-01T00:00:00Z");

        useCase.execute(new UpdateAssessmentPolicyCommand(null, POLICY_ID, BAND_ID, new BigDecimal("8"), true,
            AssessmentPolicyStrictness.LENIENT, true, EFFECTIVE_FROM, newEffectiveTo, true));

        assertThat(policy.getPassingScore()).isEqualByComparingTo("8");
        assertThat(policy.getStrictness()).isEqualTo(AssessmentPolicyStrictness.LENIENT);
        assertThat(policy.getEffectiveTo()).isEqualTo(newEffectiveTo);
    }

    @Test
    void should_compare_effective_from_with_the_stored_effective_to_when_effective_to_is_omitted() {
        when(frameworkResultBandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        when(band.getFrameworkVersionId()).thenReturn(FRAMEWORK_VERSION_ID);
        Instant afterStoredEffectiveTo = STORED_EFFECTIVE_TO.plusSeconds(1);

        assertThatThrownBy(() -> useCase.execute(new UpdateAssessmentPolicyCommand(
            null, POLICY_ID, BAND_ID, null, false, null, false, afterStoredEffectiveTo, null, false)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
