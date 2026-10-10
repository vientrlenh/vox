package com.sep.vox.interfaces.rest.controller.school;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.CreateSchoolRoomCommand;
import com.sep.vox.application.response.input.schoolroom.CreateSchoolRoomResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/schools/{schoolId}/rooms")
@RequiredArgsConstructor 
public class RestApiSchoolRoomController {

    private final CreateSchoolRoomUseCase createSchoolRoomUseCase;
    
    public ResponseEntity<ApiResponse<CreateSchoolRoomResponse>> createRoom(@PathVariable(name = "schoolId") UUID schoolId, @Valid @RequestBody CreateSchoolRoomRequest request) {
        CreateSchoolRoomCommand command = CreateSchoolRoomRequest.toCommand(schoolId, request);
        CreateSchoolRoomResponse data = createSchoolRoomUseCase.execute(command);
        ApiResponse<CreateSchoolRoomResponse> response = ApiResponse.success("School room created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
