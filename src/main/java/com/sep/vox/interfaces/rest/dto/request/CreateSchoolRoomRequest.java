package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateSchoolRoomCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record CreateSchoolRoomRequest(
        @NotBlank(message = "School room code is required")
        @Size(max = 100, message = "School room code must not exceed 100 characters")
        String code,

        @NotBlank(message = "School room name is required")
        @Size(max = 255, message = "School room name must not exceed 255 characters")
        String name,

        @Size(max = 2048, message = "School room description must not exceed 2048 characters")
        String description
) {

        public static CreateSchoolRoomCommand toCommand(UUID schoolId, CreateSchoolRoomRequest request) {
                return new CreateSchoolRoomCommand(
                        schoolId, 
                        request.code, 
                        request.name, 
                        request.description
                );
        }
 }