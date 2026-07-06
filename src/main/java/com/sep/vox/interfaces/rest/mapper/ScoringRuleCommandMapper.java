package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.CreateScoringRuleCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateScoringRuleRequest;

public final class ScoringRuleCommandMapper {

    private ScoringRuleCommandMapper() {
    }

    public static CreateScoringRuleCommand fromCreateRequest(UUID policyId, CreateScoringRuleRequest request) {
        return new CreateScoringRuleCommand(
                policyId,
                request.code(),
                request.name(),
                request.description(),
                request.conditionType(),
                request.conditionParams(),
                request.actionType(),
                request.actionParams(),
                request.priority(),
                request.severity(),
                request.stopProcessing(),
                request.isActive()
        );
    }

    public static CreateSchoolScoringRuleCommand fromCreateSchoolRequest(UUID schoolId, UUID policyId, CreateScoringRuleRequest request) {
        return new CreateSchoolScoringRuleCommand(
                schoolId,
                policyId,
                request.code(),
                request.name(),
                request.description(),
                request.conditionType(),
                request.conditionParams(),
                request.actionType(),
                request.actionParams(),
                request.priority(),
                request.severity(),
                request.stopProcessing(),
                request.isActive()
        );
    }
}