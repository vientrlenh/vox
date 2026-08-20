package com.sep.vox.interfaces.rest.controller;

import java.io.IOException;
import java.util.UUID;

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
import com.sep.vox.application.port.input.command.AcceptQuestionTopicImportCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionTopicCommand;
import com.sep.vox.application.port.input.command.PreviewQuestionTopicImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.questiontopic.AcceptQuestionTopicImportUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.CreateQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.DeleteQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.PreviewQuestionTopicImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicStatusUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicUseCase;
import com.sep.vox.application.response.input.importfile.PreviewImportResponse;
import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionTopicRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionTopicStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.CreateQuestionTopicCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionTopicCommandMapper;
import com.sep.vox.interfaces.rest.mapper.UpdateQuestionTopicStatusCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/question-topics")
public class QuestionTopicController {

    private final CreateQuestionTopicUseCase createQuestionTopicUseCase;
    private final UpdateQuestionTopicUseCase updateQuestionTopicUseCase;
    private final UpdateQuestionTopicStatusUseCase updateQuestionTopicStatusUseCase;
    private final DeleteQuestionTopicUseCase deleteQuestionTopicUseCase;
    private final PreviewQuestionTopicImportFromFileUseCase previewQuestionTopicImportFromFileUseCase;
    private final AcceptQuestionTopicImportUseCase acceptQuestionTopicImportUseCase;

    public QuestionTopicController(
            CreateQuestionTopicUseCase createQuestionTopicUseCase,
            UpdateQuestionTopicUseCase updateQuestionTopicUseCase,
            UpdateQuestionTopicStatusUseCase updateQuestionTopicStatusUseCase,
            DeleteQuestionTopicUseCase deleteQuestionTopicUseCase,
            PreviewQuestionTopicImportFromFileUseCase previewQuestionTopicImportFromFileUseCase,
            AcceptQuestionTopicImportUseCase acceptQuestionTopicImportUseCase) {
        this.createQuestionTopicUseCase = createQuestionTopicUseCase;
        this.updateQuestionTopicUseCase = updateQuestionTopicUseCase;
        this.updateQuestionTopicStatusUseCase = updateQuestionTopicStatusUseCase;
        this.deleteQuestionTopicUseCase = deleteQuestionTopicUseCase;
        this.previewQuestionTopicImportFromFileUseCase = previewQuestionTopicImportFromFileUseCase;
        this.acceptQuestionTopicImportUseCase = acceptQuestionTopicImportUseCase;
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewImportResponse>> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("questionBankId") UUID questionBankId) throws IOException {
        var uploadedFile = UploadedFile.upload(
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            file.getBytes()
        );
        var data = previewQuestionTopicImportFromFileUseCase.execute(
            new PreviewQuestionTopicImportFromFileCommand(questionBankId, uploadedFile)
        );
        return ResponseEntity.ok(ApiResponse.success("Xem trước import chủ đề câu hỏi thành công", data));
    }

    @PostMapping("/import/{sessionId}/accept")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptImport(
            @PathVariable(name = "sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        acceptQuestionTopicImportUseCase.execute(
            new AcceptQuestionTopicImportCommand(sessionId, request.confirmedMapping())
        );
        return ResponseEntity.ok(ApiResponse.success("Đã tiếp nhận yêu cầu import chủ đề câu hỏi, đang xử lý"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionTopicResponse>> create(@Valid @RequestBody CreateQuestionTopicRequest request) {
        var command = CreateQuestionTopicCommandMapper.fromRequest(request);
        var data = createQuestionTopicUseCase.execute(command);
        var response = ApiResponse.success("Tạo chủ đề câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTopicDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionTopicRequest request) {
        var command = UpdateQuestionTopicCommandMapper.fromRequest(id, request);
        var data = updateQuestionTopicUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật chủ đề câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionTopicDto>> updateStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateQuestionTopicStatusRequest request) {
        var command = UpdateQuestionTopicStatusCommandMapper.fromRequest(id, request);
        var data = updateQuestionTopicStatusUseCase.execute(command);
        var response = ApiResponse.success("Cập nhật trạng thái chủ đề câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable(name = "id") UUID id) {
        deleteQuestionTopicUseCase.execute(new DeleteQuestionTopicCommand(id));
        ApiResponse<Void> response = ApiResponse.success("Xóa chủ đề câu hỏi thành công");
        return ResponseEntity.ok(response);
    }
}
