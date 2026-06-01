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

import com.sep.vox.application.port.input.usecase.questiontopic.CreateQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicUseCase;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionTopicCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionTopicCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/question-topics")
public class QuestionTopicController {

    private final CreateQuestionTopicUseCase createQuestionTopicUseCase;
    private final UpdateQuestionTopicUseCase updateQuestionTopicUseCase;

    public QuestionTopicController(
            CreateQuestionTopicUseCase createQuestionTopicUseCase,
            UpdateQuestionTopicUseCase updateQuestionTopicUseCase) {
        this.createQuestionTopicUseCase = createQuestionTopicUseCase;
        this.updateQuestionTopicUseCase = updateQuestionTopicUseCase;
    }

    @PostMapping
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTopicDto>> create(@Valid @RequestBody CreateQuestionTopicRequest request) {
        var command = CreateQuestionTopicCommandMapper.fromRequest(request);
        var data = createQuestionTopicUseCase.execute(command);
        var response = ApiResponse.success("Tạo chủ đề câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTopicDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionTopicRequest request) {
        var command = UpdateQuestionTopicCommandMapper.fromRequest(id, request);
        var data = updateQuestionTopicUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật chủ đề câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }
}
