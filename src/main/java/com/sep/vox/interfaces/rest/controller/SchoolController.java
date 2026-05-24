package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.systemadmin.CreateSchoolUseCase;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolCommandMapper;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schools")
public class SchoolController {

    private final CreateSchoolUseCase createSchoolUseCase;

    public SchoolController(CreateSchoolUseCase createSchoolUseCase) {
        this.createSchoolUseCase = createSchoolUseCase;
    }
    
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        var command = CreateSchoolCommandMapper.fromRequest(request);
        createSchoolUseCase.execute(command);
        var response = ApiResponse.success("Trường học đã tạo thành công");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
