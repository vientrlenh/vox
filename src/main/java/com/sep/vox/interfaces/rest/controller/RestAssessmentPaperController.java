package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.assessmentpaper.DeleteAssessmentPaperCommand;
import com.sep.vox.application.port.input.usecase.assessmentpaper.CreateAssessmentPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.DeleteAssessmentPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperItemUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperStatusUseCase;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.dto.AssessmentPaperItemDto;
import com.sep.vox.domain.dto.AssessmentPaperSectionDto;
import com.sep.vox.interfaces.rest.dto.request.assessmentpaper.CreateAssessmentPaperRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentpaper.UpdateAssessmentPaperItemRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentpaper.UpdateAssessmentPaperSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentpaper.UpdateAssessmentPaperStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RestAssessmentPaperController {

    private final CreateAssessmentPaperUseCase createAssessmentPaperUseCase;
    private final UpdateAssessmentPaperItemUseCase updateAssessmentPaperItemUseCase;
    private final UpdateAssessmentPaperSectionUseCase updateAssessmentPaperSectionUseCase;
    private final UpdateAssessmentPaperStatusUseCase updateAssessmentPaperStatusUseCase;
    private final DeleteAssessmentPaperUseCase deleteAssessmentPaperUseCase;

    @PostMapping("/assessments/{assessmentId}/papers")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentPaperDto>> create(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody(required = false) CreateAssessmentPaperRequest request) {
        AssessmentPaperDto data = createAssessmentPaperUseCase.execute(CreateAssessmentPaperRequest.toCommand(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Assessment paper created successfully", data));
    }

    @PutMapping("/assessment-papers/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentPaperItemDto>> updateItem(
            @PathVariable("id") UUID id,
            @PathVariable("itemId") UUID itemId,
            @Valid @RequestBody UpdateAssessmentPaperItemRequest request) {
        AssessmentPaperItemDto data = updateAssessmentPaperItemUseCase.execute(UpdateAssessmentPaperItemRequest.toCommand(id, itemId, request));
        return ResponseEntity.ok(ApiResponse.success("Question in the assessment paper updated successfully", data));
    }

    @PatchMapping("/assessment-papers/{id}/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentPaperSectionDto>> updateSection(
            @PathVariable("id") UUID id,
            @PathVariable("sectionId") UUID sectionId,
            @RequestBody UpdateAssessmentPaperSectionRequest request) {
        AssessmentPaperSectionDto data = updateAssessmentPaperSectionUseCase.execute(UpdateAssessmentPaperSectionRequest.toCommand(id, sectionId, request));
        return ResponseEntity.ok(ApiResponse.success("Assessment paper section updated successfully", data));
    }

    @PatchMapping("/assessment-papers/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentPaperDto>> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentPaperStatusRequest request) {
        AssessmentPaperDto data = updateAssessmentPaperStatusUseCase.execute(UpdateAssessmentPaperStatusRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Assessment paper status updated successfully", data));
    }

    @DeleteMapping("/assessment-papers/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        deleteAssessmentPaperUseCase.execute(new DeleteAssessmentPaperCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Assessment paper deleted successfully"));
    }
}
