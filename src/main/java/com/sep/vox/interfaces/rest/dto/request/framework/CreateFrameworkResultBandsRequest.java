package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkResultBandsCommand;
import java.util.UUID;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateFrameworkResultBandsRequest(
        @NotEmpty(message = "At least 1 result band must be submitted")
        @Valid
        List<ResultBandItemRequest> bands
) {
    public record ResultBandItemRequest(
            @NotBlank(message = "Result code must not be blank")
            String code,

            @NotBlank(message = "Result label must not be blank")
            String label,

            String description,

            int order
    ) {}

    public static CreateFrameworkResultBandsCommand toCommand(
            UUID frameworkId, UUID versionId, CreateFrameworkResultBandsRequest request) {
        List<CreateFrameworkResultBandsCommand.ResultBandItemCommand> bands = request.bands().stream()
                .map(CreateFrameworkResultBandsRequest::toBandItem)
                .toList();
        return new CreateFrameworkResultBandsCommand(frameworkId, versionId, bands);
    }

    private static CreateFrameworkResultBandsCommand.ResultBandItemCommand toBandItem(
            CreateFrameworkResultBandsRequest.ResultBandItemRequest item) {
        return new CreateFrameworkResultBandsCommand.ResultBandItemCommand(
                item.code(),
                item.label(),
                item.description(),
                item.order());
    }
}
