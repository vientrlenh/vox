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
import com.sep.vox.application.port.input.command.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassUserImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.UpdateSchoolClassUserStatusUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclass.UpdateSchoolClassUserStatusResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassUserRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassUserStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolClassImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolClassUserImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolClassCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolClassUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolClassUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateSchoolClassUserStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schools")
public class SchoolController {

    private final CreateSchoolClassUseCase createSchoolClassUseCase;
    private final CreateSchoolClassUserUseCase createSchoolClassUserUseCase;
    private final DeleteSchoolClassUseCase deleteSchoolClassUseCase;
    private final DeleteSchoolClassUserUseCase deleteSchoolClassUserUseCase;
    private final UpdateSchoolClassUserStatusUseCase updateSchoolClassUserStatusUseCase;
    private final PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase;
    private final AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase;
    private final PreviewSchoolClassUserImportFromFileUseCase previewSchoolClassUserImportFromFileUseCase;
    private final AcceptSchoolClassUserImportUseCase acceptSchoolClassUserImportUseCase;

    public SchoolController(
            CreateSchoolClassUseCase createSchoolClassUseCase,
            CreateSchoolClassUserUseCase createSchoolClassUserUseCase,
            DeleteSchoolClassUseCase deleteSchoolClassUseCase,
            DeleteSchoolClassUserUseCase deleteSchoolClassUserUseCase,
            UpdateSchoolClassUserStatusUseCase updateSchoolClassUserStatusUseCase,
            PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase,
            AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase,
            PreviewSchoolClassUserImportFromFileUseCase previewSchoolClassUserImportFromFileUseCase,
            AcceptSchoolClassUserImportUseCase acceptSchoolClassUserImportUseCase) {
        this.createSchoolClassUseCase = createSchoolClassUseCase;
        this.createSchoolClassUserUseCase = createSchoolClassUserUseCase;
        this.deleteSchoolClassUseCase = deleteSchoolClassUseCase;
        this.deleteSchoolClassUserUseCase = deleteSchoolClassUserUseCase;
        this.updateSchoolClassUserStatusUseCase = updateSchoolClassUserStatusUseCase;
        this.previewSchoolClassImportFromFileUseCase = previewSchoolClassImportFromFileUseCase;
        this.acceptSchoolClassImportUseCase = acceptSchoolClassImportUseCase;
        this.previewSchoolClassUserImportFromFileUseCase = previewSchoolClassUserImportFromFileUseCase;
        this.acceptSchoolClassUserImportUseCase = acceptSchoolClassUserImportUseCase;
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

    @PostMapping("/{schoolId}/classes/{classId}/users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolClassUserResponse>> createClassUser(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @Valid @RequestBody CreateSchoolClassUserRequest request) {
        var command = CreateSchoolClassUserCommandMapper.fromRequest(schoolId, classId, request);
        var data = createSchoolClassUserUseCase.execute(command);
        var response = ApiResponse.success("Thêm người dùng vào lớp học thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{schoolId}/classes/{classId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<DeleteSchoolClassUserResponse>> deleteClassUser(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @PathVariable UUID userId) {
        var command = DeleteSchoolClassUserCommandMapper.fromPath(schoolId, classId, userId);
        var data = deleteSchoolClassUserUseCase.execute(command);
        var response = ApiResponse.success("Xóa người dùng khỏi lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{schoolId}/classes/{classId}/users/{userId}/status")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateSchoolClassUserStatusResponse>> updateClassUserStatus(
            @PathVariable UUID schoolId,
            @PathVariable UUID classId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateSchoolClassUserStatusRequest request) {
        var command = UpdateSchoolClassUserStatusCommandMapper.fromRequest(schoolId, classId, userId, request);
        var data = updateSchoolClassUserStatusUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật trạng thái người dùng trong lớp học thành công", data);
        return ResponseEntity.ok(response);
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

    @PostMapping(
        value = "/{schoolId}/classes/users/import/preview",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolClassUserImportResponse>> createClassUserImportFileSession(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolClassUserImportFromFileUseCase.execute(new PreviewSchoolClassUserImportFromFileCommand(schoolId, uploadedFile));
        var response = ApiResponse.success("Preview import người dùng vào lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/classes/users/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptSchoolClassUserImportResponse>> acceptClassUserImportSession(
            @PathVariable UUID schoolId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AcceptSchoolClassUserImportRequest request) {
        var command = AcceptSchoolClassUserImportCommandMapper.fromRequest(schoolId, sessionId, request);
        var data = acceptSchoolClassUserImportUseCase.execute(command);
        var response = ApiResponse.success("Import người dùng vào lớp học thành công", data);
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
}
