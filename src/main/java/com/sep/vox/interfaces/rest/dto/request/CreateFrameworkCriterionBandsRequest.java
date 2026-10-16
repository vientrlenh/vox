package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateFrameworkCriterionBandsCommand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateFrameworkCriterionBandsRequest(
        @NotEmpty(message = "Phải gửi lên ít nhất 1 mức đánh giá")
        @Valid
        List<CriterionBandItemRequest> bands
) {
    public record CriterionBandItemRequest(
            @NotBlank(message = "Mã kết quả (resultBandCode) không được để trống")
            String resultBandCode,

            String descriptor,

            @Valid
            List<SignalRequest> positiveSignals,

            @Valid
            List<SignalRequest> negativeSignals
    ) {}

    public record SignalRequest(
            @NotBlank(message = "Mã dấu hiệu không được để trống")
            String code,

            @NotBlank(message = "Mô tả dấu hiệu không được để trống")
            String description,

            @NotBlank(message = "Độ quan trọng không được để trống")
            String importance,

            String evidenceHint
    ) {}

    public static CreateFrameworkCriterionBandsCommand toCommand(UUID frameworkId, UUID versionId, UUID criterionId, CreateFrameworkCriterionBandsRequest request) {
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
