package com.sep.vox.interfaces.rest.dto.request.rubricsystem;

import com.sep.vox.application.port.input.command.rubricsystem.CreateSystemRubricResultBandsCommand;
import java.util.stream.Collectors;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateSystemRubricResultBandsRequest(
        @NotEmpty(message = "At least 1 score band must be submitted")
        @Valid
        List<ResultBandItemRequest> resultBands
) {
    public record ResultBandItemRequest(
            @NotBlank(message = "Score band code must not be blank")
            String code,

            @NotBlank(message = "Score band name (e.g. Excellent, Good, Pass) must not be blank")
            String name,

            String description,

            @NotNull(message = "Minimum converted score must not be blank")
            BigDecimal mappedScoreMin,

            @NotNull(message = "Maximum converted score must not be blank")
            BigDecimal mappedScoreMax,

            @NotNull(message = "Order must not be blank")
            @Min(value = 1, message = "Order must be greater than 0")
            Integer order
    ) {}

    public static CreateSystemRubricResultBandsCommand toCommand(UUID versionId, CreateSystemRubricResultBandsRequest request) {

        List<CreateSystemRubricResultBandsCommand.ResultBandItemCommand> bandCommands = request.resultBands()
                .stream()
                .map(b -> new CreateSystemRubricResultBandsCommand.ResultBandItemCommand(
                        b.code(),
                        b.name(),
                        b.description(),
                        b.mappedScoreMin(),
                        b.mappedScoreMax(),
                        b.order()
                ))
                .collect(Collectors.toList());

        return new CreateSystemRubricResultBandsCommand(versionId, bandCommands);
    }
}
