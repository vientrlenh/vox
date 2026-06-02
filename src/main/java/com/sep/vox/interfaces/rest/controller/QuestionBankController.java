package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.questionbank.CreateQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankUseCase;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionBankCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionBankCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/question-banks")
public class QuestionBankController {

    private final CreateQuestionBankUseCase createQuestionBankUseCase;
    private final UpdateQuestionBankUseCase updateQuestionBankUseCase;

    public QuestionBankController(
            CreateQuestionBankUseCase createQuestionBankUseCase,
            UpdateQuestionBankUseCase updateQuestionBankUseCase) {
        this.createQuestionBankUseCase = createQuestionBankUseCase;
        this.updateQuestionBankUseCase = updateQuestionBankUseCase;
    }

    @PostMapping
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankDto>> create(@Valid @RequestBody CreateQuestionBankRequest request) {
        var command = CreateQuestionBankCommandMapper.fromRequest(request);
        var data = createQuestionBankUseCase.execute(command);
        var response = ApiResponse.success("Tạo ngân hàng câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionBankRequest request) {
        var command = UpdateQuestionBankCommandMapper.fromRequest(id, request);
        var data = updateQuestionBankUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật ngân hàng câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }
}
