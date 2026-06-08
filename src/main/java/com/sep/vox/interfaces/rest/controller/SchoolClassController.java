package com.sep.vox.interfaces.rest.controller;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.sep.vox.application.port.input.usecase.schoolclass.AcceptSchoolClassImportUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.CreateSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.DeleteSchoolClassUseCase;
import com.sep.vox.application.port.input.usecase.schoolclass.PreviewSchoolClassImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewSchoolClassImportResponse;
import com.sep.vox.application.response.input.schoolclass.CreateSchoolClassResponse;
import com.sep.vox.application.response.input.schoolclass.DeleteSchoolClassResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptSchoolClassImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolClassRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AcceptSchoolClassImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateSchoolClassCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/school-classes")
public class SchoolClassController {

    private final CreateSchoolClassUseCase createSchoolClassUseCase;
    private final DeleteSchoolClassUseCase deleteSchoolClassUseCase;
    private final PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase;
    private final AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase;

    public SchoolClassController(
            CreateSchoolClassUseCase createSchoolClassUseCase,
            DeleteSchoolClassUseCase deleteSchoolClassUseCase,
            PreviewSchoolClassImportFromFileUseCase previewSchoolClassImportFromFileUseCase,
            AcceptSchoolClassImportUseCase acceptSchoolClassImportUseCase) {
        this.createSchoolClassUseCase = createSchoolClassUseCase;
        this.deleteSchoolClassUseCase = deleteSchoolClassUseCase;
        this.previewSchoolClassImportFromFileUseCase = previewSchoolClassImportFromFileUseCase;
        this.acceptSchoolClassImportUseCase = acceptSchoolClassImportUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateSchoolClassResponse>> create(@Valid @RequestBody CreateSchoolClassRequest request) {
        var command = CreateSchoolClassCommandMapper.fromRequest(request);
        var data = createSchoolClassUseCase.execute(command);
        var response = ApiResponse.success("Tạo lớp học thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/import/preview")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewSchoolClassImportResponse>> createImportFileSession(@RequestParam("file") MultipartFile file) throws IOException {
        var uploadedFile = UploadedFile.upload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        var data = previewSchoolClassImportFromFileUseCase.execute(new PreviewSchoolClassImportFromFileCommand(uploadedFile));
        var response = ApiResponse.success("Preview import lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptSchoolClassImportResponse>> acceptImportSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AcceptSchoolClassImportRequest request) {
        var command = AcceptSchoolClassImportCommandMapper.fromRequest(sessionId, request);
        var data = acceptSchoolClassImportUseCase.execute(command);
        var response = ApiResponse.success("Import lớp học thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<DeleteSchoolClassResponse>> delete(@PathVariable UUID id) {
        var data = deleteSchoolClassUseCase.execute(new DeleteSchoolClassCommand(id));
        var response = ApiResponse.success("Xóa lớp học thành công", data);
        return ResponseEntity.ok(response);
    }
}
