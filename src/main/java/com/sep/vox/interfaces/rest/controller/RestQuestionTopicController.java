package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.questiontopic.CreateQuestionTopicCommand;
import com.sep.vox.application.port.input.command.questiontopic.UpdateQuestionTopicCommand;
import com.sep.vox.application.port.input.command.questiontopic.UpdateQuestionTopicStatusCommand;
import java.io.IOException;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.input.command.questiontopic.AcceptQuestionTopicImportCommand;
import com.sep.vox.application.port.input.command.questiontopic.DeleteQuestionTopicCommand;
import com.sep.vox.application.port.input.command.questiontopic.PreviewQuestionTopicImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.questiontopic.AcceptQuestionTopicImportUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.CreateQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.DeleteQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.PreviewQuestionTopicImportFromFileUseCase;
import com.sep.vox.application.port.input.command.questionbank.BulkUpdateQuestionScopeStatusCommand;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionTopicStatusResponse;
import com.sep.vox.application.port.input.usecase.questiontopic.BulkUpdateQuestionTopicStatusUseCase;
import com.sep.vox.interfaces.rest.dto.request.questionbank.BulkUpdateQuestionScopeStatusRequest;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicStatusUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicUseCase;
import com.sep.vox.application.response.input.importfile.PreviewImportResponse;
import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.interfaces.rest.dto.request.assessmentpolicysystem.AcceptImportRequest;
import com.sep.vox.interfaces.rest.dto.request.questiontopic.CreateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.questiontopic.UpdateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.questiontopic.UpdateQuestionTopicStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/question-topics")
@RequiredArgsConstructor
public class RestQuestionTopicController {

    private final CreateQuestionTopicUseCase createQuestionTopicUseCase;
    private final UpdateQuestionTopicUseCase updateQuestionTopicUseCase;
    private final UpdateQuestionTopicStatusUseCase updateQuestionTopicStatusUseCase;
    private final BulkUpdateQuestionTopicStatusUseCase bulkUpdateQuestionTopicStatusUseCase;
    private final DeleteQuestionTopicUseCase deleteQuestionTopicUseCase;
    private final PreviewQuestionTopicImportFromFileUseCase previewQuestionTopicImportFromFileUseCase;
    private final AcceptQuestionTopicImportUseCase acceptQuestionTopicImportUseCase;

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewImportResponse>> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("questionBankId") UUID questionBankId) throws IOException {
        UploadedFile uploadedFile = UploadedFile.upload(
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            file.getBytes()
        );
        PreviewImportResponse data = previewQuestionTopicImportFromFileUseCase.execute(
            new PreviewQuestionTopicImportFromFileCommand(questionBankId, uploadedFile)
        );
        return ResponseEntity.ok(ApiResponse.success("Question topic import previewed successfully", data));
    }

    @PostMapping("/import/{sessionId}/accept")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptImport(
            @PathVariable(name = "sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        acceptQuestionTopicImportUseCase.execute(
            new AcceptQuestionTopicImportCommand(sessionId, request.confirmedMapping())
        );
        return ResponseEntity.ok(ApiResponse.success("Question topic import request accepted, processing"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionTopicResponse>> create(@Valid @RequestBody CreateQuestionTopicRequest request) {
        CreateQuestionTopicCommand command = CreateQuestionTopicRequest.toCommand(request);
        CreateQuestionTopicResponse data = createQuestionTopicUseCase.execute(command);
        ApiResponse<CreateQuestionTopicResponse> response = ApiResponse.success("Question topic created successfully", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTopicDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionTopicRequest request) {
        UpdateQuestionTopicCommand command = UpdateQuestionTopicRequest.toCommand(id, request);
        QuestionTopicDto data = updateQuestionTopicUseCase.execute(command);
        ApiResponse<QuestionTopicDto> response = ApiResponse.success("Question topic updated successfully", data);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTopicDto>> updateStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateQuestionTopicStatusRequest request) {
        UpdateQuestionTopicStatusCommand command = UpdateQuestionTopicStatusRequest.toCommand(id, request);
        QuestionTopicDto data = updateQuestionTopicStatusUseCase.execute(command);
        ApiResponse<QuestionTopicDto> response = ApiResponse.success("Question topic status updated successfully", data);
        return ResponseEntity.ok(response);
    }

    /**
     * Partial success: valid items are changed, the others are listed in {@code failed} with a reason.
     *
     * <p>The step people trip over most when loading data: question import only accepts PUBLISHED topics, while
     * topics imported from Excel always enter as DRAFT.
     */
    @PatchMapping("/bulk/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<BulkUpdateQuestionTopicStatusResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateQuestionScopeStatusRequest request) {
        BulkUpdateQuestionTopicStatusResponse data = bulkUpdateQuestionTopicStatusUseCase.execute(
            new BulkUpdateQuestionScopeStatusCommand(request.ids(), request.action()));
        ApiResponse<BulkUpdateQuestionTopicStatusResponse> response = ApiResponse.success("Question topic statuses updated in bulk successfully", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable(name = "id") UUID id) {
        deleteQuestionTopicUseCase.execute(new DeleteQuestionTopicCommand(id));
        ApiResponse<Void> response = ApiResponse.success("Question topic deleted successfully");
        return ResponseEntity.ok(response);
    }
}
