package com.sep.vox.interfaces.rest.controller;

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

import com.sep.vox.application.port.input.command.DeleteQuestionBankCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionBankGradeCommand;
import com.sep.vox.application.port.input.usecase.questionbank.CreateQuestionBankGradeUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSchoolQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSystemQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankGradeUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankStatusUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankUseCase;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.application.response.input.questionbank.DeleteQuestionBankResponse;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionBankGradeDto;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionBankGradeRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionBankStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionBankCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionBankGradeCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionBankCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionBankStatusCommandMapper;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/question-banks")
public class QuestionBankController {

    private final CreateSystemQuestionBankUseCase createSystemQuestionBankUseCase;
    private final CreateSchoolQuestionBankUseCase createSchoolQuestionBankUseCase;
    private final UpdateQuestionBankUseCase updateQuestionBankUseCase;
    private final UpdateQuestionBankStatusUseCase updateQuestionBankStatusUseCase;
    private final DeleteQuestionBankUseCase deleteQuestionBankUseCase;
    private final CreateQuestionBankGradeUseCase createQuestionBankGradeUseCase;
    private final DeleteQuestionBankGradeUseCase deleteQuestionBankGradeUseCase;

    public QuestionBankController(
            CreateSystemQuestionBankUseCase createSystemQuestionBankUseCase,
            CreateSchoolQuestionBankUseCase createSchoolQuestionBankUseCase,
            UpdateQuestionBankUseCase updateQuestionBankUseCase,
            UpdateQuestionBankStatusUseCase updateQuestionBankStatusUseCase,
            DeleteQuestionBankUseCase deleteQuestionBankUseCase,
            CreateQuestionBankGradeUseCase createQuestionBankGradeUseCase,
            DeleteQuestionBankGradeUseCase deleteQuestionBankGradeUseCase) {
        this.createSystemQuestionBankUseCase = createSystemQuestionBankUseCase;
        this.createSchoolQuestionBankUseCase = createSchoolQuestionBankUseCase;
        this.updateQuestionBankUseCase = updateQuestionBankUseCase;
        this.updateQuestionBankStatusUseCase = updateQuestionBankStatusUseCase;
        this.deleteQuestionBankUseCase = deleteQuestionBankUseCase;
        this.createQuestionBankGradeUseCase = createQuestionBankGradeUseCase;
        this.deleteQuestionBankGradeUseCase = deleteQuestionBankGradeUseCase;
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankDto>> update(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateQuestionBankRequest request) {
        var command = UpdateQuestionBankCommandMapper.fromRequest(id, request);
        var data = updateQuestionBankUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật ngân hàng câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankDto>> updateStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateQuestionBankStatusRequest request) {
        var command = UpdateQuestionBankStatusCommandMapper.fromRequest(id, request);
        var data = updateQuestionBankStatusUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật trạng thái ngân hàng câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<DeleteQuestionBankResponse>> delete(@PathVariable(name = "id") UUID id) {
        var data = deleteQuestionBankUseCase.execute(new DeleteQuestionBankCommand(id));
        var response = ApiResponse.success("Xóa ngân hàng câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/grades")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankGradeDto>> createGrade(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody CreateQuestionBankGradeRequest request) {
        var command = CreateQuestionBankGradeCommandMapper.fromRequest(id, request);
        var data = createQuestionBankGradeUseCase.execute(command);
        var response = ApiResponse.success("Gắn khối lớp cho ngân hàng câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/grades/{gradeRowId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGrade(
            @PathVariable(name = "id") UUID id,
            @PathVariable(name = "gradeRowId") UUID gradeRowId) {
        deleteQuestionBankGradeUseCase.execute(new DeleteQuestionBankGradeCommand(id, gradeRowId));
        ApiResponse<Void> response = ApiResponse.success("Bỏ khối lớp khỏi ngân hàng câu hỏi thành công");
        return ResponseEntity.ok(response);
    }
}
