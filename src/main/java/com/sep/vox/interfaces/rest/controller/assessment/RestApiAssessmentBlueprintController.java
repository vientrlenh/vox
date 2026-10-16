package com.sep.vox.interfaces.rest.controller.assessment;

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
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentBlueprintSectionItemRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentBlueprintSlotItemRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentBlueprintVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentBlueprintActiveStatusRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentBlueprintSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentBlueprintSlotRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentBlueprintVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentBlueprintVersionStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor 
public class RestApiAssessmentBlueprintController {

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

    @PostMapping("/assessment-blueprints")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintDto>> create(@Valid @RequestBody CreateAssessmentBlueprintRequest request) {
        var data = createAssessmentBlueprintUseCase.execute(CreateAssessmentBlueprintCommandMapper.fromRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo blueprint đề thi thành công", data));
    }

    @PutMapping("/assessment-blueprints/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintDto>> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentBlueprintRequest request) {
        var data = updateAssessmentBlueprintUseCase.execute(UpdateAssessmentBlueprintCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật blueprint đề thi thành công", data));
    }

    @PatchMapping("/assessment-blueprints/{id}/active")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintDto>> updateActiveStatus(
            @PathVariable("id") UUID id,
            @RequestBody UpdateAssessmentBlueprintActiveStatusRequest request) {
        var data = updateAssessmentBlueprintActiveStatusUseCase.execute(
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
        var data = createAssessmentBlueprintVersionUseCase.execute(CreateAssessmentBlueprintVersionCommandMapper.fromRequest(id, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo version blueprint đề thi thành công", data));
    }

    @PostMapping("/assessment-blueprint-versions/{id}/duplicate")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintVersionDto>> duplicateVersion(@PathVariable("id") UUID id) {
        var data = duplicateAssessmentBlueprintVersionUseCase.execute(new DuplicateAssessmentBlueprintVersionCommand(id));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Nhân bản version blueprint đề thi thành công", data));
    }

    @PutMapping("/assessment-blueprint-versions/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintVersionDto>> updateVersion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentBlueprintVersionRequest request) {
        var data = updateAssessmentBlueprintVersionUseCase.execute(UpdateAssessmentBlueprintVersionCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật version blueprint đề thi thành công", data));
    }

    @PatchMapping("/assessment-blueprint-versions/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintVersionDto>> updateVersionStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentBlueprintVersionStatusRequest request) {
        var data = updateAssessmentBlueprintVersionStatusUseCase.execute(
            UpdateAssessmentBlueprintVersionStatusCommandMapper.fromRequest(id, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái version blueprint đề thi thành công", data));
    }

    @PostMapping("/assessment-blueprint-versions/{versionId}/sections")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentBlueprintSectionDto>> createSection(
            @PathVariable("id") UUID versionId,
            @Valid @RequestBody CreateAssessmentBlueprintSectionItemRequest request) {
        var data = createAssessmentBlueprintSectionUseCase.execute(
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
        var data = updateAssessmentBlueprintSectionUseCase.execute(
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
        var data = createAssessmentBlueprintSlotUseCase.execute(
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
        var data = updateAssessmentBlueprintSlotUseCase.execute(
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
