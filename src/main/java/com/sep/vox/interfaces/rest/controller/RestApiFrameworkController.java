package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkVersionUseCase;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.interfaces.rest.dto.request.framework.CreateFrameworkVersionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/frameworks/{frameworkId}")
@RequiredArgsConstructor 
public class RestApiFrameworkController {

    private final CreateFrameworkVersionUseCase createFrameworkVersionUseCase;

    @PostMapping("/version")
    public ResponseEntity<ApiResponse<CreateFrameworkVersionResponse>> createFrameworkVersion(@PathVariable(name = "frameworkId") UUID frameworkId, @Valid @RequestBody CreateFrameworkVersionRequest request) {
        CreateFrameworkVersionCommand command = CreateFrameworkVersionRequest.toCommand(frameworkId, request);
        CreateFrameworkVersionResponse data = createFrameworkVersionUseCase.execute(command);
        ApiResponse<CreateFrameworkVersionResponse> response = ApiResponse.success("Framework version created successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
