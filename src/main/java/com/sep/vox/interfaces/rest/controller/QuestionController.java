package com.sep.vox.interfaces.rest.controller;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.sep.vox.application.port.input.command.CloneQuestionCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionAssetCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionCollaboratorCommand;
import com.sep.vox.application.port.input.command.DeleteQuestionCommand;
import com.sep.vox.application.port.input.command.PreviewQuestionImportFromFileCommand;
import com.sep.vox.application.port.input.query.GetQuestionAssetUploadUrlQuery;
import com.sep.vox.application.port.input.service.QuestionSpreadsheetService;
import com.sep.vox.application.port.input.usecase.question.AcceptQuestionImportUseCase;
import com.sep.vox.application.port.input.usecase.question.BulkUpdateQuestionStatusUseCase;
import com.sep.vox.application.port.input.usecase.question.CloneQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateSystemQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.GetQuestionAssetUploadUrlUseCase;
import com.sep.vox.application.port.input.usecase.question.PreviewQuestionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.question.RegenerateQuestionAssetAnalysisUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionStatusUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpsertQuestionEvaluationGuideUseCase;
import com.sep.vox.application.response.input.importfile.PreviewQuestionImportResponse;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionStatusResponse;
import com.sep.vox.application.response.input.question.CreateQuestionResponse;
import com.sep.vox.application.response.input.question.DeleteQuestionResponse;
import com.sep.vox.application.response.input.question.UpdateQuestionResponse;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.interfaces.rest.dto.request.AcceptQuestionImportRequest;
import com.sep.vox.interfaces.rest.dto.request.BulkUpdateQuestionStatusRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionAssetRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionCollaboratorRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.QuestionEvaluationGuideRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionAssetRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionCollaboratorRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateQuestionStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AcceptQuestionImportCommandMapper;
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
    private final CloneQuestionUseCase cloneQuestionUseCase;
    private final UpdateQuestionStatusUseCase updateQuestionStatusUseCase;
    private final DeleteQuestionUseCase deleteQuestionUseCase;
    private final CreateQuestionCollaboratorUseCase createQuestionCollaboratorUseCase;
    private final UpdateQuestionCollaboratorUseCase updateQuestionCollaboratorUseCase;
    private final DeleteQuestionCollaboratorUseCase deleteQuestionCollaboratorUseCase;
    private final CreateQuestionAssetUseCase createQuestionAssetUseCase;
    private final UpdateQuestionAssetUseCase updateQuestionAssetUseCase;
    private final DeleteQuestionAssetUseCase deleteQuestionAssetUseCase;
    private final RegenerateQuestionAssetAnalysisUseCase regenerateQuestionAssetAnalysisUseCase;
    private final GetQuestionAssetUploadUrlUseCase getQuestionAssetUploadUrlUseCase;
    private final UpsertQuestionEvaluationGuideUseCase upsertQuestionEvaluationGuideUseCase;
    private final BulkUpdateQuestionStatusUseCase bulkUpdateQuestionStatusUseCase;
    private final PreviewQuestionImportFromFileUseCase previewQuestionImportFromFileUseCase;
    private final AcceptQuestionImportUseCase acceptQuestionImportUseCase;
    private final QuestionSpreadsheetService questionSpreadsheetService;

    public QuestionController(
            CreateSystemQuestionBankQuestionUseCase createQuestionUseCase,
            UpdateQuestionUseCase updateQuestionUseCase,
            CloneQuestionUseCase cloneQuestionUseCase,
            UpdateQuestionStatusUseCase updateQuestionStatusUseCase,
            DeleteQuestionUseCase deleteQuestionUseCase,
            CreateQuestionCollaboratorUseCase createQuestionCollaboratorUseCase,
            UpdateQuestionCollaboratorUseCase updateQuestionCollaboratorUseCase,
            DeleteQuestionCollaboratorUseCase deleteQuestionCollaboratorUseCase,
            CreateQuestionAssetUseCase createQuestionAssetUseCase,
            UpdateQuestionAssetUseCase updateQuestionAssetUseCase,
            DeleteQuestionAssetUseCase deleteQuestionAssetUseCase,
            RegenerateQuestionAssetAnalysisUseCase regenerateQuestionAssetAnalysisUseCase,
            GetQuestionAssetUploadUrlUseCase getQuestionAssetUploadUrlUseCase,
            UpsertQuestionEvaluationGuideUseCase upsertQuestionEvaluationGuideUseCase,
            BulkUpdateQuestionStatusUseCase bulkUpdateQuestionStatusUseCase,
            PreviewQuestionImportFromFileUseCase previewQuestionImportFromFileUseCase,
            AcceptQuestionImportUseCase acceptQuestionImportUseCase,
            QuestionSpreadsheetService questionSpreadsheetService) {
        this.createQuestionUseCase = createQuestionUseCase;
        this.updateQuestionUseCase = updateQuestionUseCase;
        this.cloneQuestionUseCase = cloneQuestionUseCase;
        this.updateQuestionStatusUseCase = updateQuestionStatusUseCase;
        this.deleteQuestionUseCase = deleteQuestionUseCase;
        this.createQuestionCollaboratorUseCase = createQuestionCollaboratorUseCase;
        this.updateQuestionCollaboratorUseCase = updateQuestionCollaboratorUseCase;
        this.deleteQuestionCollaboratorUseCase = deleteQuestionCollaboratorUseCase;
        this.createQuestionAssetUseCase = createQuestionAssetUseCase;
        this.updateQuestionAssetUseCase = updateQuestionAssetUseCase;
        this.deleteQuestionAssetUseCase = deleteQuestionAssetUseCase;
        this.regenerateQuestionAssetAnalysisUseCase = regenerateQuestionAssetAnalysisUseCase;
        this.getQuestionAssetUploadUrlUseCase = getQuestionAssetUploadUrlUseCase;
        this.upsertQuestionEvaluationGuideUseCase = upsertQuestionEvaluationGuideUseCase;
        this.bulkUpdateQuestionStatusUseCase = bulkUpdateQuestionStatusUseCase;
        this.previewQuestionImportFromFileUseCase = previewQuestionImportFromFileUseCase;
        this.acceptQuestionImportUseCase = acceptQuestionImportUseCase;
        this.questionSpreadsheetService = questionSpreadsheetService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<CreateQuestionResponse>> create(
            @Valid @RequestBody CreateQuestionRequest request) {
        var command = CreateQuestionCommandMapper.fromRequest(request);
        var data = createQuestionUseCase.execute(command);
        var response = ApiResponse.success("Tao cau hoi thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PreviewQuestionImportResponse>> previewImport(
            @RequestParam("file") MultipartFile file,
            // Bỏ trống CẢ HAI = import hàng loạt: mỗi dòng trong tệp tự khai mã ngân hàng + mã
            // chủ đề. Phạm vi trường vẫn do server chốt theo người đăng nhập.
            @RequestParam(name = "questionBankId", required = false) UUID questionBankId,
            @RequestParam(name = "questionTopicId", required = false) UUID questionTopicId) throws IOException {
        var uploadedFile = UploadedFile.upload(
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            file.getBytes()
        );
        var data = previewQuestionImportFromFileUseCase.execute(
            new PreviewQuestionImportFromFileCommand(questionBankId, questionTopicId, uploadedFile)
        );
        return ResponseEntity.ok(ApiResponse.success("Preview import cau hoi thanh cong", data));
    }

    @PostMapping("/import/{sessionId}/accept")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Object>> acceptImport(
            @PathVariable(name = "sessionId") UUID sessionId,
            @Valid @RequestBody AcceptQuestionImportRequest request) {
        var command = AcceptQuestionImportCommandMapper.fromRequest(sessionId, request);
        acceptQuestionImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Yeu cau import cau hoi da duoc tiep nhan, dang xu ly"));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> export(
            @RequestParam(name = "questionBankId", required = false) UUID questionBankId,
            @RequestParam(name = "questionTopicId", required = false) UUID questionTopicId,
            @RequestParam(name = "topicName", required = false) String topicName,
            @RequestParam(name = "status", required = false) QuestionStatus status,
            @RequestParam(name = "type", required = false) QuestionType type,
            @RequestParam(name = "sharing", required = false) QuestionSharing sharing,
            @RequestParam(name = "scope", required = false) String scope,
            @RequestParam(name = "keyword", required = false) String keyword) {
        var file = questionSpreadsheetService.exportQuestions(
            questionBankId,
            questionTopicId,
            topicName,
            status,
            type,
            sharing,
            scope,
            keyword
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + questionSpreadsheetService.exportFileName() + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(file);
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> downloadTemplate(
            @RequestParam(name = "type", required = false) String type) {
        var file = questionSpreadsheetService.downloadTemplate(type);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + questionSpreadsheetService.templateFileName() + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(file);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<UpdateQuestionResponse>> update(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        var command = UpdateQuestionCommandMapper.fromRequest(id, request);
        var data = updateQuestionUseCase.execute(command);
        var response = ApiResponse.success("Cap nhat cau hoi thanh cong", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionDto>> clone(@PathVariable(name = "id") UUID id) {
        var data = cloneQuestionUseCase.execute(new CloneQuestionCommand(id));
        var response = ApiResponse.success("Nhan ban cau hoi thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionDto>> updateStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateQuestionStatusRequest request) {
        var command = UpdateQuestionStatusCommandMapper.fromRequest(id, request);
        var data = updateQuestionStatusUseCase.execute(command);
        var response = ApiResponse.success("Cap nhat trang thai cau hoi thanh cong", data);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/status/bulk")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<BulkUpdateQuestionStatusResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateQuestionStatusRequest request) {
        var command = BulkUpdateQuestionStatusCommandMapper.fromRequest(request);
        var data = bulkUpdateQuestionStatusUseCase.execute(command);
        var response = ApiResponse.success("Cap nhat trang thai hang loat thanh cong", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<DeleteQuestionResponse>> delete(@PathVariable(name = "id") UUID id) {
        var data = deleteQuestionUseCase.execute(new DeleteQuestionCommand(id));
        var response = ApiResponse.success("Xoa cau hoi thanh cong", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/collaborators")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<QuestionCollaboratorDto>> createCollaborator(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody CreateQuestionCollaboratorRequest request) {
        var command = CreateQuestionCollaboratorCommandMapper.fromRequest(id, request);
        var data = createQuestionCollaboratorUseCase.execute(command);
        var response = ApiResponse.success("Them collaborator thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/collaborators/{collaboratorId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<QuestionCollaboratorDto>> updateCollaborator(
            @PathVariable(name = "id") UUID id,
            @PathVariable(name = "collaboratorId") UUID collaboratorId,
            @Valid @RequestBody UpdateQuestionCollaboratorRequest request) {
        var command = UpdateQuestionCollaboratorCommandMapper.fromRequest(id, collaboratorId, request);
        var data = updateQuestionCollaboratorUseCase.execute(command);
        var response = ApiResponse.success("Cap nhat quyen collaborator thanh cong", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/collaborators/{collaboratorId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteCollaborator(
            @PathVariable(name = "id") UUID id,
            @PathVariable(name = "collaboratorId") UUID collaboratorId) {
        deleteQuestionCollaboratorUseCase.execute(new DeleteQuestionCollaboratorCommand(id, collaboratorId));
        ApiResponse<Void> response = ApiResponse.success("Xoa collaborator thanh cong");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assets")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionAssetDto>> createAsset(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody CreateQuestionAssetRequest request) {
        var command = CreateQuestionAssetCommandMapper.fromRequest(id, request);
        var data = createQuestionAssetUseCase.execute(command);
        var response = ApiResponse.success("Them tai nguyen cau hoi thanh cong", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/assets/upload-url")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<?>> getAssetUploadUrl(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "contentType") String contentType) {
        var data = getQuestionAssetUploadUrlUseCase.execute(new GetQuestionAssetUploadUrlQuery(id, contentType));
        return ResponseEntity.ok(ApiResponse.success("Lấy upload URL tài nguyên câu hỏi thành công", data));
    }

    @PutMapping("/{id}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionAssetDto>> updateAsset(
            @PathVariable(name = "id") UUID id,
            @PathVariable(name = "assetId") UUID assetId,
            @Valid @RequestBody UpdateQuestionAssetRequest request) {
        var command = UpdateQuestionAssetCommandMapper.fromRequest(id, assetId, request);
        var data = updateQuestionAssetUseCase.execute(command);
        var response = ApiResponse.success("Cap nhat tai nguyen cau hoi thanh cong", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable(name = "id") UUID id,
            @PathVariable(name = "assetId") UUID assetId) {
        deleteQuestionAssetUseCase.execute(new DeleteQuestionAssetCommand(id, assetId));
        ApiResponse<Void> response = ApiResponse.success("Xoa tai nguyen cau hoi thanh cong");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assets/{assetId}/regenerate-analysis")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionAssetDto>> regenerateAssetAnalysis(
            @PathVariable(name = "id") UUID id,
            @PathVariable(name = "assetId") UUID assetId) {
        var data = regenerateQuestionAssetAnalysisUseCase.execute(
            new RegenerateQuestionAssetAnalysisUseCase.Command(id, assetId)
        );
        return ResponseEntity.ok(ApiResponse.success("Yeu cau tao lai phan tich asset da duoc tiep nhan", data));
    }

    @PutMapping("/{id}/evaluation-guide")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<QuestionEvaluationGuideDto>> upsertEvaluationGuide(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody QuestionEvaluationGuideRequest request) {
        var command = UpsertQuestionEvaluationGuideCommandMapper.fromRequest(id, request);
        var data = upsertQuestionEvaluationGuideUseCase.execute(command);
        var response = ApiResponse.success("Cap nhat huong dan cham thanh cong", data);
        return ResponseEntity.ok(response);
    }
}
