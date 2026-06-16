package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.question.CreateQuestionEvaluationGuideUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionEvaluationGuideUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionEvaluationGuideUseCase;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionEvaluationGuideRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionEvaluationGuideCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions/{questionId}/evaluation-guide")
public class QuestionEvaluationGuideController {

    private final CreateQuestionEvaluationGuideUseCase createQuestionEvaluationGuideUseCase;
    private final DeleteQuestionEvaluationGuideUseCase deleteQuestionEvaluationGuideUseCase;
    private final UpdateQuestionEvaluationGuideUseCase updateQuestionEvaluationGuideUseCase;

    public QuestionEvaluationGuideController(
            CreateQuestionEvaluationGuideUseCase createQuestionEvaluationGuideUseCase,
            DeleteQuestionEvaluationGuideUseCase deleteQuestionEvaluationGuideUseCase,
            UpdateQuestionEvaluationGuideUseCase updateQuestionEvaluationGuideUseCase) {
        this.createQuestionEvaluationGuideUseCase = createQuestionEvaluationGuideUseCase;
        this.deleteQuestionEvaluationGuideUseCase = deleteQuestionEvaluationGuideUseCase;
        this.updateQuestionEvaluationGuideUseCase = updateQuestionEvaluationGuideUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> create(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionEvaluationGuideRequest request) {
        var command = UpdateQuestionEvaluationGuideCommandMapper.fromRequest(questionId, request);
        var data = createQuestionEvaluationGuideUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao huong dan danh gia thanh cong", data));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> update(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionEvaluationGuideRequest request) {
        var command = UpdateQuestionEvaluationGuideCommandMapper.fromRequest(questionId, request);
        var data = updateQuestionEvaluationGuideUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat huong dan danh gia thanh cong", data));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> delete(@PathVariable UUID questionId) {
        var data = deleteQuestionEvaluationGuideUseCase.execute(questionId);
        return ResponseEntity.ok(ApiResponse.success("Xoa huong dan danh gia thanh cong", data));
    }
}
