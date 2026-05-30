package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Size;

public record ImportFieldMappingRequest(
    @Size(max = 255, message = "Tên cột không được vượt quá 255 ký tự")
    String column,

    Integer index,

    @Size(max = 255, message = "Path không được vượt quá 255 ký tự")
    String path,

    @Size(max = 50, message = "Định dạng ngày không được vượt quá 50 ký tự")
    String dateFormat
) {
}
