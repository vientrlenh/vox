package com.sep.vox.application.usecase.assessmentpolicyschool;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.UpdateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * An omitted passingScore, strictness or effectiveTo keeps the stored value; an explicit null clears
 * passingScore and effectiveTo and stores STANDARD for strictness.
 */
@ExtendWith(MockitoExtension.class)
class UpdateSchoolAssessmentPolicyUseCaseTests {

    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
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
    private SchoolRepository schoolRepository;
    @Mock
    private SchoolUserRepository schoolUserRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserContextPort userContextPort;
    @Mock
    private GradeLevelBandScopeGuardService gradeLevelBandScopeGuardService;
    @Mock
    private User admin;
    @Mock
    private SchoolUser schoolUser;
    @Mock
    private School school;
    @Mock
    private FrameworkResultBand band;

    @InjectMocks
    private UpdateSchoolAssessmentPolicyUseCase useCase;

    private AssessmentPolicy policy;

    @BeforeEach
    void setUp() {
        policy = AssessmentPolicy.builder()
            .schoolId(SCHOOL_ID)
            .frameworkVersionId(FRAMEWORK_VERSION_ID)
            .status(AssessmentPolicyStatus.DRAFT)
            .passingScore(new BigDecimal("6.5"))
            .strictness(AssessmentPolicyStrictness.STRICT)
            .effectiveTo(STORED_EFFECTIVE_TO)
            .build();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(admin.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(schoolUserRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(schoolUser));
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolRepository.findById(SCHOOL_ID)).thenReturn(Optional.of(school));
        when(school.isActive()).thenReturn(true);
        when(assessmentPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(frameworkResultBandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        when(band.getFrameworkVersionId()).thenReturn(FRAMEWORK_VERSION_ID);
        when(assessmentPolicyRepository.save(any())).thenReturn(policy);
    }

    @Test
    void should_keep_stored_values_when_the_three_fields_are_omitted() {
        useCase.execute(new UpdateAssessmentPolicyCommand(
            SCHOOL_ID, POLICY_ID, BAND_ID, null, false, null, false, EFFECTIVE_FROM, null, false));

        assertThat(policy.getPassingScore()).isEqualByComparingTo("6.5");
        assertThat(policy.getStrictness()).isEqualTo(AssessmentPolicyStrictness.STRICT);
        assertThat(policy.getEffectiveTo()).isEqualTo(STORED_EFFECTIVE_TO);
    }

    @Test
    void should_clear_passing_score_and_effective_to_and_store_standard_on_explicit_null() {
        useCase.execute(new UpdateAssessmentPolicyCommand(
            SCHOOL_ID, POLICY_ID, BAND_ID, null, true, null, true, EFFECTIVE_FROM, null, true));

        assertThat(policy.getPassingScore()).isNull();
        assertThat(policy.getStrictness()).isEqualTo(AssessmentPolicyStrictness.STANDARD);
        assertThat(policy.getEffectiveTo()).isNull();
    }
}
