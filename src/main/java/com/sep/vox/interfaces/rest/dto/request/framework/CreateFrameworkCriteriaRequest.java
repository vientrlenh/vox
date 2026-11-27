package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkCriteriaCommand;
import java.util.UUID;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateFrameworkCriteriaRequest(
        @NotEmpty(message = "At least 1 criterion must be submitted")
        @Valid
        List<CriterionItemRequest> criteria
) {
    public record CriterionItemRequest(
            @NotBlank(message = "Criterion code must not be blank")
            String code,

            @NotBlank(message = "Criterion name must not be blank")
            String name,

            String description,

            int order
    ) {}

    public static CreateFrameworkCriteriaCommand toCommand(
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
