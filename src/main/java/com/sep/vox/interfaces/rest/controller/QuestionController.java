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

import com.sep.vox.application.port.input.command.DeleteQuestionAssetCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionCollaboratorCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionCommand;
import com.sep.vox.application.port.input.usecase.question.BulkUpdateQuestionStatusUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateSystemQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionStatusUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.UpsertQuestionEvaluationGuideUseCase;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusResponse;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.application.response.input.question.DeleteQuestionResponse;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.interfaces.rest.dto.request.BulkUpdateQuestionStatusRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionAssetRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionCollaboratorRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.QuestionEvaluationGuideRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionCollaboratorRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.BulkUpdateQuestionStatusCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionAssetCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionCollaboratorCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionAssetCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionCollaboratorCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionStatusCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpsertQuestionEvaluationGuideCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final CreateSystemQuestionBankQuestionUseCase createQuestionUseCase;
    private final UpdateQuestionUseCase updateQuestionUseCase;
    private final UpdateQuestionStatusUseCase updateQuestionStatusUseCase;
    private final DeleteQuestionUseCase deleteQuestionUseCase;
    private final CreateQuestionCollaboratorUseCase createQuestionCollaboratorUseCase;
    private final UpdateQuestionCollaboratorUseCase updateQuestionCollaboratorUseCase;
    private final DeleteQuestionCollaboratorUseCase deleteQuestionCollaboratorUseCase;
    private final CreateQuestionAssetUseCase createQuestionAssetUseCase;
    private final UpdateQuestionAssetUseCase updateQuestionAssetUseCase;
    private final DeleteQuestionAssetUseCase deleteQuestionAssetUseCase;
    private final UpsertQuestionEvaluationGuideUseCase upsertQuestionEvaluationGuideUseCase;
    private final BulkUpdateQuestionStatusUseCase bulkUpdateQuestionStatusUseCase;

    public QuestionController(
            CreateSystemQuestionBankQuestionUseCase createQuestionUseCase,
            UpdateQuestionUseCase updateQuestionUseCase,
            UpdateQuestionStatusUseCase updateQuestionStatusUseCase,
            DeleteQuestionUseCase deleteQuestionUseCase,
            CreateQuestionCollaboratorUseCase createQuestionCollaboratorUseCase,
            UpdateQuestionCollaboratorUseCase updateQuestionCollaboratorUseCase,
            DeleteQuestionCollaboratorUseCase deleteQuestionCollaboratorUseCase,
            CreateQuestionAssetUseCase createQuestionAssetUseCase,
            UpdateQuestionAssetUseCase updateQuestionAssetUseCase,
            DeleteQuestionAssetUseCase deleteQuestionAssetUseCase,
            UpsertQuestionEvaluationGuideUseCase upsertQuestionEvaluationGuideUseCase,
            BulkUpdateQuestionStatusUseCase bulkUpdateQuestionStatusUseCase) {
        this.createQuestionUseCase = createQuestionUseCase;
        this.updateQuestionUseCase = updateQuestionUseCase;
        this.updateQuestionStatusUseCase = updateQuestionStatusUseCase;
        this.deleteQuestionUseCase = deleteQuestionUseCase;
        this.createQuestionCollaboratorUseCase = createQuestionCollaboratorUseCase;
        this.updateQuestionCollaboratorUseCase = updateQuestionCollaboratorUseCase;
        this.deleteQuestionCollaboratorUseCase = deleteQuestionCollaboratorUseCase;
        this.createQuestionAssetUseCase = createQuestionAssetUseCase;
        this.updateQuestionAssetUseCase = updateQuestionAssetUseCase;
        this.deleteQuestionAssetUseCase = deleteQuestionAssetUseCase;
        this.upsertQuestionEvaluationGuideUseCase = upsertQuestionEvaluationGuideUseCase;
        this.bulkUpdateQuestionStatusUseCase = bulkUpdateQuestionStatusUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CreateQuestionResponse>> create(
            @Valid @RequestBody CreateQuestionRequest request) {
        var command = CreateQuestionCommandMapper.fromRequest(request);
        var data = createQuestionUseCase.execute(command);
        var response = ApiResponse.success("Tạo câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        var command = UpdateQuestionCommandMapper.fromRequest(id, request);
        var data = updateQuestionUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionStatusRequest request) {
        var command = UpdateQuestionStatusCommandMapper.fromRequest(id, request);
        var data = updateQuestionStatusUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật trạng thái câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/status/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<BulkUpdateQuestionStatusResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateQuestionStatusRequest request) {
        var command = BulkUpdateQuestionStatusCommandMapper.fromRequest(request);
        var data = bulkUpdateQuestionStatusUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật trạng thái hàng loạt thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<DeleteQuestionResponse>> delete(@PathVariable UUID id) {
        var data = deleteQuestionUseCase.execute(new DeleteQuestionCommand(id));
        var response = ApiResponse.success("Xóa câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/collaborators")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<QuestionCollaboratorDto>> createCollaborator(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQuestionCollaboratorRequest request) {
        var command = CreateQuestionCollaboratorCommandMapper.fromRequest(id, request);
        var data = createQuestionCollaboratorUseCase.execute(command);
        var response = ApiResponse.success("Thêm collaborator thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/collaborators/{collaboratorId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<QuestionCollaboratorDto>> updateCollaborator(
            @PathVariable UUID id,
            @PathVariable UUID collaboratorId,
            @Valid @RequestBody UpdateQuestionCollaboratorRequest request) {
        var command = UpdateQuestionCollaboratorCommandMapper.fromRequest(id, collaboratorId, request);
        var data = updateQuestionCollaboratorUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật quyền collaborator thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/collaborators/{collaboratorId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteCollaborator(
            @PathVariable UUID id,
            @PathVariable UUID collaboratorId) {
        deleteQuestionCollaboratorUseCase.execute(new DeleteQuestionCollaboratorCommand(id, collaboratorId));
        ApiResponse<Void> response = ApiResponse.success("Xóa collaborator thành công");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assets")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionAssetDto>> createAsset(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQuestionAssetRequest request) {
        var command = CreateQuestionAssetCommandMapper.fromRequest(id, request);
        var data = createQuestionAssetUseCase.execute(command);
        var response = ApiResponse.success("Thêm tài nguyên câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionAssetDto>> updateAsset(
            @PathVariable UUID id,
            @PathVariable UUID assetId,
            @Valid @RequestBody UpdateQuestionAssetRequest request) {
        var command = UpdateQuestionAssetCommandMapper.fromRequest(id, assetId, request);
        var data = updateQuestionAssetUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật tài nguyên câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable UUID id,
            @PathVariable UUID assetId) {
        deleteQuestionAssetUseCase.execute(new DeleteQuestionAssetCommand(id, assetId));
        ApiResponse<Void> response = ApiResponse.success("Xóa tài nguyên câu hỏi thành công");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/evaluation-guide")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionEvaluationGuideDto>> upsertEvaluationGuide(
            @PathVariable UUID id,
            @Valid @RequestBody QuestionEvaluationGuideRequest request) {
        var command = UpsertQuestionEvaluationGuideCommandMapper.fromRequest(id, request);
        var data = upsertQuestionEvaluationGuideUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật hướng dẫn chấm thành công", data);
        return ResponseEntity.ok(response);
    }
}
