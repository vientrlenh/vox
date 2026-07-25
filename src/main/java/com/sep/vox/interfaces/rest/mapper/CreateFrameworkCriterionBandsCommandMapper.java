package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateFrameworkCriterionBandsCommand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkCriterionBandsRequest;

public final class CreateFrameworkCriterionBandsCommandMapper {

    private CreateFrameworkCriterionBandsCommandMapper() {
    }

    public static CreateFrameworkCriterionBandsCommand fromRequest(
            UUID frameworkId, UUID versionId, UUID criterionId, CreateFrameworkCriterionBandsRequest request) {
        List<CreateFrameworkCriterionBandsCommand.CriterionBandItemCommand> bands = request.bands().stream()
                .map(CreateFrameworkCriterionBandsCommandMapper::toBandItem)
                .toList();
        return new CreateFrameworkCriterionBandsCommand(frameworkId, versionId, criterionId, bands);
    }

    private static CreateFrameworkCriterionBandsCommand.CriterionBandItemCommand toBandItem(
            CreateFrameworkCriterionBandsRequest.CriterionBandItemRequest item) {
        return new CreateFrameworkCriterionBandsCommand.CriterionBandItemCommand(
                item.resultBandCode(),
                item.descriptor(),
                toSignals(item.positiveSignals()),
                toSignals(item.negativeSignals()));
    }

    private static FrameworkCriterionSignals toSignals(List<CreateFrameworkCriterionBandsRequest.SignalRequest> raw) {
        if (raw == null) return new FrameworkCriterionSignals(List.of());
        return new FrameworkCriterionSignals(raw.stream()
                .map(s -> new FrameworkCriterionSignal(
                        s.code(),
                        s.description(),
                        FrameworkCriterionSignalImportance.valueOf(s.importance()),
                        s.evidenceHint()))
                .toList());
    }
}
