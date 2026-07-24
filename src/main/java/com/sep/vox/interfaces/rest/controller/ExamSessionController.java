package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.CompleteExamSessionGradingCommand;
import com.sep.vox.application.port.input.usecase.exam.CompleteExamSessionGradingUseCase;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/internal/exam-sessions")
public class ExamSessionController {

    private final CompleteExamSessionGradingUseCase completeExamSessionGradingUseCase;

    public ExamSessionController(CompleteExamSessionGradingUseCase completeExamSessionGradingUseCase) {
        this.completeExamSessionGradingUseCase = completeExamSessionGradingUseCase;
    }

    @PostMapping("/{sessionId}/complete-grading")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeGrading(@PathVariable UUID sessionId) {
        completeExamSessionGradingUseCase.execute(new CompleteExamSessionGradingCommand(sessionId));
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận hoàn tất chấm bài thành công"));
    }
}
