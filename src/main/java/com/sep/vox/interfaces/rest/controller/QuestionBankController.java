package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.questionbank.CreateSchoolQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSystemQuestionBankUseCase;
import com.sep.vox.application.response.input.question.CreateQuestionBankResponse;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionBankCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/question-banks")
public class QuestionBankController {

    private final CreateSystemQuestionBankUseCase createSystemQuestionBankUseCase;
    private final CreateSchoolQuestionBankUseCase createSchoolQuestionBankUseCase;

    public QuestionBankController(
            CreateSystemQuestionBankUseCase createSystemQuestionBankUseCase,
            CreateSchoolQuestionBankUseCase createSchoolQuestionBankUseCase) {
        this.createSystemQuestionBankUseCase = createSystemQuestionBankUseCase;
        this.createSchoolQuestionBankUseCase = createSchoolQuestionBankUseCase;
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionBankResponse>> createSystem(@Valid @RequestBody CreateSystemQuestionBankRequest request) {
        var command = CreateQuestionBankCommandMapper.fromSystemRequest(request);
        var data = createSystemQuestionBankUseCase.execute(command);
        var response = ApiResponse.success("Ngân hàng câu hỏi hệ thống được tạo thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionBankResponse>> createSchool(@Valid @RequestBody CreateSchoolQuestionBankRequest request) {
        var command = CreateQuestionBankCommandMapper.fromSchoolRequest(request);
        var data = createSchoolQuestionBankUseCase.execute(command);
        var response = ApiResponse.success("Tạo ngân hàng câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
