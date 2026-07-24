package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateFrameworkCriteriaCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkCriteriaRequest;

public final class CreateFrameworkCriteriaCommandMapper {

    private CreateFrameworkCriteriaCommandMapper() {
    }

    public static CreateFrameworkCriteriaCommand fromRequest(
            UUID frameworkId, UUID versionId, CreateFrameworkCriteriaRequest request) {
        List<CreateFrameworkCriteriaCommand.CriterionItemCommand> criteria = request.criteria().stream()
                .map(item -> new CreateFrameworkCriteriaCommand.CriterionItemCommand(
                        item.code(),
                        item.name(),
                        item.description(),
                        item.order()))
                .toList();
        return new CreateFrameworkCriteriaCommand(frameworkId, versionId, criteria);
    }
}
