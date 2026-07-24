package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateFrameworkResultBandsCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkResultBandsRequest;

public final class CreateFrameworkResultBandsCommandMapper {

    private CreateFrameworkResultBandsCommandMapper() {
    }

    public static CreateFrameworkResultBandsCommand fromRequest(
            UUID frameworkId, UUID versionId, CreateFrameworkResultBandsRequest request) {
        List<CreateFrameworkResultBandsCommand.ResultBandItemCommand> bands = request.bands().stream()
                .map(CreateFrameworkResultBandsCommandMapper::toBandItem)
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
