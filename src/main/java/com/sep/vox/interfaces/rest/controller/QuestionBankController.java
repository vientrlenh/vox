package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.ReviewQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSchoolQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSystemQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ReviewQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankUseCase;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.application.response.input.questionbank.UpdateQuestionBankResponse;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.model.question.QuestionBankStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.ReviewQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionBankCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteQuestionBankCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionBankCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/question-banks")
public class QuestionBankController {

    private final CreateSystemQuestionBankUseCase createSystemQuestionBankUseCase;
    private final CreateSchoolQuestionBankUseCase createSchoolQuestionBankUseCase;
    private final UpdateQuestionBankUseCase updateQuestionBankUseCase;
    private final DeleteQuestionBankUseCase deleteQuestionBankUseCase;
    private final ReviewQuestionBankUseCase reviewQuestionBankUseCase;

    public QuestionBankController(
            CreateSystemQuestionBankUseCase createSystemQuestionBankUseCase,
            CreateSchoolQuestionBankUseCase createSchoolQuestionBankUseCase,
            UpdateQuestionBankUseCase updateQuestionBankUseCase,
            DeleteQuestionBankUseCase deleteQuestionBankUseCase,
            ReviewQuestionBankUseCase reviewQuestionBankUseCase) {
        this.createSystemQuestionBankUseCase = createSystemQuestionBankUseCase;
        this.createSchoolQuestionBankUseCase = createSchoolQuestionBankUseCase;
        this.updateQuestionBankUseCase = updateQuestionBankUseCase;
        this.deleteQuestionBankUseCase = deleteQuestionBankUseCase;
        this.reviewQuestionBankUseCase = reviewQuestionBankUseCase;
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionBankResponse>> createSystem(@Valid @RequestBody CreateSystemQuestionBankRequest request) {
        var command = CreateQuestionBankCommandMapper.fromSystemRequest(request);
        var data = createSystemQuestionBankUseCase.execute(command);
        var response = ApiResponse.success("Ngan hang cau hoi he thong duoc tao thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionBankResponse>> createSchool(@Valid @RequestBody CreateSchoolQuestionBankRequest request) {
        var command = CreateQuestionBankCommandMapper.fromSchoolRequest(request);
        var data = createSchoolQuestionBankUseCase.execute(command);
        var response = ApiResponse.success("Tao ngan hang cau hoi thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{bankId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankDto>> update(
            @PathVariable UUID bankId,
            @Valid @RequestBody UpdateQuestionBankRequest request) {
        var command = UpdateQuestionBankCommandMapper.fromRequest(bankId, request);
        var data = updateQuestionBankUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat ngan hang cau hoi thanh cong", data));
    }

    @DeleteMapping("/{bankId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionBankResponse>> delete(@PathVariable UUID bankId) {
        var data = deleteQuestionBankUseCase.execute(DeleteQuestionBankCommandMapper.fromId(bankId));
        return ResponseEntity.ok(ApiResponse.success("Xoa question bank thanh cong", data));
    }

    @PatchMapping("/{bankId}/review-actions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionBankResponse>> reviewAction(
            @PathVariable UUID bankId,
            @Valid @RequestBody ReviewQuestionBankRequest request) {
        var targetStatus = QuestionBankStatus.valueOf(request.targetStatus());
        var command = new ReviewQuestionBankCommand(bankId, targetStatus);
        var data = reviewQuestionBankUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thuc hien hanh dong thanh cong", data));
    }
}
