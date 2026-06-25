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
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkUseCase;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateFrameworkRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateFrameworkCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/frameworks")
public class FrameworkController {

    private final CreateFrameworkUseCase createFrameworkUseCase;
    private final UpdateFrameworkUseCase updateFrameworkUseCase;
    private final DeleteFrameworkUseCase deleteFrameworkUseCase;

    public FrameworkController(
            CreateFrameworkUseCase createFrameworkUseCase,
            UpdateFrameworkUseCase updateFrameworkUseCase,
            DeleteFrameworkUseCase deleteFrameworkUseCase) {
        this.createFrameworkUseCase = createFrameworkUseCase;
        this.updateFrameworkUseCase = updateFrameworkUseCase;
        this.deleteFrameworkUseCase = deleteFrameworkUseCase;
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
}
