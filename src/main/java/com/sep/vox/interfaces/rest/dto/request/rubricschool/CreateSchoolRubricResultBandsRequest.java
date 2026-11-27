package com.sep.vox.interfaces.rest.dto.request.rubricschool;

import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricResultBandsCommand;
import java.util.stream.Collectors;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateSchoolRubricResultBandsRequest(
        @NotEmpty(message = "At least 1 score band must be submitted")
        @Valid // Required so that Spring validates the elements inside the List
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

    public static CreateSchoolRubricResultBandsCommand toCommand(UUID schoolId, UUID versionId, CreateSchoolRubricResultBandsRequest request) {

        List<CreateSchoolRubricResultBandsCommand.ResultBandItemCommand> bandCommands = request.resultBands()
                .stream()
                .map(b -> new CreateSchoolRubricResultBandsCommand.ResultBandItemCommand(
                        b.code(),
                        b.name(),
                        b.description(),
                        b.mappedScoreMin(),
                        b.mappedScoreMax(),
                        b.order()
                ))
                .collect(Collectors.toList());

        return new CreateSchoolRubricResultBandsCommand(schoolId, versionId, bandCommands);
    }
}
