package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.DeleteAssessmentBlueprintCommand;
import com.sep.vox.application.port.input.command.DeleteAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.DeleteAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.DeleteAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.DuplicateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintActiveStatusCommand;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.CreateAssessmentBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.CreateAssessmentBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.CreateAssessmentBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.CreateAssessmentBlueprintVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DeleteAssessmentBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DeleteAssessmentBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DeleteAssessmentBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DeleteAssessmentBlueprintVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DuplicateAssessmentBlueprintVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintActiveStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintVersionUseCase;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentBlueprintSectionItemRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentBlueprintSlotItemRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentBlueprintVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintActiveStatusRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintSlotRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintVersionStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateAssessmentBlueprintCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateAssessmentBlueprintSectionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateAssessmentBlueprintSlotCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateAssessmentBlueprintVersionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateAssessmentBlueprintCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateAssessmentBlueprintSectionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateAssessmentBlueprintSlotCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateAssessmentBlueprintVersionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateAssessmentBlueprintVersionStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AssessmentBlueprintController {

    private final CreateAssessmentBlueprintUseCase createAssessmentBlueprintUseCase;
    private final UpdateAssessmentBlueprintUseCase updateAssessmentBlueprintUseCase;
    private final UpdateAssessmentBlueprintActiveStatusUseCase updateAssessmentBlueprintActiveStatusUseCase;
    private final DeleteAssessmentBlueprintUseCase deleteAssessmentBlueprintUseCase;
    private final CreateAssessmentBlueprintVersionUseCase createAssessmentBlueprintVersionUseCase;
    private final DuplicateAssessmentBlueprintVersionUseCase duplicateAssessmentBlueprintVersionUseCase;
    private final UpdateAssessmentBlueprintVersionUseCase updateAssessmentBlueprintVersionUseCase;
    private final UpdateAssessmentBlueprintVersionStatusUseCase updateAssessmentBlueprintVersionStatusUseCase;
    private final DeleteAssessmentBlueprintVersionUseCase deleteAssessmentBlueprintVersionUseCase;
    private final CreateAssessmentBlueprintSectionUseCase createAssessmentBlueprintSectionUseCase;
    private final UpdateAssessmentBlueprintSectionUseCase updateAssessmentBlueprintSectionUseCase;
    private final DeleteAssessmentBlueprintSectionUseCase deleteAssessmentBlueprintSectionUseCase;
    private final CreateAssessmentBlueprintSlotUseCase createAssessmentBlueprintSlotUseCase;
    private final UpdateAssessmentBlueprintSlotUseCase updateAssessmentBlueprintSlotUseCase;
    private final DeleteAssessmentBlueprintSlotUseCase deleteAssessmentBlueprintSlotUseCase;

    public AssessmentBlueprintController(
            CreateAssessmentBlueprintUseCase createAssessmentBlueprintUseCase,
            UpdateAssessmentBlueprintUseCase updateAssessmentBlueprintUseCase,
            UpdateAssessmentBlueprintActiveStatusUseCase updateAssessmentBlueprintActiveStatusUseCase,
            DeleteAssessmentBlueprintUseCase deleteAssessmentBlueprintUseCase,
            CreateAssessmentBlueprintVersionUseCase createAssessmentBlueprintVersionUseCase,
            DuplicateAssessmentBlueprintVersionUseCase duplicateAssessmentBlueprintVersionUseCase,
            UpdateAssessmentBlueprintVersionUseCase updateAssessmentBlueprintVersionUseCase,
            UpdateAssessmentBlueprintVersionStatusUseCase updateAssessmentBlueprintVersionStatusUseCase,
            DeleteAssessmentBlueprintVersionUseCase deleteAssessmentBlueprintVersionUseCase,
            CreateAssessmentBlueprintSectionUseCase createAssessmentBlueprintSectionUseCase,
            UpdateAssessmentBlueprintSectionUseCase updateAssessmentBlueprintSectionUseCase,
            DeleteAssessmentBlueprintSectionUseCase deleteAssessmentBlueprintSectionUseCase,
            CreateAssessmentBlueprintSlotUseCase createAssessmentBlueprintSlotUseCase,
            UpdateAssessmentBlueprintSlotUseCase updateAssessmentBlueprintSlotUseCase,
            DeleteAssessmentBlueprintSlotUseCase deleteAssessmentBlueprintSlotUseCase) {
        this.createAssessmentBlueprintUseCase = createAssessmentBlueprintUseCase;
        this.updateAssessmentBlueprintUseCase = updateAssessmentBlueprintUseCase;
        this.updateAssessmentBlueprintActiveStatusUseCase = updateAssessmentBlueprintActiveStatusUseCase;
        this.deleteAssessmentBlueprintUseCase = deleteAssessmentBlueprintUseCase;
        this.createAssessmentBlueprintVersionUseCase = createAssessmentBlueprintVersionUseCase;
        this.duplicateAssessmentBlueprintVersionUseCase = duplicateAssessmentBlueprintVersionUseCase;
        this.updateAssessmentBlueprintVersionUseCase = updateAssessmentBlueprintVersionUseCase;
        this.updateAssessmentBlueprintVersionStatusUseCase = updateAssessmentBlueprintVersionStatusUseCase;
        this.deleteAssessmentBlueprintVersionUseCase = deleteAssessmentBlueprintVersionUseCase;
        this.createAssessmentBlueprintSectionUseCase = createAssessmentBlueprintSectionUseCase;
        this.updateAssessmentBlueprintSectionUseCase = updateAssessmentBlueprintSectionUseCase;
        this.deleteAssessmentBlueprintSectionUseCase = deleteAssessmentBlueprintSectionUseCase;
        this.createAssessmentBlueprintSlotUseCase = createAssessmentBlueprintSlotUseCase;
        this.updateAssessmentBlueprintSlotUseCase = updateAssessmentBlueprintSlotUseCase;
        this.deleteAssessmentBlueprintSlotUseCase = deleteAssessmentBlueprintSlotUseCase;
    }

    @PostMapping("/assessment-blueprints")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintDto>> create(@Valid @RequestBody CreateAssessmentBlueprintRequest request) {
        AssessmentBlueprintDto data = createAssessmentBlueprintUseCase.execute(CreateAssessmentBlueprintCommandMapper.fromRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo blueprint đề thi thành công", data));
    }

    @PutMapping("/assessment-blueprints/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintDto>> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentBlueprintRequest request) {
        AssessmentBlueprintDto data = updateAssessmentBlueprintUseCase.execute(UpdateAssessmentBlueprintCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật blueprint đề thi thành công", data));
    }

    @PatchMapping("/assessment-blueprints/{id}/active")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintDto>> updateActiveStatus(
            @PathVariable("id") UUID id,
            @RequestBody UpdateAssessmentBlueprintActiveStatusRequest request) {
        AssessmentBlueprintDto data = updateAssessmentBlueprintActiveStatusUseCase.execute(
            new UpdateAssessmentBlueprintActiveStatusCommand(id, request.isActive())
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái kích hoạt blueprint thành công", data));
    }

    @DeleteMapping("/assessment-blueprints/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        deleteAssessmentBlueprintUseCase.execute(new DeleteAssessmentBlueprintCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa blueprint đề thi thành công"));
    }

    @PostMapping("/assessment-blueprints/{id}/versions")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintVersionDto>> createVersion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateAssessmentBlueprintVersionRequest request) {
        AssessmentBlueprintVersionDto data = createAssessmentBlueprintVersionUseCase.execute(CreateAssessmentBlueprintVersionCommandMapper.fromRequest(id, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo version blueprint đề thi thành công", data));
    }

    @PostMapping("/assessment-blueprint-versions/{id}/duplicate")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintVersionDto>> duplicateVersion(@PathVariable("id") UUID id) {
        AssessmentBlueprintVersionDto data = duplicateAssessmentBlueprintVersionUseCase.execute(new DuplicateAssessmentBlueprintVersionCommand(id));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Nhân bản version blueprint đề thi thành công", data));
    }

    @PutMapping("/assessment-blueprint-versions/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintVersionDto>> updateVersion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentBlueprintVersionRequest request) {
        AssessmentBlueprintVersionDto data = updateAssessmentBlueprintVersionUseCase.execute(UpdateAssessmentBlueprintVersionCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật version blueprint đề thi thành công", data));
    }

    @PatchMapping("/assessment-blueprint-versions/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintVersionDto>> updateVersionStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentBlueprintVersionStatusRequest request) {
        AssessmentBlueprintVersionDto data = updateAssessmentBlueprintVersionStatusUseCase.execute(
            UpdateAssessmentBlueprintVersionStatusCommandMapper.fromRequest(id, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái version blueprint đề thi thành công", data));
    }

    @PostMapping("/assessment-blueprint-versions/{versionId}/sections")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintSectionDto>> createSection(
            @PathVariable("id") UUID versionId,
            @Valid @RequestBody CreateAssessmentBlueprintSectionItemRequest request) {
        AssessmentBlueprintSectionDto data = createAssessmentBlueprintSectionUseCase.execute(
            CreateAssessmentBlueprintSectionCommandMapper.fromRequest(versionId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo section thành công", data));
    }

    @PutMapping("/assessment-blueprint-sections/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintSectionDto>> updateSection(
            @PathVariable("sectionId") UUID sectionId,
            @Valid @RequestBody UpdateAssessmentBlueprintSectionRequest request) {
        AssessmentBlueprintSectionDto data = updateAssessmentBlueprintSectionUseCase.execute(
            UpdateAssessmentBlueprintSectionCommandMapper.fromRequest(sectionId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật section thành công", data));
    }

    @DeleteMapping("/assessment-blueprint-sections/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable("sectionId") UUID sectionId) {
        deleteAssessmentBlueprintSectionUseCase.execute(new DeleteAssessmentBlueprintSectionCommand(sectionId));
        return ResponseEntity.ok(ApiResponse.success("Xóa section thành công"));
    }

    @PostMapping("/assessment-blueprint-sections/{sectionId}/slots")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintSlotDto>> createSlot(
            @PathVariable("sectionId") UUID sectionId,
            @Valid @RequestBody CreateAssessmentBlueprintSlotItemRequest request) {
        AssessmentBlueprintSlotDto data = createAssessmentBlueprintSlotUseCase.execute(
            CreateAssessmentBlueprintSlotCommandMapper.fromRequest(sectionId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo slot thành công", data));
    }

    @PutMapping("/assessment-blueprint-slots/{slotId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintSlotDto>> updateSlot(
            @PathVariable("slotId") UUID slotId,
            @Valid @RequestBody UpdateAssessmentBlueprintSlotRequest request) {
        AssessmentBlueprintSlotDto data = updateAssessmentBlueprintSlotUseCase.execute(
            UpdateAssessmentBlueprintSlotCommandMapper.fromRequest(slotId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật slot thành công", data));
    }

    @DeleteMapping("/assessment-blueprint-slots/{slotId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSlot(@PathVariable("slotId") UUID slotId) {
        deleteAssessmentBlueprintSlotUseCase.execute(new DeleteAssessmentBlueprintSlotCommand(slotId));
        return ResponseEntity.ok(ApiResponse.success("Xóa slot thành công"));
    }

    @DeleteMapping("/assessment-blueprint-versions/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(@PathVariable("id") UUID id) {
        deleteAssessmentBlueprintVersionUseCase.execute(new DeleteAssessmentBlueprintVersionCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa phiên bản blueprint đề thi thành công"));
    }
}
