package com.sep.vox.interfaces.graphql.input.assessmentpolicysystem;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.assessmentpolicysystem.UpdateAssessmentPolicyCommand;

public record UpdateAssessmentPolicyInput(
        UUID targetFrameworkBandId,
        ArgumentValue<BigDecimal> passingScore,
        ArgumentValue<AssessmentPolicyStrictness> strictness,
        Instant effectiveFrom,
        ArgumentValue<Instant> effectiveTo
) {

    public static UpdateAssessmentPolicyCommand toUpdateAssessmentPolicyCommand(UUID policyId, UpdateAssessmentPolicyInput input) {
        return new UpdateAssessmentPolicyCommand(
                null,
                policyId,
                input.targetFrameworkBandId(),
                input.passingScore().value(), !input.passingScore().isOmitted(),
                input.strictness().value(), !input.strictness().isOmitted(),
                input.effectiveFrom(),
                input.effectiveTo().value(), !input.effectiveTo().isOmitted()
        );
    }

    public static UpdateAssessmentPolicyCommand toUpdateAssessmentPolicyCommand(UUID schoolId, UUID policyId, UpdateAssessmentPolicyInput input) {
        return new UpdateAssessmentPolicyCommand(
                schoolId,
                policyId,
                input.targetFrameworkBandId(),
                input.passingScore().value(), !input.passingScore().isOmitted(),
                input.strictness().value(), !input.strictness().isOmitted(),
                input.effectiveFrom(),
                input.effectiveTo().value(), !input.effectiveTo().isOmitted()
        );
    }
}
