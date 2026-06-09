package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.common.permission.ReviewAction;
import com.sep.vox.application.port.input.command.CloneQuestionCommand;
import com.sep.vox.application.port.input.command.ReviewQuestionCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionContentCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.usecase.question.CloneQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.ReviewQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionContentUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionEvaluationGuideUseCase;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.interfaces.rest.dto.request.CloneQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReviewQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetsRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionContentRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionEvaluationGuideRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionManagementController {

    private final UpdateQuestionContentUseCase updateQuestionContentUseCase;
    private final UpdateQuestionAssetsUseCase updateQuestionAssetsUseCase;
    private final UpdateQuestionEvaluationGuideUseCase updateQuestionEvaluationGuideUseCase;
    private final ReviewQuestionUseCase reviewQuestionUseCase;
    private final CloneQuestionUseCase cloneQuestionUseCase;

    public QuestionManagementController(
            UpdateQuestionContentUseCase updateQuestionContentUseCase,
            UpdateQuestionAssetsUseCase updateQuestionAssetsUseCase,
            UpdateQuestionEvaluationGuideUseCase updateQuestionEvaluationGuideUseCase,
            ReviewQuestionUseCase reviewQuestionUseCase,
            CloneQuestionUseCase cloneQuestionUseCase) {
        this.updateQuestionContentUseCase = updateQuestionContentUseCase;
        this.updateQuestionAssetsUseCase = updateQuestionAssetsUseCase;
        this.updateQuestionEvaluationGuideUseCase = updateQuestionEvaluationGuideUseCase;
        this.reviewQuestionUseCase = reviewQuestionUseCase;
        this.cloneQuestionUseCase = cloneQuestionUseCase;
    }

    @PutMapping("/{questionId}/content")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> updateContent(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionContentRequest request) {
        var command = new UpdateQuestionContentCommand(
                questionId,
                request.instructionText(),
                request.questionText(),
                request.promptText(),
                request.preparationText(),
                request.type(),
                request.preparationTimeSeconds(),
                request.minResponseSeconds(),
                request.maxResponseSeconds());
        var data = updateQuestionContentUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật nội dung câu hỏi thành công", data));
    }

    @PutMapping("/{questionId}/assets")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> updateAssets(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionAssetsRequest request) {
        var assets = request.assets().stream()
                .map(a -> new UpdateQuestionAssetsCommand.AssetItem(
                        a.title(), a.durationSeconds(), a.altText(), a.type(),
                        a.url(), a.transcript(), a.description(), a.order()))
                .toList();
        var command = new UpdateQuestionAssetsCommand(questionId, assets);
        var data = updateQuestionAssetsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tài sản câu hỏi thành công", data));
    }

    @PutMapping("/{questionId}/evaluation-guide")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> updateEvaluationGuide(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionEvaluationGuideRequest request) {
        var command = new UpdateQuestionEvaluationGuideCommand(
                questionId,
                request.expectedContent(),
                request.keyPoints(),
                request.acceptableResponses(),
                request.offTopicExamples(),
                request.scoringHints(),
                request.commonMistakes());
        var data = updateQuestionEvaluationGuideUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hướng dẫn đánh giá thành công", data));
    }

    @PostMapping("/{questionId}/review-actions")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> reviewAction(
            @PathVariable UUID questionId,
            @Valid @RequestBody ReviewQuestionRequest request) {
        var action = ReviewAction.valueOf(request.action());
        var command = new ReviewQuestionCommand(questionId, action, request.note(), request.reason());
        var data = reviewQuestionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thực hiện hành động duyệt thành công", data));
    }

    @PostMapping("/{questionId}/clone")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionResponse>> clone(
            @PathVariable UUID questionId,
            @RequestBody CloneQuestionRequest request) {
        var command = new CloneQuestionCommand(questionId);
        var data = cloneQuestionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Sao chép câu hỏi thành công", data));
    }
}
