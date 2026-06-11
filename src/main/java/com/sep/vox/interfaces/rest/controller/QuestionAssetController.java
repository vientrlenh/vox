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

import com.sep.vox.application.port.input.command.UpdateQuestionAssetsCommand;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetsUseCase;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetsRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions/{questionId}/assets")
public class QuestionAssetController {

    private final CreateQuestionAssetsUseCase createQuestionAssetsUseCase;
    private final UpdateQuestionAssetsUseCase updateQuestionAssetsUseCase;

    public QuestionAssetController(
            CreateQuestionAssetsUseCase createQuestionAssetsUseCase,
            UpdateQuestionAssetsUseCase updateQuestionAssetsUseCase) {
        this.createQuestionAssetsUseCase = createQuestionAssetsUseCase;
        this.updateQuestionAssetsUseCase = updateQuestionAssetsUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> create(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionAssetsRequest request) {
        var command = toCommand(questionId, request);
        var data = createQuestionAssetsUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao tai san cau hoi thanh cong", data));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> update(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionAssetsRequest request) {
        var command = toCommand(questionId, request);
        var data = updateQuestionAssetsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat tai san cau hoi thanh cong", data));
    }

    private UpdateQuestionAssetsCommand toCommand(UUID questionId, UpdateQuestionAssetsRequest request) {
        var assets = request.assets().stream()
                .map(a -> new UpdateQuestionAssetsCommand.AssetItem(
                        a.title(), a.durationSeconds(), a.altText(), a.type(),
                        a.url(), a.transcript(), a.description(), a.order()))
                .toList();
        return new UpdateQuestionAssetsCommand(questionId, assets);
    }
}
