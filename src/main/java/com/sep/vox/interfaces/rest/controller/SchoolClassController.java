package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.usecase.schooladmin.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schooladmin.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schooladmin.ImportSchoolClassesUseCase;
import com.sep.vox.application.port.input.usecase.schooladmin.UpdateSchoolClassUseCase;
import com.sep.vox.domain.dto.SchoolClassDeleteResultDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolClassImportResultDto;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolClassCommandMapper;
import com.sep.vox.interfaces.rest.mapper.ImportSchoolClassesCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateSchoolClassCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/school-classes")
public class SchoolClassController {

    private final CreateSchoolClassUseCase createSchoolClassUseCase;
    private final ImportSchoolClassesUseCase importSchoolClassesUseCase;
    private final UpdateSchoolClassUseCase updateSchoolClassUseCase;
    private final DeleteSchoolClassUseCase deleteSchoolClassUseCase;

    public SchoolClassController(
            CreateSchoolClassUseCase createSchoolClassUseCase,
            ImportSchoolClassesUseCase importSchoolClassesUseCase,
            UpdateSchoolClassUseCase updateSchoolClassUseCase,
            DeleteSchoolClassUseCase deleteSchoolClassUseCase) {
        this.createSchoolClassUseCase = createSchoolClassUseCase;
        this.importSchoolClassesUseCase = importSchoolClassesUseCase;
        this.updateSchoolClassUseCase = updateSchoolClassUseCase;
        this.deleteSchoolClassUseCase = deleteSchoolClassUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolClassDto>> create(@Valid @RequestBody CreateSchoolClassRequest request) {
        var command = CreateSchoolClassCommandMapper.fromRequest(request);
        var data = createSchoolClassUseCase.execute(command);
        var response = ApiResponse.success("Tạo lớp học thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolClassImportResultDto>> importFile(@RequestPart("file") MultipartFile file) {
        var command = ImportSchoolClassesCommandMapper.fromFile(file);
        var data = importSchoolClassesUseCase.execute(command);
        var response = ApiResponse.success("Import lớp học thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolClassDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchoolClassRequest request) {
        var command = UpdateSchoolClassCommandMapper.fromRequest(id, request);
        var data = updateSchoolClassUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolClassDeleteResultDto>> delete(@PathVariable UUID id) {
        var data = deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(id));
        var response = ApiResponse.success("X\u00f3a l\u1edbp h\u1ecdc th\u00e0nh c\u00f4ng", data);
        return ResponseEntity.ok(response);
    }
}
