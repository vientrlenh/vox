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

import com.sep.vox.application.port.input.command.DeleteFrameworkCommand;
import com.sep.vox.application.port.input.command.DeleteFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionStatusUseCase;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateFrameworkRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateFrameworkVersionStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkVersionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateFrameworkCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateFrameworkVersionStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/frameworks")
public class FrameworkController {

    private final CreateFrameworkUseCase createFrameworkUseCase;
    private final UpdateFrameworkUseCase updateFrameworkUseCase;
    private final DeleteFrameworkUseCase deleteFrameworkUseCase;
    private final CreateFrameworkVersionUseCase createFrameworkVersionUseCase;
    private final UpdateFrameworkVersionStatusUseCase updateFrameworkVersionStatusUseCase;
    private final DeleteFrameworkVersionUseCase deleteFrameworkVersionUseCase;

    public FrameworkController(
            CreateFrameworkUseCase createFrameworkUseCase,
            UpdateFrameworkUseCase updateFrameworkUseCase,
            DeleteFrameworkUseCase deleteFrameworkUseCase,
            CreateFrameworkVersionUseCase createFrameworkVersionUseCase,
            UpdateFrameworkVersionStatusUseCase updateFrameworkVersionStatusUseCase,
            DeleteFrameworkVersionUseCase deleteFrameworkVersionUseCase) {
        this.createFrameworkUseCase = createFrameworkUseCase;
        this.updateFrameworkUseCase = updateFrameworkUseCase;
        this.deleteFrameworkUseCase = deleteFrameworkUseCase;
        this.createFrameworkVersionUseCase = createFrameworkVersionUseCase;
        this.updateFrameworkVersionStatusUseCase = updateFrameworkVersionStatusUseCase;
        this.deleteFrameworkVersionUseCase = deleteFrameworkVersionUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createFramework(@Valid @RequestBody CreateFrameworkRequest request) {
        var command = CreateFrameworkCommandMapper.fromRequest(request);
        var id = createFrameworkUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo framework thành công", id));
    }

    @PatchMapping("/{frameworkId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> updateFramework(
            @PathVariable UUID frameworkId,
            @Valid @RequestBody UpdateFrameworkRequest request) {
        var command = UpdateFrameworkCommandMapper.fromRequest(frameworkId, request);
        var id = updateFrameworkUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật framework thành công", id));
    }

    @DeleteMapping("/{frameworkId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteFramework(@PathVariable UUID frameworkId) {
        deleteFrameworkUseCase.execute(new DeleteFrameworkCommand(frameworkId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{frameworkId}/versions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateFrameworkVersionResponse>> createVersion(
            @PathVariable UUID frameworkId,
            @Valid @RequestBody CreateFrameworkVersionRequest request) {
        var command = CreateFrameworkVersionCommandMapper.fromRequest(frameworkId, request);
        var data = createFrameworkVersionUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo phiên bản framework thành công", data));
    }

    @PatchMapping("/{frameworkId}/versions/{versionId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> updateVersionStatus(
            @PathVariable UUID frameworkId,
            @PathVariable UUID versionId,
            @Valid @RequestBody UpdateFrameworkVersionStatusRequest request) {
        var command = UpdateFrameworkVersionStatusCommandMapper.fromRequest(frameworkId, versionId, request);
        var updatedVersionId = updateFrameworkVersionStatusUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái phiên bản framework thành công", updatedVersionId));
    }

    @DeleteMapping("/{frameworkId}/versions/{versionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable UUID frameworkId,
            @PathVariable UUID versionId) {
        deleteFrameworkVersionUseCase.execute(new DeleteFrameworkVersionCommand(frameworkId, versionId));
        return ResponseEntity.noContent().build();
    }
}
