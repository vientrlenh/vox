package com.sep.vox.interfaces.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sep.vox.application.port.input.command.DeleteSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.DeleteSystemAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.PublishSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.PublishSystemAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.AcceptSchoolAssessmentPolicyImportUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CreateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.DeleteSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.PreviewSchoolAssessmentPolicyImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.PublishSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.AcceptSystemAssessmentPolicyImportUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.CreateSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.DeleteSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.PreviewSystemAssessmentPolicyImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.PublishSystemAssessmentPolicyUseCase;
import com.sep.vox.application.response.input.importfile.AcceptAssessmentPolicyImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewAssessmentPolicyImportResponse;
import com.sep.vox.interfaces.rest.dto.request.AcceptImportRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolAssessmentPolicyRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemAssessmentPolicyRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AcceptAssessmentPolicyImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreateAssessmentPolicyCommandMapper;
import com.sep.vox.interfaces.rest.mapper.PreviewAssessmentPolicyImportFromFileCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/assessment-policies")
public class AssessmentPolicyController {

    private final CreateSystemAssessmentPolicyUseCase createSystemAssessmentPolicyUseCase;
    private final CreateSchoolAssessmentPolicyUseCase createSchoolAssessmentPolicyUseCase;
    private final DeleteSystemAssessmentPolicyUseCase deleteSystemAssessmentPolicyUseCase;
    private final DeleteSchoolAssessmentPolicyUseCase deleteSchoolAssessmentPolicyUseCase;
    private final PreviewSystemAssessmentPolicyImportFromFileUseCase previewSystemAssessmentPolicyImportFromFileUseCase;
    private final AcceptSystemAssessmentPolicyImportUseCase acceptSystemAssessmentPolicyImportUseCase;
    private final PreviewSchoolAssessmentPolicyImportFromFileUseCase previewSchoolAssessmentPolicyImportFromFileUseCase;
    private final AcceptSchoolAssessmentPolicyImportUseCase acceptSchoolAssessmentPolicyImportUseCase;
    private final PublishSystemAssessmentPolicyUseCase publishSystemAssessmentPolicyUseCase;
    private final PublishSchoolAssessmentPolicyUseCase publishSchoolAssessmentPolicyUseCase;

    public AssessmentPolicyController(
            CreateSystemAssessmentPolicyUseCase createSystemAssessmentPolicyUseCase,
            CreateSchoolAssessmentPolicyUseCase createSchoolAssessmentPolicyUseCase,
            DeleteSystemAssessmentPolicyUseCase deleteSystemAssessmentPolicyUseCase,
            DeleteSchoolAssessmentPolicyUseCase deleteSchoolAssessmentPolicyUseCase,
            PreviewSystemAssessmentPolicyImportFromFileUseCase previewSystemAssessmentPolicyImportFromFileUseCase,
            AcceptSystemAssessmentPolicyImportUseCase acceptSystemAssessmentPolicyImportUseCase,
            PreviewSchoolAssessmentPolicyImportFromFileUseCase previewSchoolAssessmentPolicyImportFromFileUseCase,
            AcceptSchoolAssessmentPolicyImportUseCase acceptSchoolAssessmentPolicyImportUseCase,
            PublishSystemAssessmentPolicyUseCase publishSystemAssessmentPolicyUseCase,
            PublishSchoolAssessmentPolicyUseCase publishSchoolAssessmentPolicyUseCase) {
        this.createSystemAssessmentPolicyUseCase = createSystemAssessmentPolicyUseCase;
        this.createSchoolAssessmentPolicyUseCase = createSchoolAssessmentPolicyUseCase;
        this.deleteSystemAssessmentPolicyUseCase = deleteSystemAssessmentPolicyUseCase;
        this.deleteSchoolAssessmentPolicyUseCase = deleteSchoolAssessmentPolicyUseCase;
        this.previewSystemAssessmentPolicyImportFromFileUseCase = previewSystemAssessmentPolicyImportFromFileUseCase;
        this.acceptSystemAssessmentPolicyImportUseCase = acceptSystemAssessmentPolicyImportUseCase;
        this.previewSchoolAssessmentPolicyImportFromFileUseCase = previewSchoolAssessmentPolicyImportFromFileUseCase;
        this.acceptSchoolAssessmentPolicyImportUseCase = acceptSchoolAssessmentPolicyImportUseCase;
        this.publishSystemAssessmentPolicyUseCase = publishSystemAssessmentPolicyUseCase;
        this.publishSchoolAssessmentPolicyUseCase = publishSchoolAssessmentPolicyUseCase;
    }

    // Tạo mới Assessment Policy áp dụng toàn hệ thống (System Admin) - có thể gửi 1 lúc nhiều Policy
    @Operation(summary = "Tạo mới một hoặc nhiều Assessment Policy cho hệ thống (áp dụng toàn hệ thống, trạng thái khởi tạo DRAFT)")
    @PostMapping("/system")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSystemAssessmentPolicy(
            @Valid @RequestBody List<@Valid CreateSystemAssessmentPolicyRequest> requests
    ) {
        var commands = CreateAssessmentPolicyCommandMapper.fromSystemRequests(requests);
        List<UUID> policyIds = createSystemAssessmentPolicyUseCase.execute(commands);
        return ResponseEntity.ok(ApiResponse.success("Tạo Assessment Policy hệ thống thành công", policyIds));
    }

    // Tạo mới Assessment Policy cho một trường học (School Admin) - có thể gửi 1 lúc nhiều Policy
    @Operation(summary = "Tạo mới một hoặc nhiều Assessment Policy cho Trường học (trạng thái khởi tạo DRAFT)")
    @PostMapping("/schools/{schoolId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSchoolAssessmentPolicy(
            @PathVariable UUID schoolId,
            @Valid @RequestBody List<@Valid CreateSchoolAssessmentPolicyRequest> requests
    ) {
        var commands = CreateAssessmentPolicyCommandMapper.fromSchoolRequests(schoolId, requests);
        List<UUID> policyIds = createSchoolAssessmentPolicyUseCase.execute(commands);
        return ResponseEntity.ok(ApiResponse.success("Tạo Assessment Policy cho trường học thành công", policyIds));
    }

    // Xóa Assessment Policy áp dụng toàn hệ thống (System Admin)
    @Operation(summary = "Xóa Assessment Policy hệ thống (DRAFT sẽ bị xóa cứng, PUBLISHED sẽ chuyển sang ARCHIVED)")
    @DeleteMapping("/system/{policyId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemAssessmentPolicy(
            @PathVariable UUID policyId
    ) {
        deleteSystemAssessmentPolicyUseCase.execute(new DeleteSystemAssessmentPolicyCommand(policyId));
        return ResponseEntity.ok(ApiResponse.success("Xóa Assessment Policy hệ thống thành công"));
    }

    // Xóa Assessment Policy của một trường học (School Admin)
    @Operation(summary = "Xóa Assessment Policy của trường học (DRAFT sẽ bị xóa cứng, PUBLISHED sẽ chuyển sang ARCHIVED)")
    @DeleteMapping("/schools/{schoolId}/{policyId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolAssessmentPolicy(
            @PathVariable UUID schoolId,
            @PathVariable UUID policyId
    ) {
        deleteSchoolAssessmentPolicyUseCase.execute(new DeleteSchoolAssessmentPolicyCommand(schoolId, policyId));
        return ResponseEntity.ok(ApiResponse.success("Xóa Assessment Policy của trường học thành công"));
    }

    // Xuất bản Assessment Policy áp dụng toàn hệ thống (System Admin)
    @Operation(summary = "Xuất bản (PUBLISH) Assessment Policy hệ thống đang ở trạng thái DRAFT")
    @PatchMapping("/system/{policyId}/publish")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> publishSystemAssessmentPolicy(
            @PathVariable UUID policyId
    ) {
        var policyIdResult = publishSystemAssessmentPolicyUseCase.execute(new PublishSystemAssessmentPolicyCommand(policyId));
        return ResponseEntity.ok(ApiResponse.success("Xuất bản Assessment Policy hệ thống thành công", policyIdResult));
    }

    // Xuất bản Assessment Policy của một trường học (School Admin)
    @Operation(summary = "Xuất bản (PUBLISH) Assessment Policy của trường học đang ở trạng thái DRAFT")
    @PatchMapping("/schools/{schoolId}/{policyId}/publish")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> publishSchoolAssessmentPolicy(
            @PathVariable UUID schoolId,
            @PathVariable UUID policyId
    ) {
        var policyIdResult = publishSchoolAssessmentPolicyUseCase.execute(new PublishSchoolAssessmentPolicyCommand(schoolId, policyId));
        return ResponseEntity.ok(ApiResponse.success("Xuất bản Assessment Policy của trường học thành công", policyIdResult));
    }

    // Review File Import Assessment Policy của hệ thống
    @Operation(summary = "Upload file Excel/CSV danh sách Assessment Policy cho hệ thống (Bước 1: Preview)")
    @PostMapping(value = "/system/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewAssessmentPolicyImportResponse>> previewSystemAssessmentPolicyImport(
            @RequestParam("file") MultipartFile file) {
        var command = PreviewAssessmentPolicyImportFromFileCommandMapper.fromSystemRequest(file);
        var response = previewSystemAssessmentPolicyImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Nạp file danh sách Assessment Policy thành công. Tạo phiên làm việc (Session) hoàn tất.", response));
    }

    // Xác nhận import Assessment Policy của hệ thống
    @Operation(summary = "Xác nhận và nhập dữ liệu Assessment Policy hệ thống vào DB (Bước 2: Accept)")
    @PostMapping("/system/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptAssessmentPolicyImportResponse>> acceptSystemAssessmentPolicyImport(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        var command = AcceptAssessmentPolicyImportCommandMapper.fromSystemRequest(sessionId, request);
        var response = acceptSystemAssessmentPolicyImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Đã đưa yêu cầu Import Assessment Policy hệ thống vào hàng đợi ngầm xử lý.", response));
    }

    // Preview Assessment Policy của trường học
    @Operation(summary = "Upload file Excel/CSV danh sách Assessment Policy cho Trường học (Bước 1: Preview)")
    @PostMapping(value = "/schools/{schoolId}/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewAssessmentPolicyImportResponse>> previewSchoolAssessmentPolicyImport(
            @PathVariable UUID schoolId,
            @RequestParam("file") MultipartFile file) {
        var command = PreviewAssessmentPolicyImportFromFileCommandMapper.fromSchoolRequest(schoolId, file);
        var response = previewSchoolAssessmentPolicyImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Nạp file danh sách Assessment Policy thành công. Tạo phiên làm việc (Session) hoàn tất.", response));
    }

    // Chấp nhận import Assessment Policy của trường
    @Operation(summary = "Xác nhận và nhập dữ liệu Assessment Policy cho Trường học từ Session nháp (Bước 2: Accept)")
    @PostMapping("/schools/{schoolId}/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptAssessmentPolicyImportResponse>> acceptSchoolAssessmentPolicyImport(
            @PathVariable UUID schoolId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        var command = AcceptAssessmentPolicyImportCommandMapper.fromSchoolRequest(schoolId, sessionId, request);
        var response = acceptSchoolAssessmentPolicyImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Đã đưa yêu cầu Import Assessment Policy của trường vào hàng đợi ngầm xử lý.", response));
    }
}
