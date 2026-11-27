package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkCriterionBandsCommand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import java.util.UUID;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateFrameworkCriterionBandsRequest(
        @NotEmpty(message = "At least 1 criterion band must be submitted")
        @Valid
        List<CriterionBandItemRequest> bands
) {
    public record CriterionBandItemRequest(
            @NotBlank(message = "Result code (resultBandCode) must not be blank")
            String resultBandCode,

            String descriptor,

            @Valid
            List<SignalRequest> positiveSignals,

            @Valid
            List<SignalRequest> negativeSignals
    ) {}

    public record SignalRequest(
            @NotBlank(message = "Signal code must not be blank")
            String code,

            @NotBlank(message = "Signal description must not be blank")
            String description,

            @NotBlank(message = "Importance must not be blank")
            String importance,

            String evidenceHint
    ) {}

    public static CreateFrameworkCriterionBandsCommand toCommand(
            UUID frameworkId, UUID versionId, UUID criterionId, CreateFrameworkCriterionBandsRequest request) {
        List<CreateFrameworkCriterionBandsCommand.CriterionBandItemCommand> bands = request.bands().stream()
                .map(CreateFrameworkCriterionBandsRequest::toBandItem)
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
