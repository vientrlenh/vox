package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.schooladmin.CreateSchoolClassUseCase;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolClassCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/school-classes")
public class SchoolClassController {

    private final CreateSchoolClassUseCase createSchoolClassUseCase;

    public SchoolClassController(CreateSchoolClassUseCase createSchoolClassUseCase) {
        this.createSchoolClassUseCase = createSchoolClassUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolClassDto>> create(@Valid @RequestBody CreateSchoolClassRequest request) {
        var command = CreateSchoolClassCommandMapper.fromRequest(request);
        var data = createSchoolClassUseCase.execute(command);
        var response = ApiResponse.success("Tạo lớp học thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
