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

import com.sep.vox.application.port.input.command.ReviewQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.questiontopic.CreateQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.DeleteQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ReviewQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicUseCase;
import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;
import com.sep.vox.application.response.input.questiontopic.UpdateQuestionTopicResponse;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.ReviewQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionTopicCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteQuestionTopicCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionTopicCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/question-topics")
public class QuestionTopicController {

    private final CreateQuestionTopicUseCase createQuestionTopicUseCase;
    private final UpdateQuestionTopicUseCase updateQuestionTopicUseCase;
    private final DeleteQuestionTopicUseCase deleteQuestionTopicUseCase;
    private final ReviewQuestionTopicUseCase reviewQuestionTopicUseCase;

    public QuestionTopicController(
            CreateQuestionTopicUseCase createQuestionTopicUseCase,
            UpdateQuestionTopicUseCase updateQuestionTopicUseCase,
            DeleteQuestionTopicUseCase deleteQuestionTopicUseCase,
            ReviewQuestionTopicUseCase reviewQuestionTopicUseCase) {
        this.createQuestionTopicUseCase = createQuestionTopicUseCase;
        this.updateQuestionTopicUseCase = updateQuestionTopicUseCase;
        this.deleteQuestionTopicUseCase = deleteQuestionTopicUseCase;
        this.reviewQuestionTopicUseCase = reviewQuestionTopicUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionTopicResponse>> create(@Valid @RequestBody CreateQuestionTopicRequest request) {
        var command = CreateQuestionTopicCommandMapper.fromRequest(request);
        var data = createQuestionTopicUseCase.execute(command);
        var response = ApiResponse.success("Tao chu de cau hoi thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTopicDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionTopicRequest request) {
        var command = UpdateQuestionTopicCommandMapper.fromRequest(id, request);
        var data = updateQuestionTopicUseCase.execute(command);
        var response = ApiResponse.success("Cap nhat chu de cau hoi thanh cong", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{topicId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionTopicResponse>> delete(@PathVariable UUID topicId) {
        var data = deleteQuestionTopicUseCase.execute(DeleteQuestionTopicCommandMapper.fromId(topicId));
        return ResponseEntity.ok(ApiResponse.success("Xoa question topic thanh cong", data));
    }

    @PatchMapping("/{topicId}/review-actions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionTopicResponse>> reviewAction(
            @PathVariable UUID topicId,
            @Valid @RequestBody ReviewQuestionTopicRequest request) {
        var targetStatus = QuestionTopicStatus.valueOf(request.targetStatus());
        var command = new ReviewQuestionTopicCommand(topicId, targetStatus);
        var data = reviewQuestionTopicUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thuc hien hanh dong thanh cong", data));
    }
}
