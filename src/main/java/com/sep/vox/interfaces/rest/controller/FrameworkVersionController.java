package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.DeleteFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionStatusUseCase;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateFrameworkVersionStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkVersionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateFrameworkVersionStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/frameworks/{frameworkId}/versions")
public class FrameworkVersionController {

    private final CreateFrameworkVersionUseCase createFrameworkVersionUseCase;
    private final UpdateFrameworkVersionStatusUseCase updateFrameworkVersionStatusUseCase;
    private final DeleteFrameworkVersionUseCase deleteFrameworkVersionUseCase;

    public FrameworkVersionController(
            CreateFrameworkVersionUseCase createFrameworkVersionUseCase,
            UpdateFrameworkVersionStatusUseCase updateFrameworkVersionStatusUseCase,
            DeleteFrameworkVersionUseCase deleteFrameworkVersionUseCase) {
        this.createFrameworkVersionUseCase = createFrameworkVersionUseCase;
        this.updateFrameworkVersionStatusUseCase = updateFrameworkVersionStatusUseCase;
        this.deleteFrameworkVersionUseCase = deleteFrameworkVersionUseCase;
    }

    @PostMapping()
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateFrameworkVersionResponse>> createVersion(
            @PathVariable UUID frameworkId,
            @Valid @RequestBody CreateFrameworkVersionRequest request) {
        var command = CreateFrameworkVersionCommandMapper.fromRequest(frameworkId, request);
        var data = createFrameworkVersionUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo phiên bản framework thành công", data));
    }

    @PatchMapping("/{versionId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateVersionStatus(
            @PathVariable UUID frameworkId,
            @PathVariable UUID versionId,
            @Valid @RequestBody UpdateFrameworkVersionStatusRequest request) {
        var command = UpdateFrameworkVersionStatusCommandMapper.fromRequest(frameworkId, versionId, request);
        updateFrameworkVersionStatusUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái phiên bản thành công"));
    }

    @DeleteMapping("/{versionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(
            @PathVariable UUID frameworkId,
            @PathVariable UUID versionId) {
        var command = new DeleteFrameworkVersionCommand(frameworkId, versionId);
        deleteFrameworkVersionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Xóa phiên bản framework thành công"));
    }
}
