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

import com.sep.vox.application.port.input.usecase.question.CreateQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionUseCase;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final CreateQuestionUseCase createQuestionUseCase;
    private final UpdateQuestionUseCase updateQuestionUseCase;

    public QuestionController(
            CreateQuestionUseCase createQuestionUseCase,
            UpdateQuestionUseCase updateQuestionUseCase) {
        this.createQuestionUseCase = createQuestionUseCase;
        this.updateQuestionUseCase = updateQuestionUseCase;
    }

    @PostMapping
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> create(@Valid @RequestBody CreateQuestionRequest request) {
        var command = CreateQuestionCommandMapper.fromRequest(request);
        var data = createQuestionUseCase.execute(command);
        var response = ApiResponse.success("Tạo câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        var command = UpdateQuestionCommandMapper.fromRequest(id, request);
        var data = updateQuestionUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }
}
