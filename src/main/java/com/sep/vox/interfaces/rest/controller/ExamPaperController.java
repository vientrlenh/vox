package com.sep.vox.interfaces.rest.controller;

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

import com.sep.vox.application.port.input.command.CreateExamPaperCommand;
import com.sep.vox.application.port.input.command.DeleteExamPaperCommand;
import com.sep.vox.application.port.input.usecase.exampaper.CreateExamPaperUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.DeleteExamPaperUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.UpdateExamPaperItemUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.UpdateExamPaperStatusUseCase;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamPaperItemRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamPaperStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.UpdateExamPaperItemCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamPaperStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ExamPaperController {

    private final CreateExamPaperUseCase createExamPaperUseCase;
    private final UpdateExamPaperItemUseCase updateExamPaperItemUseCase;
    private final UpdateExamPaperStatusUseCase updateExamPaperStatusUseCase;
    private final DeleteExamPaperUseCase deleteExamPaperUseCase;

    public ExamPaperController(
            CreateExamPaperUseCase createExamPaperUseCase,
            UpdateExamPaperItemUseCase updateExamPaperItemUseCase,
            UpdateExamPaperStatusUseCase updateExamPaperStatusUseCase,
            DeleteExamPaperUseCase deleteExamPaperUseCase) {
        this.createExamPaperUseCase = createExamPaperUseCase;
        this.updateExamPaperItemUseCase = updateExamPaperItemUseCase;
        this.updateExamPaperStatusUseCase = updateExamPaperStatusUseCase;
        this.deleteExamPaperUseCase = deleteExamPaperUseCase;
    }

    @PostMapping("/exams/{examId}/papers")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamPaperDto>> create(@PathVariable UUID examId) {
        var data = createExamPaperUseCase.execute(new CreateExamPaperCommand(examId));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo đề thi thành công", data));
    }

    @PutMapping("/exam-papers/{id}/items/{itemId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamPaperItemDto>> updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateExamPaperItemRequest request) {
        var data = updateExamPaperItemUseCase.execute(UpdateExamPaperItemCommandMapper.fromRequest(id, itemId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi trong đề thi thành công", data));
    }

    @PatchMapping("/exam-papers/{id}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ExamPaperDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamPaperStatusRequest request) {
        var data = updateExamPaperStatusUseCase.execute(UpdateExamPaperStatusCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đề thi thành công", data));
    }

    @DeleteMapping("/exam-papers/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        deleteExamPaperUseCase.execute(new DeleteExamPaperCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa đề thi thành công"));
    }
}
