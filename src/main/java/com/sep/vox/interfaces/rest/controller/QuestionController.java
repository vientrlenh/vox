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

import com.sep.vox.application.port.input.command.ReviewQuestionCommand;
import com.sep.vox.application.port.input.usecase.question.CreateSchoolQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateSystemQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.ReviewQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionContentUseCase;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.application.response.input.question.DeleteQuestionResponse;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.ReviewQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionContentRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.DeleteQuestionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionContentCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final CreateSystemQuestionBankQuestionUseCase createSystemQuestionBankQuestionUseCase;
    private final CreateSchoolQuestionBankQuestionUseCase createSchoolQuestionBankQuestionUseCase;
    private final DeleteQuestionUseCase deleteQuestionUseCase;
    private final UpdateQuestionContentUseCase updateQuestionContentUseCase;
    private final ReviewQuestionUseCase reviewQuestionUseCase;

    public QuestionController(
            CreateSystemQuestionBankQuestionUseCase createSystemQuestionBankQuestionUseCase,
            CreateSchoolQuestionBankQuestionUseCase createSchoolQuestionBankQuestionUseCase,
            DeleteQuestionUseCase deleteQuestionUseCase,
            UpdateQuestionContentUseCase updateQuestionContentUseCase,
            ReviewQuestionUseCase reviewQuestionUseCase) {
        this.createSystemQuestionBankQuestionUseCase = createSystemQuestionBankQuestionUseCase;
        this.createSchoolQuestionBankQuestionUseCase = createSchoolQuestionBankQuestionUseCase;
        this.deleteQuestionUseCase = deleteQuestionUseCase;
        this.updateQuestionContentUseCase = updateQuestionContentUseCase;
        this.reviewQuestionUseCase = reviewQuestionUseCase;
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionResponse>> createSystem(
            @Valid @RequestBody CreateSystemQuestionBankQuestionRequest request) {
        var command = CreateQuestionCommandMapper.fromQuestionBankRequest(request);
        var data = createSystemQuestionBankQuestionUseCase.execute(command);
        var response = ApiResponse.success("Tao cau hoi thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/school")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<CreateQuestionResponse>> createSchool(
            @Valid @RequestBody CreateSystemQuestionBankQuestionRequest request) {
        var command = CreateQuestionCommandMapper.fromSchoolRequest(request);
        var data = createSchoolQuestionBankQuestionUseCase.execute(command);
        var response = ApiResponse.success("Tao cau hoi thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{questionId}/content")
    @PreAuthorize("hasAnyRole('TEACHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> updateContent(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionContentRequest request) {
        var command = UpdateQuestionContentCommandMapper.fromRequest(questionId, request);
        var data = updateQuestionContentUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat noi dung cau hoi thanh cong", data));
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<DeleteQuestionResponse>> delete(
            @PathVariable UUID questionId) {
        var data = deleteQuestionUseCase.execute(DeleteQuestionCommandMapper.fromId(questionId));
        return ResponseEntity.ok(ApiResponse.success("Xoa cau hoi thanh cong", data));
    }

    @PatchMapping("/{questionId}/review-actions")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> reviewAction(
            @PathVariable UUID questionId,
            @Valid @RequestBody ReviewQuestionRequest request) {
        var targetStatus = QuestionStatus.valueOf(request.targetStatus());
        var command = new ReviewQuestionCommand(questionId, targetStatus, request.note(), request.reason());
        var data = reviewQuestionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Thuc hien hanh dong duyet thanh cong", data));
    }
}
