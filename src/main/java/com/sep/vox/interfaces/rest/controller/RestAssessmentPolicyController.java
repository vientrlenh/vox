package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.assessmentpolicyschool.AcceptSchoolAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.AcceptSystemAssessmentPolicyImportCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.PreviewSchoolAssessmentPolicyImportFromFileCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.PreviewSystemAssessmentPolicyImportFromFileCommand;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
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

import com.sep.vox.application.port.input.command.assessmentpolicyschool.ArchiveSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.ArchiveSystemAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.DeleteSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.DeleteSystemAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.PublishSchoolAssessmentPoliciesByRubricVersionCommand;
import com.sep.vox.application.port.input.command.assessmentpolicyschool.PublishSchoolAssessmentPolicyCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.PublishSystemAssessmentPoliciesByRubricVersionCommand;
import com.sep.vox.application.port.input.command.assessmentpolicysystem.PublishSystemAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.AcceptSchoolAssessmentPolicyImportUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.ArchiveSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CreateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.DeleteSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.PreviewSchoolAssessmentPolicyImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.PublishSchoolAssessmentPoliciesByRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.PublishSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.AcceptSystemAssessmentPolicyImportUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.ArchiveSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.CreateSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.DeleteSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.PreviewSystemAssessmentPolicyImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.PublishSystemAssessmentPoliciesByRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.PublishSystemAssessmentPolicyUseCase;
import com.sep.vox.application.response.input.importfile.AcceptAssessmentPolicyImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewAssessmentPolicyImportResponse;
import com.sep.vox.interfaces.rest.dto.request.assessmentpolicysystem.AcceptImportRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentpolicysystem.CreateSchoolAssessmentPolicyRequest;
import com.sep.vox.interfaces.rest.dto.request.assessmentpolicysystem.CreateSystemAssessmentPolicyRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.PreviewAssessmentPolicyImportFromFileCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/assessment-policies")
@RequiredArgsConstructor
public class RestAssessmentPolicyController {

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
    private final PublishSystemAssessmentPoliciesByRubricVersionUseCase publishSystemAssessmentPoliciesByRubricVersionUseCase;
    private final PublishSchoolAssessmentPoliciesByRubricVersionUseCase publishSchoolAssessmentPoliciesByRubricVersionUseCase;
    private final ArchiveSystemAssessmentPolicyUseCase archiveSystemAssessmentPolicyUseCase;
    private final ArchiveSchoolAssessmentPolicyUseCase archiveSchoolAssessmentPolicyUseCase;

    @Operation(summary = "Creates one or more system-wide Assessment Policies (applies to the whole system, initial status DRAFT)")
    @PostMapping("/system")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSystemAssessmentPolicy(
            @Valid @RequestBody List<@Valid CreateSystemAssessmentPolicyRequest> requests
    ) {
        List<CreateAssessmentPolicyCommand> commands = CreateSystemAssessmentPolicyRequest.toCommands(requests);
        List<UUID> policyIds = createSystemAssessmentPolicyUseCase.execute(commands);
        return ResponseEntity.ok(ApiResponse.success("System Assessment Policy created successfully", policyIds));
    }

    @Operation(summary = "Creates one or more Assessment Policies for a school (initial status DRAFT)")
    @PostMapping("/schools/{schoolId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSchoolAssessmentPolicy(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody List<@Valid CreateSchoolAssessmentPolicyRequest> requests
    ) {
        List<CreateAssessmentPolicyCommand> commands = CreateSchoolAssessmentPolicyRequest.toCommands(schoolId, requests);
        List<UUID> policyIds = createSchoolAssessmentPolicyUseCase.execute(commands);
        return ResponseEntity.ok(ApiResponse.success("School Assessment Policy created successfully", policyIds));
    }

    @Operation(summary = "Deletes a system Assessment Policy (DRAFT is hard-deleted, PUBLISHED becomes ARCHIVED)")
    @DeleteMapping("/system/{policyId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemAssessmentPolicy(
            @PathVariable("policyId") UUID policyId
    ) {
        deleteSystemAssessmentPolicyUseCase.execute(new DeleteSystemAssessmentPolicyCommand(policyId));
        return ResponseEntity.ok(ApiResponse.success("System Assessment Policy deleted successfully"));
    }

    @Operation(summary = "Deletes a school Assessment Policy (DRAFT is hard-deleted, PUBLISHED becomes ARCHIVED)")
    @DeleteMapping("/schools/{schoolId}/{policyId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolAssessmentPolicy(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("policyId") UUID policyId
    ) {
        deleteSchoolAssessmentPolicyUseCase.execute(new DeleteSchoolAssessmentPolicyCommand(schoolId, policyId));
        return ResponseEntity.ok(ApiResponse.success("School Assessment Policy deleted successfully"));
    }

    @Operation(summary = "Archives a PUBLISHED system Assessment Policy")
    @PatchMapping("/system/{policyId}/archive")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archiveSystemAssessmentPolicy(
            @PathVariable("policyId") UUID policyId
    ) {
        UUID policyIdResult = archiveSystemAssessmentPolicyUseCase.execute(new ArchiveSystemAssessmentPolicyCommand(policyId));
        return ResponseEntity.ok(ApiResponse.success("System Assessment Policy archived successfully", policyIdResult));
    }

    @Operation(summary = "Archives a PUBLISHED school Assessment Policy")
    @PatchMapping("/schools/{schoolId}/{policyId}/archive")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archiveSchoolAssessmentPolicy(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("policyId") UUID policyId
    ) {
        UUID policyIdResult = archiveSchoolAssessmentPolicyUseCase.execute(new ArchiveSchoolAssessmentPolicyCommand(schoolId, policyId));
        return ResponseEntity.ok(ApiResponse.success("School Assessment Policy archived successfully", policyIdResult));
    }

    @Operation(summary = "Publishes a system Assessment Policy that is in DRAFT status")
    @PatchMapping("/system/{policyId}/publish")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> publishSystemAssessmentPolicy(
            @PathVariable("policyId") UUID policyId
    ) {
        UUID policyIdResult = publishSystemAssessmentPolicyUseCase.execute(new PublishSystemAssessmentPolicyCommand(policyId));
        return ResponseEntity.ok(ApiResponse.success("System Assessment Policy published successfully", policyIdResult));
    }

    @Operation(summary = "Publishes a school Assessment Policy that is in DRAFT status")
    @PatchMapping("/schools/{schoolId}/{policyId}/publish")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> publishSchoolAssessmentPolicy(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("policyId") UUID policyId
    ) {
        UUID policyIdResult = publishSchoolAssessmentPolicyUseCase.execute(new PublishSchoolAssessmentPolicyCommand(schoolId, policyId));
        return ResponseEntity.ok(ApiResponse.success("School Assessment Policy published successfully", policyIdResult));
    }

    @Operation(summary = "Publishes all DRAFT system Assessment Policies linked to a Rubric Version (preparation for publishing the Rubric Version)")
    @PatchMapping("/system/rubric-version/{rubricVersionId}/publish-all")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> publishSystemAssessmentPoliciesByRubricVersion(
            @PathVariable("rubricVersionId") UUID rubricVersionId
    ) {
        List<UUID> policyIds = publishSystemAssessmentPoliciesByRubricVersionUseCase.execute(
                new PublishSystemAssessmentPoliciesByRubricVersionCommand(rubricVersionId));
        return ResponseEntity.ok(ApiResponse.success("System Assessment Policies published in bulk successfully", policyIds));
    }

    @Operation(summary = "Publishes all DRAFT school Assessment Policies linked to a Rubric Version (preparation for publishing the Rubric Version)")
    @PatchMapping("/schools/{schoolId}/rubric-version/{rubricVersionId}/publish-all")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> publishSchoolAssessmentPoliciesByRubricVersion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("rubricVersionId") UUID rubricVersionId
    ) {
        List<UUID> policyIds = publishSchoolAssessmentPoliciesByRubricVersionUseCase.execute(
                new PublishSchoolAssessmentPoliciesByRubricVersionCommand(schoolId, rubricVersionId));
        return ResponseEntity.ok(ApiResponse.success("School Assessment Policies published in bulk successfully", policyIds));
    }

    @Operation(summary = "Uploads an Excel/CSV file of system Assessment Policies (Step 1: Preview)")
    @PostMapping(value = "/system/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewAssessmentPolicyImportResponse>> previewSystemAssessmentPolicyImport(
            @RequestParam("file") MultipartFile file) {
        PreviewSystemAssessmentPolicyImportFromFileCommand command = PreviewAssessmentPolicyImportFromFileCommandMapper.fromSystemRequest(file);
        PreviewAssessmentPolicyImportResponse response = previewSystemAssessmentPolicyImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Assessment Policy file loaded successfully. Session created.", response));
    }

    @Operation(summary = "Confirms and imports system Assessment Policies into the DB (Step 2: Accept)")
    @PostMapping("/system/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptAssessmentPolicyImportResponse>> acceptSystemAssessmentPolicyImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        AcceptSystemAssessmentPolicyImportCommand command = AcceptImportRequest.toAcceptSystemAssessmentPolicyImportCommand(sessionId, request);
        AcceptAssessmentPolicyImportResponse response = acceptSystemAssessmentPolicyImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System Assessment Policy import request has been queued for background processing.", response));
    }

    @Operation(summary = "Uploads an Excel/CSV file of school Assessment Policies (Step 1: Preview)")
    @PostMapping(value = "/schools/{schoolId}/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewAssessmentPolicyImportResponse>> previewSchoolAssessmentPolicyImport(
            @PathVariable("schoolId") UUID schoolId,
            @RequestParam("file") MultipartFile file) {
        PreviewSchoolAssessmentPolicyImportFromFileCommand command = PreviewAssessmentPolicyImportFromFileCommandMapper.fromSchoolRequest(schoolId, file);
        PreviewAssessmentPolicyImportResponse response = previewSchoolAssessmentPolicyImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Assessment Policy file loaded successfully. Session created.", response));
    }

    @Operation(summary = "Confirms and imports school Assessment Policies from the draft Session (Step 2: Accept)")
    @PostMapping("/schools/{schoolId}/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptAssessmentPolicyImportResponse>> acceptSchoolAssessmentPolicyImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        AcceptSchoolAssessmentPolicyImportCommand command = AcceptImportRequest.toAcceptSchoolAssessmentPolicyImportCommand(schoolId, sessionId, request);
        AcceptAssessmentPolicyImportResponse response = acceptSchoolAssessmentPolicyImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("School Assessment Policy import request has been queued for background processing.", response));
    }
}
