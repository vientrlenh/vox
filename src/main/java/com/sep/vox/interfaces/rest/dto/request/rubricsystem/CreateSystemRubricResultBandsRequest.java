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
        @NotEmpty(message = "Phải có ít nhất 1 thang điểm được gửi lên")
        @Valid
        List<ResultBandItemRequest> resultBands
) {
    public record ResultBandItemRequest(
            @NotBlank(message = "Mã thang điểm không được để trống")
            String code,

            @NotBlank(message = "Tên thang điểm (VD: Giỏi, Khá, Pass) không được để trống")
            String name,

            String description,

            @NotNull(message = "Điểm quy đổi tối thiểu không được để trống")
            BigDecimal mappedScoreMin,

            @NotNull(message = "Điểm quy đổi tối đa không được để trống")
            BigDecimal mappedScoreMax,

            @NotNull(message = "Thứ tự không được để trống")
            @Min(value = 1, message = "Thứ tự (Order) phải lớn hơn 0")
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
