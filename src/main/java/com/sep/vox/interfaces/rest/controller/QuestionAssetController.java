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

import com.sep.vox.application.port.input.usecase.question.CreateQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetsUseCase;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetsRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionAssetsCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions/{questionId}/assets")
public class QuestionAssetController {

    private final CreateQuestionAssetsUseCase createQuestionAssetsUseCase;
    private final DeleteQuestionAssetsUseCase deleteQuestionAssetsUseCase;
    private final UpdateQuestionAssetsUseCase updateQuestionAssetsUseCase;

    public QuestionAssetController(
            CreateQuestionAssetsUseCase createQuestionAssetsUseCase,
            DeleteQuestionAssetsUseCase deleteQuestionAssetsUseCase,
            UpdateQuestionAssetsUseCase updateQuestionAssetsUseCase) {
        this.createQuestionAssetsUseCase = createQuestionAssetsUseCase;
        this.deleteQuestionAssetsUseCase = deleteQuestionAssetsUseCase;
        this.updateQuestionAssetsUseCase = updateQuestionAssetsUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> create(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionAssetsRequest request) {
        var command = UpdateQuestionAssetsCommandMapper.fromRequest(questionId, request);
        var data = createQuestionAssetsUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao tai san cau hoi thanh cong", data));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> update(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionAssetsRequest request) {
        var command = UpdateQuestionAssetsCommandMapper.fromRequest(questionId, request);
        var data = updateQuestionAssetsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat tai san cau hoi thanh cong", data));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> delete(@PathVariable UUID questionId) {
        var data = deleteQuestionAssetsUseCase.execute(questionId);
        return ResponseEntity.ok(ApiResponse.success("Xoa tai san cau hoi thanh cong", data));
    }
}
