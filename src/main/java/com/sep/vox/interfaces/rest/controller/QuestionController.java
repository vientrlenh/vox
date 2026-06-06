package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.question.CreateSystemQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionUseCase;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankQuestionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionCommandMapper;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final CreateSystemQuestionBankQuestionUseCase createQuestionBankQuestionUseCase;

    public QuestionController(
            CreateSystemQuestionBankQuestionUseCase createQuestionBankQuestionUseCase,
            UpdateQuestionUseCase updateQuestionUseCase) {
        this.createQuestionBankQuestionUseCase = createQuestionBankQuestionUseCase;
    }


    @PostMapping("/system")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionResponse>> create(@Valid @RequestBody CreateSystemQuestionBankQuestionRequest request) {
        var command = CreateQuestionCommandMapper.fromQuestionBankRequest(request);
        var data = createQuestionBankQuestionUseCase.execute(command);
        var response = ApiResponse.success("Tạo câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
