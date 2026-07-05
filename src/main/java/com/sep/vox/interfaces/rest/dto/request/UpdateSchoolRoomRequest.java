package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateSchoolRoomRequest(

        @Size(max = 100, message = "Tên phòng không được vượt quá 100 ký tự")
        String name,

        String description,

        @Min(value = 1, message = "Sức chứa phòng phải lớn hơn hoặc bằng 1")
        Integer capacity
) { }
