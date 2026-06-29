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
import com.sep.vox.application.port.input.command.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolDirectoryImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolGradeImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolGradeLevelImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolUserImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.school.DeleteSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.AcceptSchoolClassUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.CreateSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.DeleteSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.PreviewSchoolClassUserImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.UpdateSchoolClassUserStatusUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.AcceptSchoolDirectoryImportUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.CreateSchoolDirectoryUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.PreviewSchoolDirectoryImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.AcceptSchoolGradeImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.CreateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.DeleteSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.PreviewSchoolGradeImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.AcceptSchoolGradeLevelImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.CreateSchoolGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.DeleteSchoolGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.schoolgradelevel.PreviewSchoolGradeLevelImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.DeleteSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclassuser.CreateSchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclassuser.UpdateSchoolClassUserStatusResponse;
import com.sep.vox.application.response.input.schooldirectory.CreateSchoolDirectoryResponse;
import com.sep.vox.application.response.input.schooldirectory.PreviewSchoolDirectoryImportResponse;
import com.sep.vox.application.response.input.schoolgrade.PreviewSchoolGradeImportResponse;
import com.sep.vox.application.response.input.schoolgradelevel.PreviewSchoolGradeLevelImportResponse;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolDirectoryImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolGradeImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolGradeLevelImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AddSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassUserRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolDirectoryRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeLevelRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassUserStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolClassImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolClassUserImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolDirectoryImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolGradeImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolGradeLevelImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolUserImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.AddSchoolRoomCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolClassCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolClassUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolDirectoryCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolGradeCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolGradeLevelCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolClassUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolGradeCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolGradeLevelCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolRoomCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteSchoolUserCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateSchoolClassUserStatusCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateSchoolStatusCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
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


    private final CreateSchoolUserUseCase createSchoolUserUseCase;
    private final DeleteSchoolUserUseCase deleteSchoolUserUseCase;
    private final PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase;
    private final AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase;
    private final PreviewSchoolClassUserImportFromFileUseCase previewSchoolClassUserImportFromFileUseCase;
    private final AcceptSchoolClassUserImportUseCase acceptSchoolClassUserImportUseCase;


    private final DeleteSchoolUseCase deleteSchoolUseCase;
    private final UpdateSchoolStatusUseCase updateSchoolStatusUseCase;


    private final AddSchoolRoomUseCase addSchoolRoomUseCase;
    private final DeleteSchoolRoomUseCase deleteSchoolRoomUseCase;

    
    private final CreateSchoolGradeUseCase createSchoolGradeUseCase;
    private final DeleteSchoolGradeUseCase deleteSchoolGradeUseCase;
    private final PreviewSchoolGradeImportFromFileUseCase previewSchoolGradeImportFromFileUseCase;
    private final AcceptSchoolGradeImportUseCase acceptSchoolGradeImportUseCase;


    private final CreateSchoolGradeLevelUseCase createSchoolGradeLevelUseCase;
    private final DeleteSchoolGradeLevelUseCase deleteSchoolGradeLevelUseCase;
    private final PreviewSchoolGradeLevelImportFromFileUseCase previewSchoolGradeLevelImportFromFileUseCase;
    private final AcceptSchoolGradeLevelImportUseCase acceptSchoolGradeLevelImportUseCase;


    private final PreviewSchoolDirectoryImportFromFileUseCase previewSchoolDirectoryImportFromFileUseCase;
    private final AcceptSchoolDirectoryImportUseCase acceptSchoolDirectoryImportUseCase;
    private final CreateSchoolDirectoryUseCase createSchoolDirectoryUseCase;

    public SchoolController(CreateSchoolClassUseCase createSchoolClassUseCase, 
                        CreateSchoolClassUserUseCase createSchoolClassUserUseCase, 
                        DeleteSchoolClassUseCase deleteSchoolClassUseCase, 
                        DeleteSchoolClassUserUseCase deleteSchoolClassUserUseCase, 
                        UpdateSchoolClassUserStatusUseCase updateSchoolClassUserStatusUseCase, 
                        PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase, 
                        AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase, 
                        CreateSchoolUserUseCase createSchoolUserUseCase, 
                        DeleteSchoolUserUseCase deleteSchoolUserUseCase, PreviewSchoolUserImportFromFileUseCase previewSchoolUserImportFromFileUseCase, AcceptSchoolUserImportUseCase acceptSchoolUserImportUseCase, PreviewSchoolClassUserImportFromFileUseCase previewSchoolClassUserImportFromFileUseCase, 
                        AcceptSchoolClassUserImportUseCase acceptSchoolClassUserImportUseCase, 
                        DeleteSchoolUseCase deleteSchoolUseCase, 
                        UpdateSchoolStatusUseCase updateSchoolStatusUseCase, 
                        AddSchoolRoomUseCase addSchoolRoomUseCase, 
                        DeleteSchoolRoomUseCase deleteSchoolRoomUseCase, 
                        CreateSchoolGradeUseCase createSchoolGradeUseCase,
                        DeleteSchoolGradeUseCase deleteSchoolGradeUseCase,
                        PreviewSchoolGradeImportFromFileUseCase previewSchoolGradeImportFromFileUseCase,
                        AcceptSchoolGradeImportUseCase acceptSchoolGradeImportUseCase,
                        CreateSchoolGradeLevelUseCase createSchoolGradeLevelUseCase,
                        DeleteSchoolGradeLevelUseCase deleteSchoolGradeLevelUseCase,
                        PreviewSchoolGradeLevelImportFromFileUseCase previewSchoolGradeLevelImportFromFileUseCase,
                        AcceptSchoolGradeLevelImportUseCase acceptSchoolGradeLevelImportUseCase,
                        PreviewSchoolDirectoryImportFromFileUseCase previewSchoolDirectoryImportFromFileUseCase,
                        AcceptSchoolDirectoryImportUseCase acceptSchoolDirectoryImportUseCase, 
                        CreateSchoolDirectoryUseCase createSchoolDirectoryUseCase
                    ) {
        this.createSchoolClassUseCase = createSchoolClassUseCase;
        this.createSchoolClassUserUseCase = createSchoolClassUserUseCase;
        this.deleteSchoolClassUseCase = deleteSchoolClassUseCase;
        this.deleteSchoolClassUserUseCase = deleteSchoolClassUserUseCase;
        this.updateSchoolClassUserStatusUseCase = updateSchoolClassUserStatusUseCase;
        this.previewSchoolClassImportFromFileUseCase = previewSchoolClassImportFromFileUseCase;
        this.acceptSchoolClassImportUseCase = acceptSchoolClassImportUseCase;
        this.createSchoolUserUseCase = createSchoolUserUseCase;
        this.deleteSchoolUserUseCase = deleteSchoolUserUseCase;
        this.previewSchoolUserImportFromFileUseCase = previewSchoolUserImportFromFileUseCase;
        this.acceptSchoolUserImportUseCase = acceptSchoolUserImportUseCase;
        this.previewSchoolClassUserImportFromFileUseCase = previewSchoolClassUserImportFromFileUseCase;
        this.acceptSchoolClassUserImportUseCase = acceptSchoolClassUserImportUseCase;
        this.deleteSchoolUseCase = deleteSchoolUseCase;
        this.updateSchoolStatusUseCase = updateSchoolStatusUseCase;
        this.addSchoolRoomUseCase = addSchoolRoomUseCase;
        this.deleteSchoolRoomUseCase = deleteSchoolRoomUseCase;
        this.createSchoolGradeUseCase = createSchoolGradeUseCase;
        this.deleteSchoolGradeUseCase = deleteSchoolGradeUseCase;
        this.previewSchoolGradeImportFromFileUseCase = previewSchoolGradeImportFromFileUseCase;
        this.acceptSchoolGradeImportUseCase = acceptSchoolGradeImportUseCase;
        this.createSchoolGradeLevelUseCase = createSchoolGradeLevelUseCase;
        this.deleteSchoolGradeLevelUseCase = deleteSchoolGradeLevelUseCase;
        this.previewSchoolGradeLevelImportFromFileUseCase = previewSchoolGradeLevelImportFromFileUseCase;
        this.acceptSchoolGradeLevelImportUseCase = acceptSchoolGradeLevelImportUseCase;
        this.previewSchoolDirectoryImportFromFileUseCase = previewSchoolDirectoryImportFromFileUseCase;
        this.acceptSchoolDirectoryImportUseCase = acceptSchoolDirectoryImportUseCase;
        this.createSchoolDirectoryUseCase = createSchoolDirectoryUseCase;
    }

    @PostMapping(value = "/directories/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolDirectoryImportResponse>> previewSchoolDirectoryImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolDirectoryImportFromFileUseCase.execute(
                new PreviewSchoolDirectoryImportFromFileCommand(uploadedFile));
        var response = ApiResponse.success("Preview import danh mục trường thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/directories/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptSchoolDirectoryImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolDirectoryImportRequest request) {
        var command = AcceptSchoolDirectoryImportCommandMapper.fromRequest(sessionId, request);
        acceptSchoolDirectoryImportUseCase.execute(command);
        var response = ApiResponse.success("Import danh mục trường thành công");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/directories")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolDirectoryResponse>> createSchoolDirectory(@Valid @RequestBody CreateSchoolDirectoryRequest request) {
        var command = CreateSchoolDirectoryCommandMapper.fromRequest(request);
        var data = createSchoolDirectoryUseCase.execute(command);
        var response = ApiResponse.success("Danh mục trường được tạo thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{schoolId}/classes")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolClassResponse>> create(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CreateSchoolClassRequest request) {
        var command = CreateSchoolClassCommandMapper.fromRequest(schoolId, request);
        var data = createSchoolClassUseCase.execute(command);
        var response = ApiResponse.success("Tạo lớp học thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{schoolId}/classes/{classId}/users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolClassUserResponse>> createClassUser(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId,
            @Valid @RequestBody CreateSchoolClassUserRequest request) {
        var command = CreateSchoolClassUserCommandMapper.fromRequest(schoolId, classId, request);
        var data = createSchoolClassUserUseCase.execute(command);
        var response = ApiResponse.success("Thêm người dùng vào lớp học thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{schoolId}/classes/{classId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteClassUser(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId,
            @PathVariable("userId") UUID userId) {
        var command = DeleteSchoolClassUserCommandMapper.fromPath(schoolId, classId, userId);
        deleteSchoolClassUserUseCase.execute(command);
        var response = ApiResponse.<Void>success("Xóa người dùng khỏi lớp học thành công");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{schoolId}/classes/{classId}/users/{userId}/status")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateSchoolClassUserStatusResponse>> updateClassUserStatus(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId,
            @PathVariable("userId") UUID userId,
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
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolClassImportFromFileUseCase.execute(new PreviewSchoolClassImportFromFileCommand(schoolId, uploadedFile));
        var response = ApiResponse.success("Preview import lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/classes/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolClassImportRequest request) {
        var command = AcceptSchoolClassImportCommandMapper.fromRequest(schoolId, sessionId, request);
        acceptSchoolClassImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu import lớp học đã được tiếp nhận, đang xử lý"));
    }

    @PostMapping(
            value = "/{schoolId}/grade-levels/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolGradeLevelImportResponse>> createGradeLevelImportFileSession(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolGradeLevelImportFromFileUseCase.execute(new PreviewSchoolGradeLevelImportFromFileCommand(schoolId, uploadedFile));
        var response = ApiResponse.success("Preview import khối học thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/grade-levels/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptGradeLevelImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolGradeLevelImportRequest request) {
        var command = AcceptSchoolGradeLevelImportCommandMapper.fromRequest(schoolId, sessionId, request);
        acceptSchoolGradeLevelImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu import khối học đã được tiếp nhận, đang xử lý"));
    }

    @PostMapping(
            value = "/{schoolId}/grades/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolGradeImportResponse>> createGradeImportFileSession(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolGradeImportFromFileUseCase.execute(new PreviewSchoolGradeImportFromFileCommand(schoolId, uploadedFile));
        var response = ApiResponse.success("Preview import năm học thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/grades/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptGradeImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolGradeImportRequest request) {
        var command = AcceptSchoolGradeImportCommandMapper.fromRequest(schoolId, sessionId, request);
        acceptSchoolGradeImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu import năm học đã được tiếp nhận, đang xử lý"));
    }

    @PostMapping(
            value = "/{schoolId}/classes/users/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolClassUserImportResponse>> createClassUserImportFileSession(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolClassUserImportFromFileUseCase.execute(new PreviewSchoolClassUserImportFromFileCommand(schoolId, uploadedFile));
        var response = ApiResponse.success("Preview import người dùng vào lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/classes/users/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptClassUserImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolClassUserImportRequest request) {
        var command = AcceptSchoolClassUserImportCommandMapper.fromRequest(schoolId, sessionId, request);
        acceptSchoolClassUserImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu import người dùng vào lớp học đã được tiếp nhận, đang xử lý"));
    }

    @DeleteMapping("/{schoolId}/classes/{classId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId) {
        deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(schoolId, classId));
        var response = ApiResponse.<Void>success("Xóa lớp học thành công");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolUserResponse>> createUser(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CreateSchoolUserRequest request) {
        var command = CreateSchoolUserCommandMapper.fromRequest(schoolId, request);
        var data = createSchoolUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo người dùng thành công", data));
    }

    @DeleteMapping("/{schoolId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("userId") UUID userId) {
        var command = DeleteSchoolUserCommandMapper.fromRequest(schoolId, userId);
        deleteSchoolUserUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{schoolId}/users/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolUserImportResponse>> previewImportFile(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolUserImportFromFileUseCase.execute(new PreviewSchoolUserImportFromFileCommand(schoolId, uploadedFile));
        return ResponseEntity.ok(ApiResponse.success("Preview import người dùng thành công", data));
    }

    @PostMapping("/{schoolId}/users/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolUserImportRequest request) {
        var command = AcceptSchoolUserImportCommandMapper.fromRequest(schoolId, sessionId, request);
        acceptSchoolUserImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu import người dùng đã được tiếp nhận, đang xử lý"));
    }

    //Delete School
    @Operation(summary = "Xóa trường học")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> deleteSchool(@PathVariable("id") UUID id) {

        var command = DeleteSchoolCommandMapper.fromRequest(id);

        // Hứng response từ UseCase
        var response = deleteSchoolUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Xóa trường học thành công", response));
    }


    @Operation(summary = "Thay đổi trạng thái hoạt động của trường học")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> updateSchoolStatus(
            @PathVariable("id") UUID id,
            @RequestParam("isActive") boolean isActive
    ) {

        var command = UpdateSchoolStatusCommandMapper.fromRequest(id, isActive);

        var response = updateSchoolStatusUseCase.execute(command);

        String message = isActive
                ? "Đã kích hoạt lại trường học thành công"
                : "Đã vô hiệu hóa trường học thành công";

        return ResponseEntity.ok(
                ApiResponse.success(message, response)
        );
    }


    //=======================SCHOOL ROOM=========================================
    @Operation(summary = "Thêm phòng học")
    @PostMapping("/{schoolId}/rooms")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> addSchoolRoom(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody AddSchoolRoomRequest request
    ) {

        var command = AddSchoolRoomCommandMapper.fromRequest(schoolId,request);

        var roomId = addSchoolRoomUseCase.execute(command);

        return ResponseEntity.ok(
                ApiResponse.success("Thêm phòng học thành công", roomId)
        );
    }

    @Operation(summary = "Xóa phòng học theo id ")
    @DeleteMapping("/{schoolId}/rooms/{roomId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRoom(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("roomId") UUID roomId
    ) {

        // Map ID từ URL vào Command
        var command = DeleteSchoolRoomCommandMapper.fromRequest(roomId,schoolId);


         deleteSchoolRoomUseCase.execute(command);

        // Trả về ApiResponse chuẩn của team bạn
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công school room"));
    }

    //====================================SCHOOL GRADE ==============================================

    @Operation(summary = "Thêm khối học sinh vd: khối 10,11,12")
    @PostMapping("/{schoolId}/grade-levels/{schoolGradeLevelId}/grades")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolGrade(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("schoolGradeLevelId") UUID schoolGradeLevelId,
            @Valid @RequestBody CreateSchoolGradeRequest request) {

        var command = CreateSchoolGradeCommandMapper.fromRequest(schoolId, schoolGradeLevelId, request);

        UUID newGradeId = createSchoolGradeUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Thêm thành công khối của trường", newGradeId));
    }



    @Operation(summary = "Xóa khối theo id của trường ")
    @DeleteMapping("/{schoolId}/grades/{gradeId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolGrade(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("gradeId") UUID gradeId
    ) {

        // Map ID từ URL vào Command
        var command = DeleteSchoolGradeCommandMapper.fromRequest(schoolId,gradeId);

        // Thực thi UseCase
        var result = deleteSchoolGradeUseCase.execute(command);

        // Trả về ApiResponse chuẩn của team bạn
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công school grade", result));
    }

    //===================SCHOOL GRADE LEVEL ================================
    @Operation(summary = "Thêm Khối học sinh cho Trường (VD: Khối 10, Khối 11)")
    @PostMapping("/{schoolId}/grade-levels")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolGradeLevel(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CreateSchoolGradeLevelRequest request) {

        var command = CreateSchoolGradeLevelCommandMapper.fromRequest(schoolId, request);
        UUID newGradeLevelId = createSchoolGradeLevelUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Thêm khối học sinh thành công", newGradeLevelId));
    }

    @Operation(summary = "Xóa Khối học sinh của Trường")
    @DeleteMapping("/{schoolId}/grade-levels/{gradeLevelId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolGradeLevel(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("gradeLevelId") UUID gradeLevelId) {

        var command = DeleteSchoolGradeLevelCommandMapper.fromRequest(schoolId, gradeLevelId);

        // Gọi UseCase, nó sẽ return null
        deleteSchoolGradeLevelUseCase.execute(command);

        // Ném về response báo thành công
        return ResponseEntity.ok(ApiResponse.success("Xóa Khối học sinh thành công", null));
    }
}
