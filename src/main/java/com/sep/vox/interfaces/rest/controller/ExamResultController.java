package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.ReviewFlaggedExamResultCommand;
import com.sep.vox.application.port.input.usecase.examsession.ReviewFlaggedExamResultUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.interfaces.rest.dto.request.ReviewFlaggedExamResultRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/exam-results")
public class ExamResultController {

    private final ReviewFlaggedExamResultUseCase reviewFlaggedExamResultUseCase;

    public ExamResultController(ReviewFlaggedExamResultUseCase reviewFlaggedExamResultUseCase) {
        this.reviewFlaggedExamResultUseCase = reviewFlaggedExamResultUseCase;
    }

    @PostMapping("/{candidateResultId}/review")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UUID>> review(
            @PathVariable UUID candidateResultId,
            @Valid @RequestBody ReviewFlaggedExamResultRequest request) {
        var data = reviewFlaggedExamResultUseCase.execute(new ReviewFlaggedExamResultCommand(
            candidateResultId,
            ExamCandidateResultStatus.valueOf(request.decision().trim().toUpperCase())
        ));
        return ResponseEntity.ok(ApiResponse.success("Duyệt kết quả bài thi thành công", data));
    }
}
