package com.sep.vox.interfaces.rest.dto.request.schoolroom;

import com.sep.vox.application.port.input.command.schoolroom.AddSchoolRoomCommand;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record AddSchoolRoomRequest(

        @NotBlank(message = "Room code must not be blank")
        @Size(max = 50, message = "Room code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Room name must not be blank")
        @Size(max = 100, message = "Room name must not exceed 100 characters")
        String name,

        String description
) {

    public static AddSchoolRoomCommand toCommand(UUID schoolId, AddSchoolRoomRequest request) {
        return new AddSchoolRoomCommand(
                schoolId,
                request.code(),
                request.name(),
                request.description()
        );
    }
}
