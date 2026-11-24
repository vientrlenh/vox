package com.sep.vox.application.usecase.scoringrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.mapper.scoringrule.ScoringRuleParamsMapper;
import com.sep.vox.application.port.input.command.scoringrule.UpdateScoringRuleCommand;
import com.sep.vox.application.port.input.usecase.scoringrule.UpdateSystemScoringRuleUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.scoringrule.ScoringRule;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.ScoringRuleRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.scoringruleaction.CapFinalScoreParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.DurationThresholdParams;

/** An omitted description keeps the stored one; an explicit null clears it; a value replaces it. */
@ExtendWith(MockitoExtension.class)
class UpdateSystemScoringRuleUseCaseTests {

    private static final UUID RULE_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final String STORED_DESCRIPTION = "Stored description";

    @Mock
    private ScoringRuleRepository scoringRuleRepository;
    @Mock
    private AssessmentPolicyRepository assessmentPolicyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserContextPort userContextPort;
    @Mock
    private RubricCriterionRepository rubricCriterionRepository;
    @Mock
    private FrameworkResultBandRepository frameworkResultBandRepository;
    @Mock
    private ScoringRuleParamsMapper scoringRuleParamsMapper;
    @Mock
    private User admin;

    @InjectMocks
    private UpdateSystemScoringRuleUseCase useCase;

    private ScoringRule rule;

    @BeforeEach
    void setUp() {
        rule = ScoringRule.builder().policyId(POLICY_ID).name("Rule").description(STORED_DESCRIPTION).build();
        AssessmentPolicy policy = AssessmentPolicy.builder().status(AssessmentPolicyStatus.DRAFT).build();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(admin.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(scoringRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(assessmentPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));
        when(scoringRuleParamsMapper.toConditionParams(any(), any())).thenReturn(new DurationThresholdParams(3));
        when(scoringRuleParamsMapper.toActionParams(any(), any())).thenReturn(new CapFinalScoreParams(BigDecimal.ZERO));
        when(scoringRuleRepository.save(any())).thenReturn(rule);
    }

    private UpdateScoringRuleCommand command(String description, boolean descriptionProvided) {
        return new UpdateScoringRuleCommand(RULE_ID, POLICY_ID, "Renamed", description, descriptionProvided,
            ScoringRuleConditionType.DURATION_LESS_THAN, Map.of("seconds", 3),
            ScoringRuleActionType.CAP_FINAL_SCORE, Map.of("maxScore", 0), 1, ScoringRuleSeverity.WARNING, false, true);
    }

    @Test
    void should_keep_the_description_when_it_is_omitted() {
        useCase.execute(command(null, false));

        assertThat(rule.getDescription()).isEqualTo(STORED_DESCRIPTION);
        assertThat(rule.getName()).isEqualTo("Renamed");
    }

    @Test
    void should_clear_the_description_on_explicit_null() {
        useCase.execute(command(null, true));

        assertThat(rule.getDescription()).isNull();
    }

    @Test
    void should_replace_the_description_when_a_value_is_given() {
        useCase.execute(command("New description", true));

        assertThat(rule.getDescription()).isEqualTo("New description");
    }
}
