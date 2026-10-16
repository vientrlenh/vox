package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateFrameworkResultBandsCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateFrameworkResultBandsRequest(
        @NotEmpty(message = "Phải gửi lên ít nhất 1 mức kết quả")
        @Valid
        List<ResultBandItemRequest> bands
) {
    public record ResultBandItemRequest(
            @NotBlank(message = "Mã kết quả không được để trống")
            String code,

            @NotBlank(message = "Nhãn kết quả không được để trống")
            String label,

            String description,

            int order
    ) {}

    public static CreateFrameworkResultBandsCommand toCommand(UUID frameworkId, UUID versionId, CreateFrameworkResultBandsRequest request) {
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
