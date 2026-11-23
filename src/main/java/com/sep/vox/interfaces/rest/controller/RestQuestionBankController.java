package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.questionbank.CreateQuestionBankGradeCommand;
import com.sep.vox.application.port.input.command.questionbank.CreateSchoolQuestionBankCommand;
import com.sep.vox.application.port.input.command.questionbank.CreateSystemQuestionBankCommand;
import com.sep.vox.application.port.input.command.questionbank.UpdateQuestionBankCommand;
import com.sep.vox.application.port.input.command.questionbank.UpdateQuestionBankStatusCommand;
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
import com.sep.vox.application.port.input.command.questionbank.AcceptQuestionBankImportCommand;
import com.sep.vox.application.port.input.command.questionbank.DeleteQuestionBankCommand;
import com.sep.vox.application.port.input.command.questionbank.DeleteQuestionBankGradeCommand;
import com.sep.vox.application.port.input.command.questionbank.PreviewQuestionBankImportFromFileCommand;
import com.sep.vox.application.port.input.usecase.questionbank.AcceptQuestionBankImportUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateQuestionBankGradeUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.PreviewQuestionBankImportFromFileUseCase;
import com.sep.vox.application.response.input.importfile.PreviewImportResponse;
import com.sep.vox.interfaces.rest.dto.request.assessmentpolicysystem.AcceptImportRequest;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSchoolQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSystemQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankGradeUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankUseCase;
import com.sep.vox.application.port.input.command.questionbank.BulkUpdateQuestionScopeStatusCommand;
import com.sep.vox.application.response.input.question.BulkUpdateQuestionBankStatusResponse;
import com.sep.vox.application.port.input.usecase.questionbank.BulkUpdateQuestionBankStatusUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankStatusUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankUseCase;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.application.response.input.questionbank.DeleteQuestionBankResponse;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionBankGradeDto;
import com.sep.vox.interfaces.rest.dto.request.questionbank.CreateQuestionBankGradeRequest;
import com.sep.vox.interfaces.rest.dto.request.questionbank.CreateSchoolQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.questionbank.CreateSystemQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.questionbank.UpdateQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.questionbank.BulkUpdateQuestionScopeStatusRequest;
import com.sep.vox.interfaces.rest.dto.request.questionbank.UpdateQuestionBankStatusRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/question-banks")
@RequiredArgsConstructor
public class RestQuestionBankController {

    private final CreateSystemQuestionBankUseCase createSystemQuestionBankUseCase;
    private final CreateSchoolQuestionBankUseCase createSchoolQuestionBankUseCase;
    private final UpdateQuestionBankUseCase updateQuestionBankUseCase;
    private final UpdateQuestionBankStatusUseCase updateQuestionBankStatusUseCase;
    private final BulkUpdateQuestionBankStatusUseCase bulkUpdateQuestionBankStatusUseCase;
    private final DeleteQuestionBankUseCase deleteQuestionBankUseCase;
    private final CreateQuestionBankGradeUseCase createQuestionBankGradeUseCase;
    private final DeleteQuestionBankGradeUseCase deleteQuestionBankGradeUseCase;
    private final PreviewQuestionBankImportFromFileUseCase previewQuestionBankImportFromFileUseCase;
    private final AcceptQuestionBankImportUseCase acceptQuestionBankImportUseCase;

    /**
     * Phạm vi (hệ thống hay trường nào) KHÔNG nhận từ client mà suy từ vai trò người đăng nhập —
     * xem {@link PreviewQuestionBankImportFromFileUseCase}.
     */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewImportResponse>> previewImport(
            @RequestParam("file") MultipartFile file) throws IOException {
        UploadedFile uploadedFile = UploadedFile.upload(
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            file.getBytes()
        );
        PreviewImportResponse data = previewQuestionBankImportFromFileUseCase.execute(
            new PreviewQuestionBankImportFromFileCommand(uploadedFile)
        );
        return ResponseEntity.ok(ApiResponse.success("Xem trước import ngân hàng câu hỏi thành công", data));
    }

    @PostMapping("/import/{sessionId}/accept")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> acceptImport(
            @PathVariable(name = "sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        acceptQuestionBankImportUseCase.execute(
            new AcceptQuestionBankImportCommand(sessionId, request.confirmedMapping())
        );
        return ResponseEntity.ok(ApiResponse.success("Đã tiếp nhận yêu cầu import ngân hàng câu hỏi, đang xử lý"));
    }

    @PostMapping("/system")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionBankResponse>> createSystem(@Valid @RequestBody CreateSystemQuestionBankRequest request) {
        CreateSystemQuestionBankCommand command = CreateSystemQuestionBankRequest.toCommand(request);
        CreateQuestionBankResponse data = createSystemQuestionBankUseCase.execute(command);
        ApiResponse<CreateQuestionBankResponse> response = ApiResponse.success("Ngân hàng câu hỏi hệ thống được tạo thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/school")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<CreateQuestionBankResponse>> createSchool(@Valid @RequestBody CreateSchoolQuestionBankRequest request) {
        CreateSchoolQuestionBankCommand command = CreateSchoolQuestionBankRequest.toCommand(request);
        CreateQuestionBankResponse data = createSchoolQuestionBankUseCase.execute(command);
        ApiResponse<CreateQuestionBankResponse> response = ApiResponse.success("Tạo ngân hàng câu hỏi thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankDto>> update(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateQuestionBankRequest request) {
        UpdateQuestionBankCommand command = UpdateQuestionBankRequest.toCommand(id, request);
        QuestionBankDto data = updateQuestionBankUseCase.execute(command);
        ApiResponse<QuestionBankDto> response = ApiResponse.success("Cập nhật ngân hàng câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankDto>> updateStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateQuestionBankStatusRequest request) {
        UpdateQuestionBankStatusCommand command = UpdateQuestionBankStatusRequest.toCommand(id, request);
        QuestionBankDto data = updateQuestionBankStatusUseCase.execute(command);
        ApiResponse<QuestionBankDto> response = ApiResponse.success("Cập nhật trạng thái ngân hàng câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    /** Thành công một phần: mục hợp lệ thì đổi, mục không thì nằm trong {@code failed} kèm lý do. */
    @PatchMapping("/bulk/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<BulkUpdateQuestionBankStatusResponse>> bulkUpdateStatus(
            @Valid @RequestBody BulkUpdateQuestionScopeStatusRequest request) {
        BulkUpdateQuestionBankStatusResponse data = bulkUpdateQuestionBankStatusUseCase.execute(
            new BulkUpdateQuestionScopeStatusCommand(request.ids(), request.action()));
        ApiResponse<BulkUpdateQuestionBankStatusResponse> response = ApiResponse.success("Cập nhật trạng thái ngân hàng câu hỏi hàng loạt thành công", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<DeleteQuestionBankResponse>> delete(@PathVariable(name = "id") UUID id) {
        DeleteQuestionBankResponse data = deleteQuestionBankUseCase.execute(new DeleteQuestionBankCommand(id));
        ApiResponse<DeleteQuestionBankResponse> response = ApiResponse.success("Xóa ngân hàng câu hỏi thành công", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/grades")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankGradeDto>> createGrade(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody CreateQuestionBankGradeRequest request) {
        CreateQuestionBankGradeCommand command = CreateQuestionBankGradeRequest.toCommand(id, request);
        QuestionBankGradeDto data = createQuestionBankGradeUseCase.execute(command);
        ApiResponse<QuestionBankGradeDto> response = ApiResponse.success("Gắn khối lớp cho ngân hàng câu hỏi thành công", data);
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
