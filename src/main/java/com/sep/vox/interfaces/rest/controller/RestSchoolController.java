package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.schoolclass.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.command.schooldirectory.AcceptSchoolDirectoryImportCommand;
import com.sep.vox.application.port.input.command.schoolgrade.AcceptSchoolGradeImportCommand;
import com.sep.vox.application.port.input.command.schoolroom.AcceptSchoolRoomImportCommand;
import com.sep.vox.application.port.input.command.schooluser.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.command.schoolroom.AddSchoolRoomCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.BulkCreateSchoolClassUsersCommand;
import com.sep.vox.application.port.input.command.gradelevel.CreateGradeLevelCommand;
import com.sep.vox.application.port.input.command.schoolclass.CreateSchoolClassCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.CreateSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.school.CreateSchoolCommand;
import com.sep.vox.application.port.input.command.schooldirectory.CreateSchoolDirectoryCommand;
import com.sep.vox.application.port.input.command.schoolgrade.CreateSchoolGradeCommand;
import com.sep.vox.application.port.input.command.schooluser.CreateSchoolUserCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.DeleteSchoolClassUserCommand;
import com.sep.vox.application.port.input.command.school.DeleteSchoolCommand;
import com.sep.vox.application.port.input.command.schoolgrade.DeleteSchoolGradeCommand;
import com.sep.vox.application.port.input.command.schoolroom.DeleteSchoolRoomCommand;
import com.sep.vox.application.port.input.command.schooluser.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.UpdateSchoolClassUserStatusCommand;
import com.sep.vox.application.port.input.command.school.UpdateSchoolStatusCommand;
import java.io.IOException;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
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
import com.sep.vox.application.port.input.command.schoolclass.DeleteSchoolClassCommand;
import com.sep.vox.application.port.input.command.schoolclass.PreviewSchoolClassImportFromFileCommand;
import com.sep.vox.application.port.input.command.schoolclassuser.PreviewSchoolClassUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.schooldirectory.PreviewSchoolDirectoryImportFromFileCommand;
import com.sep.vox.application.port.input.command.schoolgrade.PreviewSchoolGradeImportFromFileCommand;
import com.sep.vox.application.port.input.command.schooluser.PreviewSchoolUserImportFromFileCommand;
import com.sep.vox.application.port.input.command.schooldirectory.VerifySchoolDirectoryCommand;
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
import com.sep.vox.application.port.input.command.gradelevel.DeleteGradeLevelCommand;
import com.sep.vox.application.port.input.usecase.schoolroom.AddSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.DeleteSchoolRoomUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.PreviewSchoolRoomImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.schoolroom.AcceptSchoolRoomImportUseCase;
import com.sep.vox.application.port.input.command.schoolroom.PreviewSchoolRoomImportFromFileCommand;
import com.sep.vox.application.response.input.schoolroom.PreviewSchoolRoomImportResponse;
import com.sep.vox.interfaces.rest.dto.request.schoolroom.AcceptSchoolRoomImportRequest;
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
import com.sep.vox.interfaces.rest.dto.request.schoolclass.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.schoolclassuser.AcceptSchoolClassUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.schooldirectory.AcceptSchoolDirectoryImportRequest;
import com.sep.vox.interfaces.rest.dto.request.schoolgrade.AcceptSchoolGradeImportRequest;
import com.sep.vox.interfaces.rest.dto.request.schooluser.AcceptSchoolUserImportRequest;
import com.sep.vox.interfaces.rest.dto.request.schoolroom.AddSchoolRoomRequest;
import com.sep.vox.interfaces.rest.dto.request.schoolclassuser.BulkCreateSchoolClassUsersRequest;
import com.sep.vox.interfaces.rest.dto.request.schoolclass.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.request.schoolclassuser.CreateSchoolClassUserRequest;
import com.sep.vox.interfaces.rest.dto.request.schooldirectory.CreateSchoolDirectoryRequest;
import com.sep.vox.interfaces.rest.dto.request.gradelevel.CreateGradeLevelRequest;
import com.sep.vox.interfaces.rest.dto.request.schoolgrade.CreateSchoolGradeRequest;
import com.sep.vox.interfaces.rest.dto.request.school.CreateSchoolRequest;
import com.sep.vox.interfaces.rest.dto.request.schooluser.CreateSchoolUserRequest;
import com.sep.vox.interfaces.rest.dto.request.schoolclassuser.UpdateSchoolClassUserStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
public class RestSchoolController {

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
        ApiResponse<PreviewSchoolDirectoryImportResponse> response = ApiResponse.success("School directory import previewed successfully", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/directories/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptSchoolDirectoryImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolDirectoryImportRequest request) {
        AcceptSchoolDirectoryImportCommand command = AcceptSchoolDirectoryImportRequest.toCommand(sessionId, request);
        acceptSchoolDirectoryImportUseCase.execute(command);
        ApiResponse<Object> response = ApiResponse.success("School directory imported successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/directories")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolDirectoryResponse>> createSchoolDirectory(@Valid @RequestBody CreateSchoolDirectoryRequest request) {
        CreateSchoolDirectoryCommand command = CreateSchoolDirectoryRequest.toCommand(request);
        CreateSchoolDirectoryResponse data = createSchoolDirectoryUseCase.execute(command);
        ApiResponse<CreateSchoolDirectoryResponse> response = ApiResponse.success("School directory created successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{schoolId}/classes")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolClassResponse>> create(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CreateSchoolClassRequest request) {
        CreateSchoolClassCommand command = CreateSchoolClassRequest.toCommand(schoolId, request);
        CreateSchoolClassResponse data = createSchoolClassUseCase.execute(command);
        ApiResponse<CreateSchoolClassResponse> response = ApiResponse.success("Class created successfully", data);
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
        ApiResponse<CreateSchoolClassUserResponse> response = ApiResponse.success("User added to the class successfully", data);
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
        ApiResponse<BulkCreateSchoolClassUsersResponse> response = ApiResponse.success("Users added to the class successfully", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{schoolId}/classes/{classId}/users/{userId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteClassUser(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId,
            @PathVariable("userId") UUID userId) {
        DeleteSchoolClassUserCommand command = new DeleteSchoolClassUserCommand(schoolId, classId, userId);
        deleteSchoolClassUserUseCase.execute(command);
        ApiResponse<Void> response = ApiResponse.<Void>success("User removed from the class successfully");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{schoolId}/classes/{classId}/users/{userId}/status")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateSchoolClassUserStatusResponse>> updateClassUserStatus(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId,
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateSchoolClassUserStatusRequest request) {
        UpdateSchoolClassUserStatusCommand command = UpdateSchoolClassUserStatusRequest.toCommand(schoolId, classId, userId, request);
        UpdateSchoolClassUserStatusResponse data = updateSchoolClassUserStatusUseCase.execute(command);
        ApiResponse<UpdateSchoolClassUserStatusResponse> response = ApiResponse.success("User status in the class updated successfully", data);
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
        UploadedFile uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        PreviewSchoolClassImportResponse data = previewSchoolClassImportFromFileUseCase.execute(new PreviewSchoolClassImportFromFileCommand(schoolId, uploadedFile));
        ApiResponse<PreviewSchoolClassImportResponse> response = ApiResponse.success("Class import previewed successfully", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/classes/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolClassImportRequest request) {
        AcceptSchoolClassImportCommand command = AcceptSchoolClassImportRequest.toCommand(schoolId, sessionId, request);
        acceptSchoolClassImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Class import request accepted, processing"));
    }

    // Importing grades from Excel was dropped: the shared catalog has only a few rows, seeded by a migration
    // (V41) and then edited by the system admin through POST/DELETE /schools/grade-levels.

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
        ApiResponse<PreviewSchoolGradeImportResponse> response = ApiResponse.success("School year import previewed successfully", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/grades/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptGradeImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolGradeImportRequest request) {
        AcceptSchoolGradeImportCommand command = AcceptSchoolGradeImportRequest.toCommand(schoolId, sessionId, request);
        acceptSchoolGradeImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("School year import request accepted, processing"));
    }

    @PostMapping(
            value = "/{schoolId}/classes/users/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolClassUserImportResponse>> createClassUserImportFileSession(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        UploadedFile uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        PreviewSchoolClassUserImportResponse data = previewSchoolClassUserImportFromFileUseCase.execute(new PreviewSchoolClassUserImportFromFileCommand(schoolId, uploadedFile));
        ApiResponse<PreviewSchoolClassUserImportResponse> response = ApiResponse.success("Class user import previewed successfully", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/classes/users/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptClassUserImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolClassUserImportRequest request) {
        AcceptSchoolClassUserImportCommand command = AcceptSchoolClassUserImportRequest.toCommand(schoolId, sessionId, request);
        acceptSchoolClassUserImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Class user import request accepted, processing"));
    }

    @DeleteMapping("/{schoolId}/classes/{classId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("classId") UUID classId) {
        deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(schoolId, classId));
        ApiResponse<Void> response = ApiResponse.<Void>success("Class deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolId}/users")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolUserResponse>> createUser(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CreateSchoolUserRequest request) {
        CreateSchoolUserCommand command = CreateSchoolUserRequest.toCommand(schoolId, request);
        CreateSchoolUserResponse data = createSchoolUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User created successfully", data));
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
        UploadedFile uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        PreviewSchoolUserImportResponse data = previewSchoolUserImportFromFileUseCase.execute(new PreviewSchoolUserImportFromFileCommand(schoolId, uploadedFile));
        return ResponseEntity.ok(ApiResponse.success("User import previewed successfully", data));
    }

    @PostMapping("/{schoolId}/users/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolUserImportRequest request) {
        AcceptSchoolUserImportCommand command = AcceptSchoolUserImportRequest.toCommand(schoolId, sessionId, request);
        acceptSchoolUserImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("User import request accepted, processing"));
    }

    //Delete School
    @Operation(summary = "Deletes a school")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> deleteSchool(@PathVariable("id") UUID id) {

        DeleteSchoolCommand command = new DeleteSchoolCommand(id);

        UUID response = deleteSchoolUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("School deleted successfully", response));
    }


    @Operation(summary = "Changes the active status of a school")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> updateSchoolStatus(
            @PathVariable("id") UUID id,
            @RequestParam("isActive") boolean isActive
    ) {

        UpdateSchoolStatusCommand command = new UpdateSchoolStatusCommand(id, isActive);

        UUID response = updateSchoolStatusUseCase.execute(command);

        String message = isActive
                ? "School reactivated successfully"
                : "School deactivated successfully";

        return ResponseEntity.ok(
                ApiResponse.success(message, response)
        );
    }


    //=======================SCHOOL ROOM=========================================
    @Operation(summary = "Adds a room")
    @PostMapping("/{schoolId}/rooms")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> addSchoolRoom(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody AddSchoolRoomRequest request
    ) {

        AddSchoolRoomCommand command = AddSchoolRoomRequest.toCommand(schoolId,request);

        UUID roomId = addSchoolRoomUseCase.execute(command);

        return ResponseEntity.ok(
                ApiResponse.success("Room added successfully", roomId)
        );
    }

    @Operation(summary = "Previews a room import from an Excel file")
    @PostMapping(
            value = "/{schoolId}/rooms/import/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolRoomImportResponse>> createRoomImportFileSession(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) throws IOException {
        UploadedFile uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        PreviewSchoolRoomImportResponse data = previewSchoolRoomImportFromFileUseCase.execute(new PreviewSchoolRoomImportFromFileCommand(schoolId, uploadedFile));
        return ResponseEntity.ok(ApiResponse.success("Room import previewed successfully", data));
    }

    @Operation(summary = "Confirms the room import")
    @PostMapping("/{schoolId}/rooms/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptRoomImportSession(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptSchoolRoomImportRequest request) {
        AcceptSchoolRoomImportCommand command = AcceptSchoolRoomImportRequest.toCommand(schoolId, sessionId, request);
        acceptSchoolRoomImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Room import request accepted, processing"));
    }

    @Operation(summary = "Deletes a room by id")
    @DeleteMapping("/{schoolId}/rooms/{roomId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRoom(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("roomId") UUID roomId
    ) {

        DeleteSchoolRoomCommand command = new DeleteSchoolRoomCommand(schoolId, roomId);


         deleteSchoolRoomUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("School room deleted successfully"));
    }

    //====================================SCHOOL GRADE ==============================================

    @Operation(summary = "Adds a student grade, e.g. grades 10, 11, 12")
    @PostMapping("/{schoolId}/grade-levels/{gradeLevelId}/grades")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolGrade(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("gradeLevelId") UUID gradeLevelId,
            @Valid @RequestBody CreateSchoolGradeRequest request) {

        CreateSchoolGradeCommand command = CreateSchoolGradeRequest.toCommand(schoolId, gradeLevelId, request);

        UUID newGradeId = createSchoolGradeUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("School grade added successfully", newGradeId));
    }



    @Operation(summary = "Deletes a school grade by id")
    @DeleteMapping("/{schoolId}/grades/{gradeId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolGrade(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("gradeId") UUID gradeId
    ) {

        DeleteSchoolGradeCommand command = new DeleteSchoolGradeCommand(schoolId, gradeId);

        Void result = deleteSchoolGradeUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("School grade deleted successfully", result));
    }

    //=================== GRADE LEVEL (global catalog) ================================
    // No longer under /{schoolId}: grade levels are shared by every school, so only SYSTEM_ADMIN
    // may create/delete them. See GradeLevelController for the read endpoints.
    @Operation(summary = "Adds a student Grade to the shared catalog (e.g. Grade 10, Grade 11)")
    @PostMapping("/grade-levels")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createGradeLevel(
            @Valid @RequestBody CreateGradeLevelRequest request) {

        CreateGradeLevelCommand command = CreateGradeLevelRequest.toCommand(request);
        UUID newGradeLevelId = createGradeLevelUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Student grade added successfully", newGradeLevelId));
    }

    @Operation(summary = "Removes a student Grade from the shared catalog")
    @DeleteMapping("/grade-levels/{gradeLevelId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGradeLevel(
            @PathVariable("gradeLevelId") UUID gradeLevelId) {

        deleteGradeLevelUseCase.execute(new DeleteGradeLevelCommand(gradeLevelId));

        return ResponseEntity.ok(ApiResponse.success("Student grade deleted successfully", null));
    }


    @Operation(summary = "Verifies a school on the system's own initiative (lets registrants skip submitting supporting documents)")
    @PatchMapping("/directories/{id}/verify")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> verifySchoolDirectory(@PathVariable("id") UUID id) {
        VerifySchoolDirectoryCommand command = new VerifySchoolDirectoryCommand(id);
        UUID data = verifySchoolDirectoryUseCase.execute(command);
        ApiResponse<UUID> response = ApiResponse.success("School directory entry verified", data);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Creates a school directly, without a registration form (when a directory entry exists its information takes priority)")
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        CreateSchoolCommand command = CreateSchoolRequest.toCommand(request);
        UUID schoolId = createSchoolUseCase.execute(command);
        ApiResponse<UUID> response = ApiResponse.success("School created successfully", schoolId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
