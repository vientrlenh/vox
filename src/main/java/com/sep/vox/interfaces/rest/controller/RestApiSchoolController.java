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

import com.sep.vox.application.port.input.command.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolCommand;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolDirectoryImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolGradeImportFromFileCommand;
import com.sep.vox.application.port.input.command.PreviewSchoolUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.UpdateSchoolStatusCommand;
import com.sep.vox.application.port.input.command.VerifySchoolDirectoryCommand;
import com.sep.vox.application.port.input.usecase.school.CreateSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.DeleteSchoolUseCase;
import com.sep.vox.application.port.input.usecase.school.UpdateSchoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.AcceptSchoolClassUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.BulkCreateSchoolClassUsersUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.CreateSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.DeleteSchoolClassUserUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.PreviewSchoolClassUserImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolclassuser.UpdateSchoolClassUserStatusUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.AcceptSchoolDirectoryImportUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.CreateSchoolDirectoryUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.PreviewSchoolDirectoryImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schooldirectory.VerifySchoolDirectoryUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.AcceptSchoolGradeImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.CreateSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.DeleteSchoolGradeUseCase;
import com.sep.vox.application.port.input.usecase.schoolgrade.PreviewSchoolGradeImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.CreateGradeLevelUseCase;
import com.sep.vox.application.port.input.usecase.gradelevel.DeleteGradeLevelUseCase;
import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolDirectoryImportCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolGradeImportCommand;
import com.sep.vox.application.port.input.command.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.command.BulkCreateSchoolClassUsersCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.CreateSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.CreateSchoolDirectoryCommand;
import com.sep.vox.application.port.input.command.DeleteGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.DeleteSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.PreviewSchoolRoomImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AcceptSchoolRoomImportUseCase;
import com.sep.vox.application.port.input.command.PreviewSchoolRoomImportFromFileCommand;
import com.sep.vox.application.response.input.schoolroom.PreviewSchoolRoomImportResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolRoomImportRequest;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolRoomImportCommandMapper;
import com.sep.vox.application.port.input.usecase.schooluser.AcceptSchoolUserImportUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.CreateSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.DeleteSchoolUserUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.PreviewSchoolUserImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassUserImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolUserImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclassuser.BulkCreateSchoolClassUsersResponse;
import com.sep.vox.application.response.input.schoolclassuser.CreateSchoolClassUserResponse;
import com.sep.vox.application.response.input.schoolclassuser.UpdateSchoolClassUserStatusResponse;
import com.sep.vox.application.response.input.schooldirectory.CreateSchoolDirectoryResponse;
import com.sep.vox.application.response.input.schooldirectory.PreviewSchoolDirectoryImportResponse;
import com.sep.vox.application.response.input.schoolgrade.PreviewSchoolGradeImportResponse;
import com.sep.vox.application.response.input.schooluser.CreateSchoolUserResponse;
import com.sep.vox.application.shared.UploadedFile;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolDirectoryImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolGradeImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.AddSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.request.BulkCreateSchoolClassUsersRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassUserRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolDirectoryRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateGradeLevelRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolGradeRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolUserRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateSchoolClassUserStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor 
public class RestApiSchoolController {

    private final CreateSchoolClassUseCase createSchoolClassUseCase;
    private final CreateSchoolClassUserUseCase createSchoolClassUserUseCase;
    private final BulkCreateSchoolClassUsersUseCase bulkCreateSchoolClassUsersUseCase;
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


    private final CreateSchoolUseCase createSchoolUseCase;
    private final DeleteSchoolUseCase deleteSchoolUseCase;
    private final UpdateSchoolStatusUseCase updateSchoolStatusUseCase;


    private final AddSchoolRoomUseCase addSchoolRoomUseCase;
    private final DeleteSchoolRoomUseCase deleteSchoolRoomUseCase;
    private final PreviewSchoolRoomImportFromFileUseCase previewSchoolRoomImportFromFileUseCase;
    private final AcceptSchoolRoomImportUseCase acceptSchoolRoomImportUseCase;

    
    private final CreateSchoolGradeUseCase createSchoolGradeUseCase;
    private final DeleteSchoolGradeUseCase deleteSchoolGradeUseCase;
    private final PreviewSchoolGradeImportFromFileUseCase previewSchoolGradeImportFromFileUseCase;
    private final AcceptSchoolGradeImportUseCase acceptSchoolGradeImportUseCase;


    private final CreateGradeLevelUseCase createGradeLevelUseCase;
    private final DeleteGradeLevelUseCase deleteGradeLevelUseCase;


    private final PreviewSchoolDirectoryImportFromFileUseCase previewSchoolDirectoryImportFromFileUseCase;
    private final AcceptSchoolDirectoryImportUseCase acceptSchoolDirectoryImportUseCase;
    private final CreateSchoolDirectoryUseCase createSchoolDirectoryUseCase;
    private final VerifySchoolDirectoryUseCase verifySchoolDirectoryUseCase;


    @PostMapping(value = "/directories/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolDirectoryImportResponse>> previewSchoolDirectoryImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        UploadedFile uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        PreviewSchoolDirectoryImportResponse data = previewSchoolDirectoryImportFromFileUseCase.execute(
                new PreviewSchoolDirectoryImportFromFileCommand(uploadedFile));
        ApiResponse<PreviewSchoolDirectoryImportResponse> response = ApiResponse.success("Successfully loaded school directory import preview", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/directories/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> acceptSchoolDirectoryImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolDirectoryImportRequest request) {
        AcceptSchoolDirectoryImportCommand command = AcceptSchoolDirectoryImportRequest.toCommand(sessionId, request);
        acceptSchoolDirectoryImportUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("School directories have been imported successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/directories")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolDirectoryResponse>> createSchoolDirectory(@Valid @RequestBody CreateSchoolDirectoryRequest request) {
        CreateSchoolDirectoryCommand command = CreateSchoolDirectoryRequest.toCommand(request);
        CreateSchoolDirectoryResponse data = createSchoolDirectoryUseCase.execute(command);
        ApiResponse<CreateSchoolDirectoryResponse> response = ApiResponse.success("School directory has been created successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{schoolId}/classes")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolClassResponse>> create(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CreateSchoolClassRequest request) {
        CreateSchoolClassCommand command = CreateSchoolClassRequest.toCommand(schoolId, request);
        CreateSchoolClassResponse data = createSchoolClassUseCase.execute(command);
        ApiResponse<CreateSchoolClassResponse> response = ApiResponse.success("School class created successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{schoolId}/classes/{classId}/users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolClassUserResponse>> createClassUser(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId,
            @Valid @RequestBody CreateSchoolClassUserRequest request) {
        CreateSchoolClassUserCommand command = CreateSchoolClassUserRequest.toCommand(schoolId, classId, request);
        CreateSchoolClassUserResponse data = createSchoolClassUserUseCase.execute(command);
        ApiResponse<CreateSchoolClassUserResponse> response = ApiResponse.success("User has been assigned to school class successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{schoolId}/classes/{classId}/users/bulk")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<BulkCreateSchoolClassUsersResponse>> createClassUsersBulk(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId,
            @Valid @RequestBody BulkCreateSchoolClassUsersRequest request) {
        BulkCreateSchoolClassUsersCommand command = BulkCreateSchoolClassUsersRequest.toCommand(schoolId, classId, request);
        BulkCreateSchoolClassUsersResponse data = bulkCreateSchoolClassUsersUseCase.execute(command);
        ApiResponse<BulkCreateSchoolClassUsersResponse> response = ApiResponse.success("School user list has been assigned succcessfully", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{schoolId}/classes/{classId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> deleteClassUser(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId,
            @PathVariable("userId") UUID userId) {
        DeleteSchoolClassUserCommand command = new DeleteSchoolClassUserCommand(schoolId, classId, userId);
        deleteSchoolClassUserUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/{schoolId}/classes/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolClassImportResponse>> createSchoolClassImportFileSession(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        UploadedFile uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        PreviewSchoolClassImportResponse data = previewSchoolClassImportFromFileUseCase.execute(new PreviewSchoolClassImportFromFileCommand(schoolId, uploadedFile));
        ApiResponse<PreviewSchoolClassImportResponse> response = ApiResponse.success("Successfully loaded school class import preview", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/classes/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> acceptImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolClassImportRequest request) {
        AcceptSchoolClassImportCommand command = AcceptSchoolClassImportRequest.toCommand(schoolId, sessionId, request);
        acceptSchoolClassImportUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("School classes have been imported successfully");
        return ResponseEntity.ok(response);
    }

    // Import khối lớp từ Excel đã bị bỏ: catalog dùng chung chỉ có vài dòng, seed bằng migration
    // (V41) rồi quản trị hệ thống sửa qua POST/DELETE /schools/grade-levels.

    @PostMapping(
            value = "/{schoolId}/grades/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolGradeImportResponse>> createGradeImportFileSession(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        UploadedFile uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        PreviewSchoolGradeImportResponse data = previewSchoolGradeImportFromFileUseCase.execute(new PreviewSchoolGradeImportFromFileCommand(schoolId, uploadedFile));
        ApiResponse<PreviewSchoolGradeImportResponse> response = ApiResponse.success("Successfully loaded school grade import preview", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/grades/import/{importSessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> acceptGradeImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("importSessionId") UUID importSessionId,
            @Valid @RequestBody AcceptSchoolGradeImportRequest request) {
        AcceptSchoolGradeImportCommand command = AcceptSchoolGradeImportRequest.toCommand(schoolId, importSessionId, request);
        acceptSchoolGradeImportUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("School grades have been imported successfully");
        return ResponseEntity.ok(response);
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
        DeleteSchoolClassCommand command = new DeleteSchoolClassCommand(schoolId, classId);
        deleteSchoolClassUseCase.execute(command);
        return ResponseEntity.noContent().build();
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
        DeleteSchoolUserCommand command = new DeleteSchoolUserCommand(schoolId, userId);
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
    public ResponseEntity<ApiResponse<Void>> acceptImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolUserImportRequest request) {
        AcceptSchoolUserImportCommand command = AcceptSchoolUserImportRequest.toCommand(schoolId, sessionId, request);
        acceptSchoolUserImportUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.success("School users have been imported successfully");
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteSchool(@PathVariable("id") UUID id) {
        DeleteSchoolCommand command = new DeleteSchoolCommand(id);
        deleteSchoolUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> updateSchoolStatus(
            @PathVariable("id") UUID id,
            @RequestParam("isActive") boolean isActive
    ) {
        UpdateSchoolStatusCommand command = new UpdateSchoolStatusCommand(id, isActive);
        UUID data = updateSchoolStatusUseCase.execute(command);

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

    @Operation(summary = "Preview import phòng học từ file Excel")
    @PostMapping(
            value = "/{schoolId}/rooms/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolRoomImportResponse>> createRoomImportFileSession(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolRoomImportFromFileUseCase.execute(new PreviewSchoolRoomImportFromFileCommand(schoolId, uploadedFile));
        return ResponseEntity.ok(ApiResponse.success("Preview import phòng học thành công", data));
    }

    @Operation(summary = "Xác nhận import phòng học")
    @PostMapping("/{schoolId}/rooms/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptRoomImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolRoomImportRequest request) {
        var command = AcceptSchoolRoomImportCommandMapper.fromRequest(schoolId, sessionId, request);
        acceptSchoolRoomImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu import phòng học đã được tiếp nhận, đang xử lý"));
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
    @PostMapping("/{schoolId}/grade-levels/{gradeLevelId}/grades")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolGrade(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("gradeLevelId") UUID gradeLevelId,
            @Valid @RequestBody CreateSchoolGradeRequest request) {

        var command = CreateSchoolGradeCommandMapper.fromRequest(schoolId, gradeLevelId, request);

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

    //=================== GRADE LEVEL (catalog toàn cục) ================================
    // Không còn nằm dưới /{schoolId}: khối lớp dùng chung cho mọi trường nên chỉ SYSTEM_ADMIN
    // được tạo/xóa. Xem GradeLevelController để biết các endpoint đọc.
    @Operation(summary = "Thêm Khối học sinh vào catalog dùng chung (VD: Khối 10, Khối 11)")
    @PostMapping("/grade-levels")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createGradeLevel(
            @Valid @RequestBody CreateGradeLevelRequest request) {

        var command = CreateGradeLevelCommandMapper.fromRequest(request);
        UUID newGradeLevelId = createGradeLevelUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Thêm khối học sinh thành công", newGradeLevelId));
    }

    @Operation(summary = "Xóa Khối học sinh khỏi catalog dùng chung")
    @DeleteMapping("/grade-levels/{gradeLevelId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGradeLevel(
            @PathVariable("gradeLevelId") UUID gradeLevelId) {

        deleteGradeLevelUseCase.execute(new DeleteGradeLevelCommand(gradeLevelId));

        return ResponseEntity.ok(ApiResponse.success("Xóa Khối học sinh thành công", null));
    }


    @Operation(summary = "Xác minh trường theo nhu cầu của hệ thống (hỗ trợ nguời đăng ký không cần nộp tài liệu chứng thực)")
    @PatchMapping("/directories/{id}/verify")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> verifySchoolDirectory(@PathVariable("id") UUID id) {
        var command = new VerifySchoolDirectoryCommand(id);
        var data = verifySchoolDirectoryUseCase.execute(command);
        var response = ApiResponse.success("Danh mục trường đã được xác minh", data);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Tạo trường học trực tiếp, không qua đơn đăng ký (có danh mục thì ưu tiên lấy thông tin từ danh mục)")
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        var command = CreateSchoolCommandMapper.fromRequest(request);
        var schoolId = createSchoolUseCase.execute(command);
        var response = ApiResponse.success("Trường học đã được tạo thành công", schoolId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
