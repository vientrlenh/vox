package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import com.sep.vox.application.port.input.usecase.schooluser.ChangeSchoolUserRoleUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ImportSchoolUsersUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.UploadSchoolUserImportFileUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUserUseCase;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportUploadResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.interfaces.rest.dto.request.SchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ChangeSchoolUserRoleCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.SchoolUserImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UploadSchoolUserImportFileCommandMapper;
import com.sep.vox.interfaces.rest.mapper.ViewSchoolUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolUserCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schools")
public class SchoolUserController {

    private final CreateSchoolUserUseCase createSchoolUserUseCase;
    private final ViewSchoolUserUseCase viewSchoolUserUseCase;
    private final DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private final ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;
    private final UploadSchoolUserImportFileUseCase uploadSchoolUserImportFileUseCase;
    private final ImportSchoolUsersUseCase importSchoolUsersUseCase;

    public SchoolUserController(
            CreateSchoolUserUseCase createSchoolUserUseCase,
            ViewSchoolUserUseCase viewSchoolUserUseCase,
            DeleteSchoolUserUseCase deleteSchoolUserUseCase,
            ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase,
            UploadSchoolUserImportFileUseCase uploadSchoolUserImportFileUseCase,
            ImportSchoolUsersUseCase importSchoolUsersUseCase) {
        this.createSchoolUserUseCase = createSchoolUserUseCase;
        this.viewSchoolUserUseCase = viewSchoolUserUseCase;
        this.deleteSchoolUserUseCase = deleteSchoolUserUseCase;
        this.changeSchoolUserRoleUseCase = changeSchoolUserRoleUseCase;
        this.uploadSchoolUserImportFileUseCase = uploadSchoolUserImportFileUseCase;
        this.importSchoolUsersUseCase = importSchoolUsersUseCase;
    }

    @PostMapping("/{schoolId}/users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolUserResponse>> createUser(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSchoolUserRequest request) {
        var command = CreateSchoolUserCommandMapper.fromRequest(schoolId, request);
        var data = createSchoolUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo người dùng thành công", data));
    }

    @GetMapping("/{schoolId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolUserResponse>> getUser(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId) {
        var command = ViewSchoolUserCommandMapper.fromRequest(schoolId, userId);
        SchoolUserResponse data = viewSchoolUserUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", data));
    }

    @DeleteMapping("/{schoolId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteUser(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId) {
        var command = DeleteSchoolUserCommandMapper.fromRequest(schoolId, userId);
        deleteSchoolUserUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công"));
    }

    @PatchMapping("/{schoolId}/users/{userId}/role")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> changeUserRole(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeSchoolUserRoleRequest request) {
        var command = ChangeSchoolUserRoleCommandMapper.fromRequest(schoolId, userId, request);
        changeSchoolUserRoleUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò người dùng thành công"));
    }

    @PostMapping(value = "/{schoolId}/users/import/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolUserImportUploadResponse>> uploadImportFile(
            @PathVariable UUID schoolId,
            @Parameter(description = "File import (CSV/XLSX/JSON)", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file) {
        var command = UploadSchoolUserImportFileCommandMapper.fromRequest(schoolId, file);
        var data = uploadSchoolUserImportFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Upload file import thành công", data));
    }

    @PostMapping("/{schoolId}/users/import")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolUserImportResponse>> importUsers(
            @PathVariable UUID schoolId,
            @Valid @RequestBody SchoolUserImportRequest request) {
        var command = SchoolUserImportCommandMapper.fromRequest(schoolId, request);
        var data = importSchoolUsersUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Import người dùng thành công", data));
    }
}
