package com.sep.vox.interfaces.rest.controller.assessment;

import java.util.UUID;

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

import com.sep.vox.application.port.input.command.DeleteAssessmentPaperCommand;
import com.sep.vox.application.port.input.usecase.assessmentpaper.CreateAssessmentPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.DeleteAssessmentPaperUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperItemUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.UpdateAssessmentPaperStatusUseCase;
import com.sep.vox.domain.dto.assessment.AssessmentPaperDto;
import com.sep.vox.domain.dto.assessment.AssessmentPaperItemDto;
import com.sep.vox.domain.dto.assessment.AssessmentPaperSectionDto;
import com.sep.vox.interfaces.rest.dto.request.assessment.CreateAssessmentPaperRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentPaperItemRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentPaperSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentPaperStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor 
public class RestApiAssessmentPaperController {

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
        var data = createAssessmentPaperUseCase.execute(CreateAssessmentPaperCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo đề thi thành công", data));
    }

    @PutMapping("/assessment-papers/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentPaperItemDto>> updateItem(
            @PathVariable("id") UUID id,
            @PathVariable("itemId") UUID itemId,
            @Valid @RequestBody UpdateAssessmentPaperItemRequest request) {
        var data = updateAssessmentPaperItemUseCase.execute(UpdateAssessmentPaperItemRequest.toCommand(id, itemId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi trong đề thi thành công", data));
    }

    @PatchMapping("/assessment-papers/{id}/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentPaperSectionDto>> updateSection(
            @PathVariable("id") UUID id,
            @PathVariable("sectionId") UUID sectionId,
            @RequestBody UpdateAssessmentPaperSectionRequest request) {
        var data = updateAssessmentPaperSectionUseCase.execute(UpdateAssessmentPaperSectionRequest.toCommand(id, sectionId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phần đề thi thành công", data));
    }

    @PatchMapping("/assessment-papers/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AssessmentPaperDto>> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAssessmentPaperStatusRequest request) {
        var data = updateAssessmentPaperStatusUseCase.execute(UpdateAssessmentPaperStatusRequest.toCommand(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đề thi thành công", data));
    }

    @DeleteMapping("/assessment-papers/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        deleteAssessmentPaperUseCase.execute(new DeleteAssessmentPaperCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa đề thi thành công"));
    }
}
