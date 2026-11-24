package com.sep.vox.interfaces.rest.dto.request.framework;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkVersionCommand;
import java.util.UUID;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateFrameworkVersionRequest(
    @NotBlank(message = "Mã phiên bản không được để trống")
    @Size(max = 100, message = "Mã phiên bản không được vượt quá 100 ký tự")
    String code,

    @NotBlank(message = "Tên phiên bản không được để trống")
    @Size(max = 255, message = "Tên phiên bản không được vượt quá 255 ký tự")
    String name,

    @Size(max = 2048, message = "Mô tả không được vượt quá 2048 ký tự")
    String description,

    @Min(value = 1, message = "Số phiên bản phải lớn hơn 0")
    int version,

    @NotNull(message = "Ngày hiệu lực không được để trống")
    Instant effectiveFrom,

    Instant effectiveTo
) {

    public static CreateFrameworkVersionCommand toCommand(UUID frameworkId, CreateFrameworkVersionRequest request) {
        return new CreateFrameworkVersionCommand(
            frameworkId,
            request.code(),
            request.name(),
            request.description(),
            request.version(),
            request.effectiveFrom(),
            request.effectiveTo()
        );
    }
}
