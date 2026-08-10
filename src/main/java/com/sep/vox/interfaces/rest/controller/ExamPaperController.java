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

import com.sep.vox.application.port.input.command.DeleteExamPaperCommand;
import com.sep.vox.application.port.input.usecase.exampaper.CreateExamPaperUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.DeleteExamPaperUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.UpdateExamPaperItemUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.UpdateExamPaperSectionUseCase;
import com.sep.vox.application.port.input.usecase.exampaper.UpdateExamPaperStatusUseCase;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.domain.dto.ExamPaperSectionDto;
import com.sep.vox.interfaces.rest.dto.request.CreateExamPaperRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamPaperItemRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamPaperSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamPaperStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateExamPaperCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamPaperItemCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamPaperSectionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateExamPaperStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ExamPaperController {

    private final CreateExamPaperUseCase createExamPaperUseCase;
    private final UpdateExamPaperItemUseCase updateExamPaperItemUseCase;
    private final UpdateExamPaperSectionUseCase updateExamPaperSectionUseCase;
    private final UpdateExamPaperStatusUseCase updateExamPaperStatusUseCase;
    private final DeleteExamPaperUseCase deleteExamPaperUseCase;

    public ExamPaperController(
            CreateExamPaperUseCase createExamPaperUseCase,
            UpdateExamPaperItemUseCase updateExamPaperItemUseCase,
            UpdateExamPaperSectionUseCase updateExamPaperSectionUseCase,
            UpdateExamPaperStatusUseCase updateExamPaperStatusUseCase,
            DeleteExamPaperUseCase deleteExamPaperUseCase) {
        this.createExamPaperUseCase = createExamPaperUseCase;
        this.updateExamPaperItemUseCase = updateExamPaperItemUseCase;
        this.updateExamPaperSectionUseCase = updateExamPaperSectionUseCase;
        this.updateExamPaperStatusUseCase = updateExamPaperStatusUseCase;
        this.deleteExamPaperUseCase = deleteExamPaperUseCase;
    }

    @PostMapping("/exams/{examId}/papers")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ExamPaperDto>> create(
            @PathVariable("examId") UUID examId,
            @Valid @RequestBody(required = false) CreateExamPaperRequest request) {
        var data = createExamPaperUseCase.execute(CreateExamPaperCommandMapper.fromRequest(examId, request));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Tạo đề thi thành công", data));
    }

    @PutMapping("/exam-papers/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ExamPaperItemDto>> updateItem(
            @PathVariable("id") UUID id,
            @PathVariable("itemId") UUID itemId,
            @Valid @RequestBody UpdateExamPaperItemRequest request) {
        var data = updateExamPaperItemUseCase.execute(UpdateExamPaperItemCommandMapper.fromRequest(id, itemId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi trong đề thi thành công", data));
    }

    @PatchMapping("/exam-papers/{id}/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ExamPaperSectionDto>> updateSection(
            @PathVariable("id") UUID id,
            @PathVariable("sectionId") UUID sectionId,
            @RequestBody UpdateExamPaperSectionRequest request) {
        var data = updateExamPaperSectionUseCase.execute(UpdateExamPaperSectionCommandMapper.fromRequest(id, sectionId, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phần đề thi thành công", data));
    }

    @PatchMapping("/exam-papers/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<ExamPaperDto>> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateExamPaperStatusRequest request) {
        var data = updateExamPaperStatusUseCase.execute(UpdateExamPaperStatusCommandMapper.fromRequest(id, request));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đề thi thành công", data));
    }

    @DeleteMapping("/exam-papers/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") UUID id) {
        deleteExamPaperUseCase.execute(new DeleteExamPaperCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa đề thi thành công"));
    }
}
