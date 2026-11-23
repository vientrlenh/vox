package com.sep.vox.interfaces.graphql.dto.request.assessmentpolicysystem;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.UpdateAssessmentPolicyCommand;

public record UpdateAssessmentPolicyInput(
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        AssessmentPolicyStrictness strictness,
        Instant effectiveFrom,
        Instant effectiveTo
) {

    public static UpdateAssessmentPolicyCommand toUpdateAssessmentPolicyCommand(UUID policyId, UpdateAssessmentPolicyInput input) {
        return new UpdateAssessmentPolicyCommand(
                null,
                policyId,
                input.targetFrameworkBandId(),
                input.passingScore(),
                input.strictness(),
                input.effectiveFrom(),
                input.effectiveTo()
        );
    }

    public static UpdateAssessmentPolicyCommand toUpdateAssessmentPolicyCommand(UUID schoolId, UUID policyId, UpdateAssessmentPolicyInput input) {
        return new UpdateAssessmentPolicyCommand(
                schoolId,
                policyId,
                input.targetFrameworkBandId(),
                input.passingScore(),
                input.strictness(),
                input.effectiveFrom(),
                input.effectiveTo()
        );
    }
}
