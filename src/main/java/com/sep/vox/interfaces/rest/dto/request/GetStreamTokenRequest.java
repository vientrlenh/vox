package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;


public record GetStreamTokenRequest(
    @NotNull(message = "Id của danh sách phòng không được để trống")
    List<UUID> roomIds,

    UUID examSessionId,

    @NotNull(message = "Id của kỳ thi không được để trống")
    UUID examId, 

    @NotNull(message = "Loại stream không được để trống")
    List<String> streamTypes
) {
    
}
