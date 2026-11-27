package com.sep.vox.domain.model.scoringrule;

import static org.assertj.core.api.Assertions.assertThat;

import com.sep.vox.domain.valueobject.scoringruleaction.CapFinalScoreParams;
import com.sep.vox.domain.valueobject.scoringruleaction.ScoringRuleActionParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.DurationThresholdParams;
import com.sep.vox.domain.valueobject.scoringrulecondition.ScoringRuleConditionParams;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScoringRuleTests {

    @Test
    void forPolicySetsEveryArgument() {
        UUID policyId = UUID.randomUUID();
        ScoringRuleConditionParams conditionParams = new DurationThresholdParams(30);
        ScoringRuleActionParams actionParams = new CapFinalScoreParams(BigDecimal.TEN);

        ScoringRule rule = ScoringRule.forPolicy(policyId, "SHORT", "Short answer", "too short",
                ScoringRuleConditionType.DURATION_LESS_THAN, conditionParams,
                ScoringRuleActionType.CAP_FINAL_SCORE, actionParams,
                5, ScoringRuleSeverity.WARNING, true, false);

        assertThat(rule.getId()).isNull();
        assertThat(rule.getPolicyId()).isEqualTo(policyId);
        assertThat(rule.getCode()).isEqualTo("SHORT");
        assertThat(rule.getName()).isEqualTo("Short answer");
        assertThat(rule.getDescription()).isEqualTo("too short");
        assertThat(rule.getConditionType()).isEqualTo(ScoringRuleConditionType.DURATION_LESS_THAN);
        assertThat(rule.getConditionParams()).isSameAs(conditionParams);
        assertThat(rule.getActionType()).isEqualTo(ScoringRuleActionType.CAP_FINAL_SCORE);
        assertThat(rule.getActionParams()).isSameAs(actionParams);
        assertThat(rule.getPriority()).isEqualTo(5);
        assertThat(rule.getSeverity()).isEqualTo(ScoringRuleSeverity.WARNING);
        assertThat(rule.isStopProcessing()).isTrue();
        assertThat(rule.isActive()).isFalse();
    }
}
