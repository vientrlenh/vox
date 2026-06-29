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

import com.sep.vox.application.port.input.command.DeleteExamBlueprintCommand;
import com.sep.vox.application.port.input.command.DeleteExamBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.DeleteExamBlueprintSlotCommand;
import com.sep.vox.application.port.input.usecase.examblueprint.CreateExamBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.CreateExamBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.CreateExamBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.CreateExamBlueprintVersionUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.DeleteExamBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.DeleteExamBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.DeleteExamBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.UpdateExamBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.UpdateExamBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.UpdateExamBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.UpdateExamBlueprintVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.UpdateExamBlueprintVersionUseCase;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.dto.ExamBlueprintSectionDto;
import com.sep.vox.domain.dto.ExamBlueprintSlotDto;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.interfaces.rest.dto.request.CreateExamBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamBlueprintSectionItemRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamBlueprintSlotItemRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateExamBlueprintVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintSlotRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintVersionStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateExamBlueprintCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateExamBlueprintSectionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateExamBlueprintSlotCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateExamBlueprintVersionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamBlueprintCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamBlueprintSectionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamBlueprintSlotCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamBlueprintVersionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamBlueprintVersionStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ExamBlueprintController {

    private final CreateExamBlueprintUseCase createExamBlueprintUseCase;
    private final UpdateExamBlueprintUseCase updateExamBlueprintUseCase;
    private final DeleteExamBlueprintUseCase deleteExamBlueprintUseCase;
    private final CreateExamBlueprintVersionUseCase createExamBlueprintVersionUseCase;
    private final UpdateExamBlueprintVersionUseCase updateExamBlueprintVersionUseCase;
    private final UpdateExamBlueprintVersionStatusUseCase updateExamBlueprintVersionStatusUseCase;
    private final CreateExamBlueprintSectionUseCase createExamBlueprintSectionUseCase;
    private final UpdateExamBlueprintSectionUseCase updateExamBlueprintSectionUseCase;
    private final DeleteExamBlueprintSectionUseCase deleteExamBlueprintSectionUseCase;
    private final CreateExamBlueprintSlotUseCase createExamBlueprintSlotUseCase;
    private final UpdateExamBlueprintSlotUseCase updateExamBlueprintSlotUseCase;
    private final DeleteExamBlueprintSlotUseCase deleteExamBlueprintSlotUseCase;

    public ExamBlueprintController(
            CreateExamBlueprintUseCase createExamBlueprintUseCase,
            UpdateExamBlueprintUseCase updateExamBlueprintUseCase,
            DeleteExamBlueprintUseCase deleteExamBlueprintUseCase,
            CreateExamBlueprintVersionUseCase createExamBlueprintVersionUseCase,
            UpdateExamBlueprintVersionUseCase updateExamBlueprintVersionUseCase,
            UpdateExamBlueprintVersionStatusUseCase updateExamBlueprintVersionStatusUseCase,
            CreateExamBlueprintSectionUseCase createExamBlueprintSectionUseCase,
            UpdateExamBlueprintSectionUseCase updateExamBlueprintSectionUseCase,
            DeleteExamBlueprintSectionUseCase deleteExamBlueprintSectionUseCase,
            CreateExamBlueprintSlotUseCase createExamBlueprintSlotUseCase,
            UpdateExamBlueprintSlotUseCase updateExamBlueprintSlotUseCase,
            DeleteExamBlueprintSlotUseCase deleteExamBlueprintSlotUseCase) {
        this.createExamBlueprintUseCase = createExamBlueprintUseCase;
        this.updateExamBlueprintUseCase = updateExamBlueprintUseCase;
        this.deleteExamBlueprintUseCase = deleteExamBlueprintUseCase;
        this.createExamBlueprintVersionUseCase = createExamBlueprintVersionUseCase;
        this.updateExamBlueprintVersionUseCase = updateExamBlueprintVersionUseCase;
        this.updateExamBlueprintVersionStatusUseCase = updateExamBlueprintVersionStatusUseCase;
        this.createExamBlueprintSectionUseCase = createExamBlueprintSectionUseCase;
        this.updateExamBlueprintSectionUseCase = updateExamBlueprintSectionUseCase;
        this.deleteExamBlueprintSectionUseCase = deleteExamBlueprintSectionUseCase;
        this.createExamBlueprintSlotUseCase = createExamBlueprintSlotUseCase;
        this.updateExamBlueprintSlotUseCase = updateExamBlueprintSlotUseCase;
        this.deleteExamBlueprintSlotUseCase = deleteExamBlueprintSlotUseCase;
    }

    @PostMapping("/exam-blueprints")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintDto>> create(@Valid @RequestBody CreateExamBlueprintRequest request) {
        var data = createExamBlueprintUseCase.execute(CreateExamBlueprintCommandMapper.fromRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo blueprint đề thi thành công", data));
    }

    @PutMapping("/exam-blueprints/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamBlueprintRequest request) {
        var data = updateExamBlueprintUseCase.execute(UpdateExamBlueprintCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật blueprint đề thi thành công", data));
    }

    @DeleteMapping("/exam-blueprints/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        deleteExamBlueprintUseCase.execute(new DeleteExamBlueprintCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa blueprint đề thi thành công"));
    }

    @PostMapping("/exam-blueprints/{id}/versions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintVersionDto>> createVersion(
            @PathVariable UUID id,
            @Valid @RequestBody CreateExamBlueprintVersionRequest request) {
        var data = createExamBlueprintVersionUseCase.execute(CreateExamBlueprintVersionCommandMapper.fromRequest(id, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo version blueprint đề thi thành công", data));
    }

    @PutMapping("/exam-blueprint-versions/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintVersionDto>> updateVersion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamBlueprintVersionRequest request) {
        var data = updateExamBlueprintVersionUseCase.execute(UpdateExamBlueprintVersionCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật version blueprint đề thi thành công", data));
    }

    @PatchMapping("/exam-blueprint-versions/{id}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintVersionDto>> updateVersionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamBlueprintVersionStatusRequest request) {
        var data = updateExamBlueprintVersionStatusUseCase.execute(
            UpdateExamBlueprintVersionStatusCommandMapper.fromRequest(id, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái version blueprint đề thi thành công", data));
    }

    @PostMapping("/exam-blueprint-versions/{versionId}/sections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintSectionDto>> createSection(
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateExamBlueprintSectionItemRequest request) {
        var data = createExamBlueprintSectionUseCase.execute(
            CreateExamBlueprintSectionCommandMapper.fromRequest(versionId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo section thành công", data));
    }

    @PutMapping("/exam-blueprint-sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintSectionDto>> updateSection(
            @PathVariable UUID sectionId,
            @Valid @RequestBody UpdateExamBlueprintSectionRequest request) {
        var data = updateExamBlueprintSectionUseCase.execute(
            UpdateExamBlueprintSectionCommandMapper.fromRequest(sectionId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật section thành công", data));
    }

    @DeleteMapping("/exam-blueprint-sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID sectionId) {
        deleteExamBlueprintSectionUseCase.execute(new DeleteExamBlueprintSectionCommand(sectionId));
        return ResponseEntity.ok(ApiResponse.success("Xóa section thành công"));
    }

    @PostMapping("/exam-blueprint-sections/{sectionId}/slots")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintSlotDto>> createSlot(
            @PathVariable UUID sectionId,
            @Valid @RequestBody CreateExamBlueprintSlotItemRequest request) {
        var data = createExamBlueprintSlotUseCase.execute(
            CreateExamBlueprintSlotCommandMapper.fromRequest(sectionId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo slot thành công", data));
    }

    @PutMapping("/exam-blueprint-slots/{slotId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamBlueprintSlotDto>> updateSlot(
            @PathVariable UUID slotId,
            @Valid @RequestBody UpdateExamBlueprintSlotRequest request) {
        var data = updateExamBlueprintSlotUseCase.execute(
            UpdateExamBlueprintSlotCommandMapper.fromRequest(slotId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật slot thành công", data));
    }

    @DeleteMapping("/exam-blueprint-slots/{slotId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteSlot(@PathVariable UUID slotId) {
        deleteExamBlueprintSlotUseCase.execute(new DeleteExamBlueprintSlotCommand(slotId));
        return ResponseEntity.ok(ApiResponse.success("Xóa slot thành công"));
    }
}
