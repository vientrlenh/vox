package com.sep.vox.interfaces.rest.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolUserImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ChangeSchoolUserRoleUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptSchoolUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.ChangeSchoolUserRoleRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolClassImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolUserImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.ChangeSchoolUserRoleCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolClassCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolUserCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schools")
public class SchoolController {

    private final CreateSchoolClassUseCase createSchoolClassUseCase;
    private final DeleteSchoolClassUseCase deleteSchoolClassUseCase;
    private final PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase;
    private final AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase;
    private final CreateSchoolUserUseCase createSchoolUserUseCase;
    private final DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private final ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase;
    private final PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    private final AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase;

    public SchoolController(
            CreateSchoolClassUseCase createSchoolClassUseCase,
            DeleteSchoolClassUseCase deleteSchoolClassUseCase,
            PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase,
            AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase,
            CreateSchoolUserUseCase createSchoolUserUseCase,
            DeleteSchoolUserUseCase deleteSchoolUserUseCase,
            ChangeSchoolUserRoleUseCase changeSchoolUserRoleUseCase,
            PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase,
            AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase) {
        this.createSchoolClassUseCase = createSchoolClassUseCase;
        this.deleteSchoolClassUseCase = deleteSchoolClassUseCase;
        this.previewSchoolClassImportFromFileUseCase = previewSchoolClassImportFromFileUseCase;
        this.acceptSchoolClassImportUseCase = acceptSchoolClassImportUseCase;
        this.createSchoolUserUseCase = createSchoolUserUseCase;
        this.deleteSchoolUserUseCase = deleteSchoolUserUseCase;
        this.changeSchoolUserRoleUseCase = changeSchoolUserRoleUseCase;
        this.previewSchoolUserImportFromFileUseCase = previewSchoolUserImportFromFileUseCase;
        this.acceptSchoolUserImportUseCase = acceptSchoolUserImportUseCase;
    }

    @PostMapping("/{schoolId}/classes")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolClassResponse>> create(
            @PathVariable UUID schoolId,
            @Valid @RequestBody CreateSchoolClassRequest request) {
        var command = CreateSchoolClassCommandMapper.fromRequest(schoolId, request);
        var data = createSchoolClassUseCase.execute(command);
        var response = ApiResponse.success("Tạo lớp học thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(
        value = "/{schoolId}/classes/import/preview",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolClassImportResponse>> createImportFileSession(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolClassImportFromFileUseCase.execute(new PreviewSchoolClassImportFromFileCommand(schoolId, uploadedFile));
        var response = ApiResponse.success("Preview import lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/classes/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptSchoolClassImportResponse>> acceptImportSession(
            @PathVariable UUID schoolId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AcceptSchoolClassImportRequest request) {
        var command = AcceptSchoolClassImportCommandMapper.fromRequest(schoolId, sessionId, request);
        var data = acceptSchoolClassImportUseCase.execute(command);
        var response = ApiResponse.success("Import lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{schoolId}/classes/{classId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<DeleteSchoolClassResponse>> delete(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId) {
        var data = deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(schoolId, classId));
        var response = ApiResponse.success("Xóa lớp học thành công", data);
        return ResponseEntity.ok(response);
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

    @PostMapping(value = "/{schoolId}/users/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolUserImportResponse>> previewImportFile(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolUserImportFromFileUseCase.execute(new PreviewSchoolUserImportFromFileCommand(schoolId, uploadedFile));
        return ResponseEntity.ok(ApiResponse.success("Preview import người dùng thành công", data));
    }

    @PostMapping("/{schoolId}/users/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptSchoolUserImportResponse>> acceptImportSession(
            @PathVariable UUID schoolId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AcceptSchoolUserImportRequest request) {
        var command = AcceptSchoolUserImportCommandMapper.fromRequest(schoolId, sessionId, request);
        var data = acceptSchoolUserImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Import người dùng thành công", data));
    }
}
