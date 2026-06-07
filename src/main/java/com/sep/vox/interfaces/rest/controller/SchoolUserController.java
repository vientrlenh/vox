package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.importfile.UploadImportFileUseCase;
import com.sep.vox.application.port.input.command.PreviewSchoolUserImportFromFileCommand;
import com.sep.vox.application.response.input.importfile.CreateImportSessionResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportResponse;
import com.sep.vox.application.response.input.importfile.ImportFileUploadResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.interfaces.rest.dto.request.SchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ChangeSchoolUserRoleCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.SchoolUserImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UploadImportFileCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolUserCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schools/{schoolId}/users")
public class SchoolUserController {

    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of("csv", "xlsx", "json");

    private final CreateSchoolUserUseCase createSchoolUserUseCase;
    private final DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private final ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;
    private final UploadImportFileUseCase uploadSchoolUserImportFileUseCase;
    private final PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    private final ImportSchoolUsersUseCase importSchoolUsersUseCase;

    public SchoolUserController(
            CreateSchoolUserUseCase createSchoolUserUseCase,
            DeleteSchoolUserUseCase deleteSchoolUserUseCase,
            ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase,
            UploadImportFileUseCase uploadSchoolUserImportFileUseCase,
            PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase,
            ImportSchoolUsersUseCase importSchoolUsersUseCase) {
        this.createSchoolUserUseCase = createSchoolUserUseCase;
        this.deleteSchoolUserUseCase = deleteSchoolUserUseCase;
        this.changeSchoolUserRoleUseCase = changeSchoolUserRoleUseCase;
        this.uploadSchoolUserImportFileUseCase = uploadSchoolUserImportFileUseCase;
        this.previewSchoolUserImportFromFileUseCase = previewSchoolUserImportFromFileUseCase;
        this.importSchoolUsersUseCase = importSchoolUsersUseCase;
    }

    @PostMapping()
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolUserResponse>> createUser(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSchoolUserRequest request) {
        var command = CreateSchoolUserCommandMapper.fromRequest(schoolId, request);
        var data = createSchoolUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo người dùng thành công", data));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteUser(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId) {
        var command = DeleteSchoolUserCommandMapper.fromRequest(schoolId, userId);
        deleteSchoolUserUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công"));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> changeUserRole(
            @PathVariable UUID schoolId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeSchoolUserRoleRequest request) {
        var command = ChangeSchoolUserRoleCommandMapper.fromRequest(schoolId, userId, request);
        changeSchoolUserRoleUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò người dùng thành công"));
    }

    @PostMapping(value = "/import/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ImportFileUploadResponse>> uploadImportFile(
            @PathVariable UUID schoolId,
            @Parameter(description = "File import (CSV/XLSX/JSON)", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file) {
        validateUploadFile(file);
        var command = UploadImportFileCommandMapper.fromRequest(schoolId, file);
        var data = uploadSchoolUserImportFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Upload file import thành công", data));
    }

    @PostMapping("/import/preview")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateImportSessionResponse>> previewImportFile(
            @PathVariable UUID schoolId,
            @Valid @RequestBody SchoolUserImportRequest request) {
        var previewCommand = new PreviewSchoolUserImportFromFileCommand(
            schoolId,
            request.fileId(),
            request.defaultRole(),
            SchoolUserImportCommandMapper.fromRequest(schoolId, request).mapping()
        );
        var data = previewSchoolUserImportFromFileUseCase.execute(previewCommand);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiên preview import người dùng thành công", data));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolUserImportResponse>> importUsers(
            @PathVariable UUID schoolId,
            @Valid @RequestBody SchoolUserImportRequest request) {
        var command = SchoolUserImportCommandMapper.fromRequest(schoolId, request);
        var data = importSchoolUsersUseCase.execute(command);
        var message = data.failedCount() > 0 && data.createdCount() == 0
            ? "Import người dùng không thành công"
            : data.failedCount() > 0
                ? "Import người dùng hoàn thành với một số lỗi"
                : "Import người dùng thành công";
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không hợp lệ");
        }
        var fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ");
        }
        var extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ");
        }
    }
}
