package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.ReviewQuestionCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionContentCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionEvaluationGuideCommand;
import com.sep.vox.application.port.input.usecase.question.CreateSystemQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.ReviewQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionContentUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionEvaluationGuideUseCase;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReviewQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetsRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionContentRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionEvaluationGuideRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final CreateSystemQuestionBankQuestionUseCase createQuestionBankQuestionUseCase;
    private final UpdateQuestionContentUseCase updateQuestionContentUseCase;
    private final UpdateQuestionAssetsUseCase updateQuestionAssetsUseCase;
    private final UpdateQuestionEvaluationGuideUseCase updateQuestionEvaluationGuideUseCase;
    private final ReviewQuestionUseCase reviewQuestionUseCase;

    public QuestionController(
            CreateSystemQuestionBankQuestionUseCase createQuestionBankQuestionUseCase,
            UpdateQuestionContentUseCase updateQuestionContentUseCase,
            UpdateQuestionAssetsUseCase updateQuestionAssetsUseCase,
            UpdateQuestionEvaluationGuideUseCase updateQuestionEvaluationGuideUseCase,
            ReviewQuestionUseCase reviewQuestionUseCase) {
        this.createQuestionBankQuestionUseCase = createQuestionBankQuestionUseCase;
        this.updateQuestionContentUseCase = updateQuestionContentUseCase;
        this.updateQuestionAssetsUseCase = updateQuestionAssetsUseCase;
        this.updateQuestionEvaluationGuideUseCase = updateQuestionEvaluationGuideUseCase;
        this.reviewQuestionUseCase = reviewQuestionUseCase;
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionResponse>> create(
            @Valid @RequestBody CreateSystemQuestionBankQuestionRequest request) {
        var command = CreateQuestionCommandMapper.fromQuestionBankRequest(request);
        var data = createQuestionBankQuestionUseCase.execute(command);
        var response = ApiResponse.success("Táº¡o cÃ¢u há»i thÃ nh cÃ´ng", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        return ResponseEntity.ok(ApiResponse.success("Cáº­p nháº­t ná»™i dung cÃ¢u há»i thÃ nh cÃ´ng", data));
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
        return ResponseEntity.ok(ApiResponse.success("Cáº­p nháº­t tÃ i sáº£n cÃ¢u há»i thÃ nh cÃ´ng", data));
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
        return ResponseEntity.ok(ApiResponse.success("cập nhật thành công", data));
    }

    @PostMapping("/{questionId}/review-actions")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> reviewAction(
            @PathVariable UUID questionId,
            @Valid @RequestBody ReviewQuestionRequest request) {
        var targetStatus = QuestionStatus.valueOf(request.targetStatus());
        var command = new ReviewQuestionCommand(questionId, targetStatus, request.note(), request.reason());
        var data = reviewQuestionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thá»±c hiá»‡n hÃ nh Ä‘á»™ng duyá»‡t thÃ nh cÃ´ng", data));
    }
}
