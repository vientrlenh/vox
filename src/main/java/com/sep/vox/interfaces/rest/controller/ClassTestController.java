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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.DeleteClassTestSectionCommand;
import com.sep.vox.application.port.input.usecase.assessment.ChangeClassTestBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateClassTestUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateClassTestQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClaimClassTestGradingUseCase;
import com.sep.vox.application.response.input.assessment.CreateClassTestResponse;
import com.sep.vox.application.response.input.assessment.DeleteAssessmentResponse;
import com.sep.vox.domain.dto.assessment.AssessmentDto;
import com.sep.vox.application.port.input.command.DeleteAssessmentCommand;
import com.sep.vox.interfaces.rest.dto.request.ChangeClassTestBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.ClaimClassTestGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateClassTestRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateClassTestSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateClassTestQuestionsRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateClassTestSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentRequest;
import com.sep.vox.interfaces.rest.dto.request.assessment.UpdateAssessmentStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateClassTestSectionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateClassTestQuestionsCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateClassTestSectionCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/class-tests")
public class ClassTestController {

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

    public ClassTestController(
            CreateClassTestUseCase createClassTestUseCase,
            UpdateAssessmentUseCase updateAssessmentUseCase,
            UpdateAssessmentStatusUseCase updateAssessmentStatusUseCase,
            DeleteAssessmentUseCase deleteAssessmentUseCase,
            UpdateClassTestQuestionsUseCase updateClassTestQuestionsUseCase,
            CreateClassTestSectionUseCase createClassTestSectionUseCase,
            UpdateClassTestSectionUseCase updateClassTestSectionUseCase,
            DeleteClassTestSectionUseCase deleteClassTestSectionUseCase,
            ChangeClassTestBlueprintUseCase changeClassTestBlueprintUseCase,
            ClaimClassTestGradingUseCase claimClassTestGradingUseCase) {
        this.createClassTestUseCase = createClassTestUseCase;
        this.updateAssessmentUseCase = updateAssessmentUseCase;
        this.updateAssessmentStatusUseCase = updateAssessmentStatusUseCase;
        this.deleteAssessmentUseCase = deleteAssessmentUseCase;
        this.updateClassTestQuestionsUseCase = updateClassTestQuestionsUseCase;
        this.createClassTestSectionUseCase = createClassTestSectionUseCase;
        this.updateClassTestSectionUseCase = updateClassTestSectionUseCase;
        this.deleteClassTestSectionUseCase = deleteClassTestSectionUseCase;
        this.changeClassTestBlueprintUseCase = changeClassTestBlueprintUseCase;
        this.claimClassTestGradingUseCase = claimClassTestGradingUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CreateClassTestResponse>> create(@Valid @RequestBody CreateClassTestRequest request) {
        var data = createClassTestUseCase.execute(CreateClassTestRequest.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo bài kiểm tra trên lớp thành công", data));
    }

    @PutMapping("/{assessmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> update(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody UpdateAssessmentRequest request) {
        var data = updateAssessmentUseCase.execute(UpdateAssessmentRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài kiểm tra trên lớp thành công", data));
    }

    @PutMapping("/{assessmentId}/questions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateQuestions(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody UpdateClassTestQuestionsRequest request) {
        var data = updateClassTestQuestionsUseCase.execute(UpdateClassTestQuestionsCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh sách câu hỏi thành công", data));
    }

    @PatchMapping("/{assessmentId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateStatus(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody UpdateAssessmentStatusRequest request) {
        var data = updateAssessmentStatusUseCase.execute(UpdateAssessmentStatusRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái bài kiểm tra trên lớp thành công", data));
    }

    @DeleteMapping("/{assessmentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<DeleteAssessmentResponse>> delete(@PathVariable("assessmentId") UUID assessmentId) {
        var data = deleteAssessmentUseCase.execute(new DeleteAssessmentCommand(assessmentId));
        return ResponseEntity.ok(ApiResponse.success("Xóa bài kiểm tra trên lớp thành công", data));
    }

    @PostMapping("/{assessmentId}/sections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> createSection(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody CreateClassTestSectionRequest request) {
        var data = createClassTestSectionUseCase.execute(CreateClassTestSectionCommandMapper.fromRequest(assessmentId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo section thành công", data));
    }

    @PutMapping("/{assessmentId}/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> updateSection(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("sectionId") UUID sectionId,
            @RequestBody UpdateClassTestSectionRequest request) {
        var data = updateClassTestSectionUseCase.execute(
            UpdateClassTestSectionCommandMapper.fromRequest(assessmentId, sectionId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật section thành công", data));
    }

    @DeleteMapping("/{assessmentId}/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> deleteSection(
            @PathVariable("assessmentId") UUID assessmentId,
            @PathVariable("sectionId") UUID sectionId) {
        var data = deleteClassTestSectionUseCase.execute(new DeleteClassTestSectionCommand(assessmentId, sectionId));
        return ResponseEntity.ok(ApiResponse.success("Xóa section thành công", data));
    }

    @PatchMapping("/{assessmentId}/blueprint")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AssessmentDto>> changeBlueprint(
            @PathVariable("assessmentId") UUID assessmentId,
            @RequestBody ChangeClassTestBlueprintRequest request) {
        var data = changeClassTestBlueprintUseCase.execute(ChangeClassTestBlueprintRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật blueprint cho bài kiểm tra trên lớp thành công", data));
    }

    /**
     * Giáo viên tạo bài tự nhận chấm. Đặt ở đây thay vì nới quyền của
     * {@code /api/v1/grading-assignments}: chỗ đó nới là mở cho mọi giáo viên trên mọi
     * kỳ thi, còn ở đây phạm vi đóng đúng bằng bài mà người gọi làm CHAIR.
     */
    @PostMapping("/{assessmentId}/grading/claim")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<UUID>>> claimGrading(
            @PathVariable("assessmentId") UUID assessmentId,
            @Valid @RequestBody ClaimClassTestGradingRequest request) {
        var data = claimClassTestGradingUseCase.execute(
            ClaimClassTestGradingRequest.toCommand(assessmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Nhận chấm bài thành công", data));
    }
}
