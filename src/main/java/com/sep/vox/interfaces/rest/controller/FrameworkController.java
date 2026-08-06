package com.sep.vox.interfaces.rest.controller;

import java.util.List;
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
import com.sep.vox.application.port.input.command.UpdateFrameworkActiveStatusCommand;
import com.sep.vox.application.port.input.command.DeleteFrameworkCriterionBandCommand;
import com.sep.vox.application.port.input.command.DeleteFrameworkCriterionCommand;
import com.sep.vox.application.port.input.command.DeleteFrameworkResultBandCommand;
import com.sep.vox.application.port.input.command.DeleteFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkCriterionBandsUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkCriterionBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkCriterionUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkResultBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkStatusUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionStatusUseCase;
import com.sep.vox.application.response.input.framework.CreateFrameworkVersionResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkCriteriaRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkCriterionBandsRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkResultBandsRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateFrameworkVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateFrameworkRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateFrameworkVersionStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkCriteriaCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkCriterionBandsCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkResultBandsCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateFrameworkVersionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateFrameworkCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateFrameworkVersionStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/frameworks")
public class FrameworkController {

    private final CreateFrameworkUseCase createFrameworkUseCase;
    private final UpdateFrameworkUseCase updateFrameworkUseCase;
    private final UpdateFrameworkStatusUseCase updateFrameworkStatusUseCase;
    private final DeleteFrameworkUseCase deleteFrameworkUseCase;
    private final CreateFrameworkVersionUseCase createFrameworkVersionUseCase;
    private final UpdateFrameworkVersionStatusUseCase updateFrameworkVersionStatusUseCase;
    private final DeleteFrameworkVersionUseCase deleteFrameworkVersionUseCase;
    private final CreateFrameworkCriterionBandsUseCase createFrameworkCriterionBandsUseCase;
    private final CreateFrameworkResultBandsUseCase createFrameworkResultBandsUseCase;
    private final DeleteFrameworkResultBandUseCase deleteFrameworkResultBandUseCase;
    private final DeleteFrameworkCriterionBandUseCase deleteFrameworkCriterionBandUseCase;
    private final CreateFrameworkCriteriaUseCase createFrameworkCriteriaUseCase;
    private final DeleteFrameworkCriterionUseCase deleteFrameworkCriterionUseCase;

    public FrameworkController(
            CreateFrameworkUseCase createFrameworkUseCase,
            UpdateFrameworkUseCase updateFrameworkUseCase,
            UpdateFrameworkStatusUseCase updateFrameworkStatusUseCase,
            DeleteFrameworkUseCase deleteFrameworkUseCase,
            CreateFrameworkVersionUseCase createFrameworkVersionUseCase,
            UpdateFrameworkVersionStatusUseCase updateFrameworkVersionStatusUseCase,
            DeleteFrameworkVersionUseCase deleteFrameworkVersionUseCase,
            CreateFrameworkCriterionBandsUseCase createFrameworkCriterionBandsUseCase,
            CreateFrameworkResultBandsUseCase createFrameworkResultBandsUseCase,
            DeleteFrameworkResultBandUseCase deleteFrameworkResultBandUseCase,
            DeleteFrameworkCriterionBandUseCase deleteFrameworkCriterionBandUseCase,
            CreateFrameworkCriteriaUseCase createFrameworkCriteriaUseCase,
            DeleteFrameworkCriterionUseCase deleteFrameworkCriterionUseCase) {
        this.createFrameworkUseCase = createFrameworkUseCase;
        this.updateFrameworkUseCase = updateFrameworkUseCase;
        this.updateFrameworkStatusUseCase = updateFrameworkStatusUseCase;
        this.deleteFrameworkUseCase = deleteFrameworkUseCase;
        this.createFrameworkVersionUseCase = createFrameworkVersionUseCase;
        this.updateFrameworkVersionStatusUseCase = updateFrameworkVersionStatusUseCase;
        this.deleteFrameworkVersionUseCase = deleteFrameworkVersionUseCase;
        this.createFrameworkCriterionBandsUseCase = createFrameworkCriterionBandsUseCase;
        this.createFrameworkResultBandsUseCase = createFrameworkResultBandsUseCase;
        this.deleteFrameworkResultBandUseCase = deleteFrameworkResultBandUseCase;
        this.deleteFrameworkCriterionBandUseCase = deleteFrameworkCriterionBandUseCase;
        this.createFrameworkCriteriaUseCase = createFrameworkCriteriaUseCase;
        this.deleteFrameworkCriterionUseCase = deleteFrameworkCriterionUseCase;
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
            @PathVariable(name = "frameworkId") UUID frameworkId,
            @Valid @RequestBody UpdateFrameworkRequest request) {
        var command = UpdateFrameworkCommandMapper.fromRequest(frameworkId, request);
        var id = updateFrameworkUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật framework thành công", id));
    }

    @PatchMapping("/{frameworkId}/activate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> activateFramework(@PathVariable(name = "frameworkId") UUID frameworkId) {
        var id = updateFrameworkStatusUseCase.execute(new UpdateFrameworkActiveStatusCommand(frameworkId, true));
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt framework thành công", id));
    }

    @PatchMapping("/{frameworkId}/deactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> deactivateFramework(@PathVariable(name = "frameworkId") UUID frameworkId) {
        var id = updateFrameworkStatusUseCase.execute(new UpdateFrameworkActiveStatusCommand(frameworkId, false));
        return ResponseEntity.ok(ApiResponse.success("Vô hiệu hóa framework thành công", id));
    }

    @DeleteMapping("/{frameworkId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteFramework(@PathVariable(name = "frameworkId") UUID frameworkId) {
        deleteFrameworkUseCase.execute(new DeleteFrameworkCommand(frameworkId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{frameworkId}/versions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateFrameworkVersionResponse>> createVersion(
            @PathVariable(name = "frameworkId") UUID frameworkId,
            @Valid @RequestBody CreateFrameworkVersionRequest request) {
        var command = CreateFrameworkVersionCommandMapper.fromRequest(frameworkId, request);
        var data = createFrameworkVersionUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo phiên bản framework thành công", data));
    }

    @PatchMapping("/{frameworkId}/versions/{versionId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> updateVersionStatus(
            @PathVariable(name = "frameworkId") UUID frameworkId,
            @PathVariable(name = "versionId") UUID versionId,
            @Valid @RequestBody UpdateFrameworkVersionStatusRequest request) {
        var command = UpdateFrameworkVersionStatusCommandMapper.fromRequest(frameworkId, versionId, request);
        var updatedVersionId = updateFrameworkVersionStatusUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái phiên bản framework thành công", updatedVersionId));
    }

    @DeleteMapping("/{frameworkId}/versions/{versionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable(name = "frameworkId") UUID frameworkId,
            @PathVariable(name = "versionId") UUID versionId) {
        deleteFrameworkVersionUseCase.execute(new DeleteFrameworkVersionCommand(frameworkId, versionId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{frameworkId}/versions/{versionId}/criteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createCriteria(
            @PathVariable(name = "frameworkId") UUID frameworkId,
            @PathVariable(name = "versionId") UUID versionId,
            @Valid @RequestBody CreateFrameworkCriteriaRequest request) {
        var command = CreateFrameworkCriteriaCommandMapper.fromRequest(frameworkId, versionId, request);
        var ids = createFrameworkCriteriaUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Thêm tiêu chí thành công", ids));
    }

    @DeleteMapping("/{frameworkId}/versions/{versionId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteCriterion(
            @PathVariable(name = "frameworkId") UUID frameworkId, @PathVariable(name = "versionId") UUID versionId, @PathVariable(name = "criterionId") UUID criterionId) {
        deleteFrameworkCriterionUseCase.execute(
                new DeleteFrameworkCriterionCommand(frameworkId, versionId, criterionId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{frameworkId}/versions/{versionId}/criteria/{criterionId}/bands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createCriterionBands(
            @PathVariable(name = "frameworkId") UUID frameworkId,
            @PathVariable(name = "versionId") UUID versionId,
            @PathVariable(name = "criterionId") UUID criterionId,
            @Valid @RequestBody CreateFrameworkCriterionBandsRequest request) {
        var command = CreateFrameworkCriterionBandsCommandMapper.fromRequest(frameworkId, versionId, criterionId, request);
        var ids = createFrameworkCriterionBandsUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Thêm mức đánh giá cho tiêu chí thành công", ids));
    }

    @PostMapping("/{frameworkId}/versions/{versionId}/result-bands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createResultBands(
            @PathVariable(name = "frameworkId") UUID frameworkId,
            @PathVariable(name = "versionId") UUID versionId,
            @Valid @RequestBody CreateFrameworkResultBandsRequest request) {
        var command = CreateFrameworkResultBandsCommandMapper.fromRequest(frameworkId, versionId, request);
        var ids = createFrameworkResultBandsUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Thêm mức kết quả thành công", ids));
    }

    @DeleteMapping("/{frameworkId}/versions/{versionId}/result-bands/{bandId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteResultBand(
            @PathVariable(name = "frameworkId") UUID frameworkId, @PathVariable(name = "versionId") UUID versionId, @PathVariable(name = "bandId") UUID bandId) {
        deleteFrameworkResultBandUseCase.execute(new DeleteFrameworkResultBandCommand(frameworkId, versionId, bandId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{frameworkId}/versions/{versionId}/criteria/{criterionId}/bands/{bandId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteCriterionBand(
            @PathVariable(name = "frameworkId") UUID frameworkId, @PathVariable(name = "versionId") UUID versionId,
            @PathVariable(name = "criterionId") UUID criterionId, @PathVariable(name = "bandId") UUID bandId) {
        deleteFrameworkCriterionBandUseCase.execute(
                new DeleteFrameworkCriterionBandCommand(frameworkId, versionId, criterionId, bandId));
        return ResponseEntity.noContent().build();
    }
}
