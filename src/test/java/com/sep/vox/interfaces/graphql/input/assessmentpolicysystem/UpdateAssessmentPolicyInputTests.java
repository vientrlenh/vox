package com.sep.vox.interfaces.graphql.input.assessmentpolicysystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.assessmentpolicysystem.UpdateAssessmentPolicyCommand;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

class UpdateAssessmentPolicyInputTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID policyId = UUID.randomUUID();
    private final UUID targetBandId = UUID.randomUUID();
    private final Instant effectiveFrom = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void should_mark_every_omitted_field_as_not_provided_for_both_owners() {
        UpdateAssessmentPolicyInput input = new UpdateAssessmentPolicyInput(targetBandId,
            ArgumentValue.omitted(), ArgumentValue.omitted(), effectiveFrom, ArgumentValue.omitted());

        assertThat(UpdateAssessmentPolicyInput.toUpdateAssessmentPolicyCommand(policyId, input))
            .isEqualTo(new UpdateAssessmentPolicyCommand(null, policyId, targetBandId,
                null, false, null, false, effectiveFrom, null, false));
        assertThat(UpdateAssessmentPolicyInput.toUpdateAssessmentPolicyCommand(schoolId, policyId, input))
            .isEqualTo(new UpdateAssessmentPolicyCommand(schoolId, policyId, targetBandId,
                null, false, null, false, effectiveFrom, null, false));
    }

    @Test
    void should_mark_every_explicit_null_as_provided_for_both_owners() {
        UpdateAssessmentPolicyInput input = new UpdateAssessmentPolicyInput(targetBandId,
            ArgumentValue.ofNullable(null), ArgumentValue.ofNullable(null), effectiveFrom,
            ArgumentValue.ofNullable(null));

        assertThat(UpdateAssessmentPolicyInput.toUpdateAssessmentPolicyCommand(policyId, input))
            .isEqualTo(new UpdateAssessmentPolicyCommand(null, policyId, targetBandId,
                null, true, null, true, effectiveFrom, null, true));
        assertThat(UpdateAssessmentPolicyInput.toUpdateAssessmentPolicyCommand(schoolId, policyId, input))
            .isEqualTo(new UpdateAssessmentPolicyCommand(schoolId, policyId, targetBandId,
                null, true, null, true, effectiveFrom, null, true));
    }

    @Test
    void should_mark_every_sent_value_as_provided_for_both_owners() {
        BigDecimal passingScore = new BigDecimal("5.5");
        Instant effectiveTo = Instant.parse("2026-12-31T00:00:00Z");
        UpdateAssessmentPolicyInput input = new UpdateAssessmentPolicyInput(targetBandId,
            ArgumentValue.ofNullable(passingScore), ArgumentValue.ofNullable(AssessmentPolicyStrictness.STANDARD),
            effectiveFrom, ArgumentValue.ofNullable(effectiveTo));

        assertThat(UpdateAssessmentPolicyInput.toUpdateAssessmentPolicyCommand(policyId, input))
            .isEqualTo(new UpdateAssessmentPolicyCommand(null, policyId, targetBandId, passingScore, true,
                AssessmentPolicyStrictness.STANDARD, true, effectiveFrom, effectiveTo, true));
        assertThat(UpdateAssessmentPolicyInput.toUpdateAssessmentPolicyCommand(schoolId, policyId, input))
            .isEqualTo(new UpdateAssessmentPolicyCommand(schoolId, policyId, targetBandId, passingScore, true,
                AssessmentPolicyStrictness.STANDARD, true, effectiveFrom, effectiveTo, true));
    }
}
