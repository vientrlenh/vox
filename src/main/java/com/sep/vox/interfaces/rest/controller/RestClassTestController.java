package com.sep.vox.interfaces.rest.controller;

import java.util.List;
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

import com.sep.vox.application.port.input.command.assessment.DeleteClassTestSectionCommand;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClaimClassTestGradingUseCase;
import com.sep.vox.application.port.input.command.assessment.DeleteAssessmentCommand;
import com.sep.vox.application.port.input.usecase.assessment.ChangeClassTestBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateClassTestUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateClassTestQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentUseCase;
import com.sep.vox.application.response.input.assessment.CreateClassTestResponse;
import com.sep.vox.application.response.input.assessment.DeleteAssessmentResponse;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.interfaces.rest.dto.request.assessment.ChangeClassTestBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentgrading.ClaimClassTestGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateClassTestRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateClassTestSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateClassTestQuestionsRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateClassTestSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/class-tests")
@RequiredArgsConstructor
public class RestClassTestController {

    private final CreateClassTestUseCase createClassTestUseCase;
    private final UpdateAssessmentUseCase updateAssessmentUseCase;
    private final UpdateAssessmentStatusUseCase updateAssessmentStatusUseCase;
    private final DeleteAssessmentUseCase deleteAssessmentUseCase;
    private final UpdateClassTestQuestionsUseCase updateClassTestQuestionsUseCase;
    private final CreateClassTestSectionUseCase createClassTestSectionUseCase;
    private final UpdateClassTestSectionUseCase updateClassTestSectionUseCase;
    private final DeleteClassTestSectionUseCase deleteClassTestSectionUseCase;
    private final ChangeClassTestBlueprintUseCase changeClassTestBlueprintUseCase;
    private final ClaimClassTestGradingUseCase claimClassTestGradingUseCase;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CreateClassTestResponse>> create(@Valid @RequestBody CreateClassTestRequest request) {
        CreateClassTestResponse data = createClassTestUseCase.execute(CreateClassTestRequest.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Class test created successfully", data));
    }

    @PutMapping("/{assessmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> update(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody UpdateAssessmentRequest request) {
        AssessmentDto data = updateAssessmentUseCase.execute(UpdateAssessmentRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Class test updated successfully", data));
    }

    @PutMapping("/{assessmentId}/questions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateQuestions(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody UpdateClassTestQuestionsRequest request) {
        AssessmentDto data = updateClassTestQuestionsUseCase.execute(UpdateClassTestQuestionsRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Question list updated successfully", data));
    }

    @PatchMapping("/{assessmentId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateStatus(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody UpdateAssessmentStatusRequest request) {
        AssessmentDto data = updateAssessmentStatusUseCase.execute(UpdateAssessmentStatusRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Class test status updated successfully", data));
    }

    @DeleteMapping("/{assessmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<DeleteAssessmentResponse>> delete(@PathVariable("assessmentId") UUID assessmentId) {
        DeleteAssessmentResponse data = deleteAssessmentUseCase.execute(new DeleteAssessmentCommand(assessmentId));
        return ResponseEntity.ok(ApiResponse.success("Class test deleted successfully", data));
    }

    @PostMapping("/{assessmentId}/sections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> createSection(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody CreateClassTestSectionRequest request) {
        AssessmentDto data = createClassTestSectionUseCase.execute(CreateClassTestSectionRequest.toCommand(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Section created successfully", data));
    }

    @PutMapping("/{assessmentId}/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateSection(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("sectionId") UUID sectionId,
            @RequestBody UpdateClassTestSectionRequest request) {
        AssessmentDto data = updateClassTestSectionUseCase.execute(
            UpdateClassTestSectionRequest.toCommand(assessmentId, sectionId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Section updated successfully", data));
    }

    @DeleteMapping("/{assessmentId}/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> deleteSection(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("sectionId") UUID sectionId) {
        AssessmentDto data = deleteClassTestSectionUseCase.execute(new DeleteClassTestSectionCommand(assessmentId, sectionId));
        return ResponseEntity.ok(ApiResponse.success("Section deleted successfully", data));
    }

    @PatchMapping("/{assessmentId}/blueprint")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> changeBlueprint(
            @PathVariable("assessmentId") UUID assessmentId,
            @RequestBody ChangeClassTestBlueprintRequest request) {
        AssessmentDto data = changeClassTestBlueprintUseCase.execute(ChangeClassTestBlueprintRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Blueprint of the class test updated successfully", data));
    }

    /**
     * A teacher creates a self-claimed grading. It is placed here instead of widening the permission of
     * {@code /api/v1/grading-assignments}: widening it there would be open to every teacher on every
     * assessment, while here the scope is closed to exactly the results where the caller is CHAIR.
     */
    @PostMapping("/{assessmentId}/grading/claim")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<UUID>>> claimGrading(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody ClaimClassTestGradingRequest request) {
        List<UUID> data = claimClassTestGradingUseCase.execute(
            ClaimClassTestGradingRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Grading claimed successfully", data));
    }
}
