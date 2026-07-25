package com.sep.vox.interfaces.graphql.mapper;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.port.input.command.UpdateAssessmentPolicyCommand;
import com.sep.vox.interfaces.graphql.dto.request.UpdateAssessmentPolicyInput;

import java.util.UUID;

public class UpdateAssessmentPolicyGraphQLMapper {

    public static UpdateAssessmentPolicyCommand fromSystemInput(UUID policyId, UpdateAssessmentPolicyInput input) {
        return new UpdateAssessmentPolicyCommand(
                null,
                policyId,
                input.targetFrameworkBandId(),
                input.passingScore(),
                input.strictness(),
                DateMapper.toOffsetDateTime(input.effectiveFrom()),
                DateMapper.toOffsetDateTime(input.effectiveTo())
        );
    }

    public static UpdateAssessmentPolicyCommand fromSchoolInput(UUID schoolId, UUID policyId, UpdateAssessmentPolicyInput input) {
        return new UpdateAssessmentPolicyCommand(
                schoolId,
                policyId,
                input.targetFrameworkBandId(),
                input.passingScore(),
                input.strictness(),
                DateMapper.toOffsetDateTime(input.effectiveFrom()),
                DateMapper.toOffsetDateTime(input.effectiveTo())
        );
    }
}
