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
import com.sep.vox.application.port.input.usecase.examgrading.ClaimClassTestGradingUseCase;
import com.sep.vox.application.port.input.command.DeleteExamCommand;
import com.sep.vox.application.port.input.usecase.exam.ChangeClassTestBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.exam.CreateClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.exam.CreateClassTestUseCase;
import com.sep.vox.application.port.input.usecase.exam.DeleteClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.exam.DeleteExamUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateClassTestQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamStatusUseCase;
import com.sep.vox.application.port.input.usecase.exam.UpdateExamUseCase;
import com.sep.vox.application.response.input.exam.CreateClassTestResponse;
import com.sep.vox.application.response.input.exam.DeleteExamResponse;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.interfaces.rest.dto.request.ChangeClassTestBlueprintRequest;
import com.sep.vox.interfaces.rest.dto.request.ClaimClassTestGradingRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateClassTestRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateClassTestSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateClassTestQuestionsRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateClassTestSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ChangeClassTestBlueprintCommandMapper;
import com.sep.vox.interfaces.rest.mapper.ClaimClassTestGradingCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateClassTestCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateClassTestSectionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateClassTestQuestionsCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateClassTestSectionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/class-tests")
public class ClassTestController {

    private final CreateClassTestUseCase createClassTestUseCase;
    private final UpdateExamUseCase updateExamUseCase;
    private final UpdateExamStatusUseCase updateExamStatusUseCase;
    private final DeleteExamUseCase deleteExamUseCase;
    private final UpdateClassTestQuestionsUseCase updateClassTestQuestionsUseCase;
    private final CreateClassTestSectionUseCase createClassTestSectionUseCase;
    private final UpdateClassTestSectionUseCase updateClassTestSectionUseCase;
    private final DeleteClassTestSectionUseCase deleteClassTestSectionUseCase;
    private final ChangeClassTestBlueprintUseCase changeClassTestBlueprintUseCase;
    private final ClaimClassTestGradingUseCase claimClassTestGradingUseCase;

    public ClassTestController(
            CreateClassTestUseCase createClassTestUseCase,
            UpdateExamUseCase updateExamUseCase,
            UpdateExamStatusUseCase updateExamStatusUseCase,
            DeleteExamUseCase deleteExamUseCase,
            UpdateClassTestQuestionsUseCase updateClassTestQuestionsUseCase,
            CreateClassTestSectionUseCase createClassTestSectionUseCase,
            UpdateClassTestSectionUseCase updateClassTestSectionUseCase,
            DeleteClassTestSectionUseCase deleteClassTestSectionUseCase,
            ChangeClassTestBlueprintUseCase changeClassTestBlueprintUseCase,
            ClaimClassTestGradingUseCase claimClassTestGradingUseCase) {
        this.createClassTestUseCase = createClassTestUseCase;
        this.updateExamUseCase = updateExamUseCase;
        this.updateExamStatusUseCase = updateExamStatusUseCase;
        this.deleteExamUseCase = deleteExamUseCase;
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
        var data = createClassTestUseCase.execute(CreateClassTestCommandMapper.fromRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo bài kiểm tra trên lớp thành công", data));
    }

    @PutMapping("/{examId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> update(
            @PathVariable UUID examId,
            @Valid @RequestBody UpdateExamRequest request) {
        var data = updateExamUseCase.execute(UpdateExamCommandMapper.fromRequest(examId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài kiểm tra trên lớp thành công", data));
    }

    @PutMapping("/{examId}/questions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> updateQuestions(
            @PathVariable UUID examId,
            @Valid @RequestBody UpdateClassTestQuestionsRequest request) {
        var data = updateClassTestQuestionsUseCase.execute(UpdateClassTestQuestionsCommandMapper.fromRequest(examId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh sách câu hỏi thành công", data));
    }

    @PatchMapping("/{examId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> updateStatus(
            @PathVariable UUID examId,
            @Valid @RequestBody UpdateExamStatusRequest request) {
        var data = updateExamStatusUseCase.execute(UpdateExamStatusCommandMapper.fromRequest(examId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái bài kiểm tra trên lớp thành công", data));
    }

    @DeleteMapping("/{examId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<DeleteExamResponse>> delete(@PathVariable UUID examId) {
        var data = deleteExamUseCase.execute(new DeleteExamCommand(examId));
        return ResponseEntity.ok(ApiResponse.success("Xóa bài kiểm tra trên lớp thành công", data));
    }

    @PostMapping("/{examId}/sections")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> createSection(
            @PathVariable UUID examId,
            @Valid @RequestBody CreateClassTestSectionRequest request) {
        var data = createClassTestSectionUseCase.execute(CreateClassTestSectionCommandMapper.fromRequest(examId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo section thành công", data));
    }

    @PutMapping("/{examId}/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> updateSection(
            @PathVariable UUID examId,
            @PathVariable UUID sectionId,
            @RequestBody UpdateClassTestSectionRequest request) {
        var data = updateClassTestSectionUseCase.execute(
            UpdateClassTestSectionCommandMapper.fromRequest(examId, sectionId, request)
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật section thành công", data));
    }

    @DeleteMapping("/{examId}/sections/{sectionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> deleteSection(
            @PathVariable UUID examId,
            @PathVariable UUID sectionId) {
        var data = deleteClassTestSectionUseCase.execute(new DeleteClassTestSectionCommand(examId, sectionId));
        return ResponseEntity.ok(ApiResponse.success("Xóa section thành công", data));
    }

    @PatchMapping("/{examId}/blueprint")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamDto>> changeBlueprint(
            @PathVariable UUID examId,
            @RequestBody ChangeClassTestBlueprintRequest request) {
        var data = changeClassTestBlueprintUseCase.execute(ChangeClassTestBlueprintCommandMapper.fromRequest(examId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật blueprint cho bài kiểm tra trên lớp thành công", data));
    }

    /**
     * Giáo viên tạo bài tự nhận chấm. Đặt ở đây thay vì nới quyền của
     * {@code /api/v1/grading-assignments}: chỗ đó nới là mở cho mọi giáo viên trên mọi
     * kỳ thi, còn ở đây phạm vi đóng đúng bằng bài mà người gọi làm CHAIR.
     */
    @PostMapping("/{examId}/grading/claim")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<UUID>>> claimGrading(
            @PathVariable UUID examId,
            @Valid @RequestBody ClaimClassTestGradingRequest request) {
        var data = claimClassTestGradingUseCase.execute(
            ClaimClassTestGradingCommandMapper.fromRequest(examId, request));
        return ResponseEntity.ok(ApiResponse.success("Nhận chấm bài thành công", data));
    }
}
