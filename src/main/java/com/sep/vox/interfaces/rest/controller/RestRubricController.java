package com.sep.vox.interfaces.rest.controller;


import com.sep.vox.application.port.input.command.rubricsystem.AcceptRubricCriterionImportCommand;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptRubricResultBandImportCommand;
import com.sep.vox.application.port.input.command.rubricschool.AcceptSchoolRubricVersionImportCommand;
import com.sep.vox.application.port.input.command.rubricsystem.AcceptSystemRubricVersionImportCommand;
import com.sep.vox.application.port.input.command.rubricschool.AddSchoolRubricVersionsCommand;
import com.sep.vox.application.port.input.command.rubricsystem.AddSystemRubricVersionsCommand;
import com.sep.vox.application.port.input.command.rubricschool.ChangeSchoolRubricVersionStatusCommand;
import com.sep.vox.application.port.input.command.rubricsystem.ChangeSystemRubricVersionStatusCommand;
import com.sep.vox.application.port.input.command.rubricschool.CloneSystemRubricToSchoolCommand;
import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricCommand;
import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricCriteriaCommand;
import com.sep.vox.application.port.input.command.rubricschool.CreateSchoolRubricResultBandsCommand;
import com.sep.vox.application.port.input.command.rubricsystem.CreateSystemRubricCommand;
import com.sep.vox.application.port.input.command.rubricsystem.CreateSystemRubricCriteriaCommand;
import com.sep.vox.application.port.input.command.rubricsystem.CreateSystemRubricResultBandsCommand;
import com.sep.vox.application.port.input.command.rubricschool.DeleteSchoolRubricCommand;
import com.sep.vox.application.port.input.command.rubricschool.DeleteSchoolRubricCriterionCommand;
import com.sep.vox.application.port.input.command.rubricschool.DeleteSchoolRubricResultBandCommand;
import com.sep.vox.application.port.input.command.rubricschool.DeleteSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.DeleteSystemRubricCommand;
import com.sep.vox.application.port.input.command.rubricsystem.DeleteSystemRubricCriterionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.DeleteSystemRubricResultBandCommand;
import com.sep.vox.application.port.input.command.rubricsystem.DeleteSystemRubricVersionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.PreviewRubricCriterionImportCommand;
import com.sep.vox.application.port.input.command.rubricsystem.PreviewRubricResultBandImportCommand;
import com.sep.vox.application.port.input.command.rubricsystem.PreviewRubricVersionImportFromFileCommand;
import com.sep.vox.application.port.input.command.rubricschool.PreviewSchoolRubricVersionImportCommand;
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

import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricCriterionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricResultBandImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricVersionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AddSchoolRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ArchiveSchoolRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ChangeSchoolRubricVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CloneSystemRubricToSchoolUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricResultBandUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricCriterionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricResultBandImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricVersionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricCriterionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricResultBandImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricVersionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AddSystemRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ArchiveSystemRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ChangeSystemRubricVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricResultBandUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricCriterionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricResultBandImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricVersionImportFromFileUseCase;
import com.sep.vox.application.port.input.command.rubricschool.ArchiveSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.command.rubricsystem.ArchiveSystemRubricVersionCommand;
import com.sep.vox.application.response.input.importfile.AcceptRubricCriterionImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptRubricResultBandImportResponse;
import com.sep.vox.application.response.input.importfile.AcceptRubricVersionImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewRubricCriterionImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewRubricResultBandImportResponse;
import com.sep.vox.application.response.input.importfile.PreviewRubricVersionImportResponse;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.interfaces.rest.dto.request.assessmentpolicysystem.AcceptImportRequest;
import com.sep.vox.interfaces.rest.dto.request.rubricschool.CloneSystemRubricRequest;
import com.sep.vox.interfaces.rest.dto.request.rubricsystem.AddRubricVersionsRequest;
import com.sep.vox.interfaces.rest.dto.request.rubricschool.CreateSchoolRubricCriteriaRequest;
import com.sep.vox.interfaces.rest.dto.request.rubricschool.CreateSchoolRubricRequest;
import com.sep.vox.interfaces.rest.dto.request.rubricschool.CreateSchoolRubricResultBandsRequest;
import com.sep.vox.interfaces.rest.dto.request.rubricsystem.CreateSystemRubricCriteriaRequest;
import com.sep.vox.interfaces.rest.dto.request.rubricsystem.CreateSystemRubricRequest;
import com.sep.vox.interfaces.rest.dto.request.rubricsystem.CreateSystemRubricResultBandsRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.PreviewRubricCriterionImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.PreviewRubricResultBandImportCommandMapper;
import com.sep.vox.interfaces.rest.mapper.PreviewRubricVersionImportFromFileCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/rubrics")
@RequiredArgsConstructor
public class RestRubricController {

    private final CreateSchoolRubricCriterionUseCase createSchoolRubricCriterionUseCase;
    private final CreateSchoolRubricUseCase createSchoolRubricUseCase;
    private final CreateSystemRubricUseCase createSystemRubricUseCase;
    private final CreateSystemRubricCriteriaUseCase createSystemRubricCriteriaUseCase;
    private final CreateSchoolRubricResultBandsUseCase createSchoolRubricResultBandsUseCase;
    private final CreateSystemRubricResultBandsUseCase createSystemRubricResultBandsUseCase;
    private final DeleteSchoolRubricVersionUseCase deleteSchoolRubricVersionUseCase;
    private final DeleteSchoolRubricCriterionUseCase deleteSchoolRubricCriterionUseCase;
    private final DeleteSchoolRubricResultBandUseCase deleteSchoolRubricResultBandUseCase;
    private final DeleteSchoolRubricUseCase deleteSchoolRubricUseCase;
    private final DeleteSystemRubricUseCase deleteSystemRubricUseCase;
    private final DeleteSystemRubricVersionUseCase deleteSystemRubricVersionUseCase;
    private final DeleteSystemRubricCriterionUseCase deleteSystemRubricCriterionUseCase;
    private final DeleteSystemRubricResultBandUseCase deleteSystemRubricResultBandUseCase;
    private final ChangeSystemRubricVersionStatusUseCase changeSystemRubricVersionStatusUseCase;
    private final ChangeSchoolRubricVersionStatusUseCase changeSchoolRubricVersionStatusUseCase;
    private final PreviewSystemRubricVersionImportFromFileUseCase previewSystemRubricVersionImportFromFileUseCase;
    private final AcceptSystemRubricVersionImportUseCase acceptSystemRubricVersionImportUseCase;
    private final AddSystemRubricVersionsUseCase addSystemRubricVersionsUseCase;
    private final AcceptSchoolRubricVersionImportUseCase acceptSchoolRubricVersionImportUseCase;
    private final PreviewSchoolRubricVersionImportFromFileUseCase previewSchoolRubricVersionImportFromFileUseCase;
    private final AddSchoolRubricVersionsUseCase addSchoolRubricVersionsUseCase;
    private final PreviewSystemRubricCriterionImportFromFileUseCase previewSystemRubricCriterionImportFromFileUseCase;
    private final AcceptSystemRubricCriterionImportUseCase acceptSystemRubricCriterionImportUseCase;
    private final PreviewSchoolRubricCriterionImportFromFileUseCase previewSchoolRubricCriterionImportFromFileUseCase;
    private final AcceptSchoolRubricCriterionImportUseCase acceptSchoolRubricCriterionImportUseCase;
    private final PreviewSystemRubricResultBandImportFromFileUseCase previewSystemRubricResultBandImportFromFileUseCase;
    private final AcceptSystemRubricResultBandImportUseCase acceptSystemRubricResultBandImportUseCase;
    private final PreviewSchoolRubricResultBandImportFromFileUseCase previewSchoolRubricResultBandImportFromFileUseCase;
    private final AcceptSchoolRubricResultBandImportUseCase acceptSchoolRubricResultBandImportUseCase;
    private final ArchiveSystemRubricVersionUseCase archiveSystemRubricVersionUseCase;
    private final ArchiveSchoolRubricVersionUseCase archiveSchoolRubricVersionUseCase;
    private final CloneSystemRubricToSchoolUseCase cloneSystemRubricToSchoolUseCase;

    //==========================RUBRIC  & RUBRIC VERSION===================================

    @Operation(summary = "Creates one Rubric for the system")
    @PostMapping("/system/rubrics")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSystemRubric(
            @Valid @RequestBody CreateSystemRubricRequest request
    ) {
        CreateSystemRubricCommand command = CreateSystemRubricRequest.toCommand(request);
        UUID response = createSystemRubricUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System Rubric created successfully", response));
    }


    @Operation(summary = "Uploads an Excel/CSV file of Rubric Versions for the system (Step 1: Preview)")
    @PostMapping(value = "/system/rubrics/{rubricId}/versions/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewRubricVersionImportResponse>> previewSystemRubricVersionImport(
            @PathVariable("rubricId") UUID rubricId,
            @RequestParam("file") MultipartFile file) {

        PreviewRubricVersionImportFromFileCommand command = PreviewRubricVersionImportFromFileCommandMapper.fromSystemRequest(rubricId, file);
        PreviewRubricVersionImportResponse sessionId = previewSystemRubricVersionImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Version list file loaded successfully. Session created.", sessionId));
    }

    @Operation(summary = "Confirms and imports Rubric Version data into the DB (Step 2: Accept)")
    @PostMapping(value = "/system/rubrics/versions/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptRubricVersionImportResponse>> acceptSystemRubricVersionImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        AcceptSystemRubricVersionImportCommand command = AcceptImportRequest.toAcceptSystemRubricVersionImportCommand(sessionId, request);
        AcceptRubricVersionImportResponse response = acceptSystemRubricVersionImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System Version import request has been queued for background processing.", response));
    }


    @Operation(summary = "Adds a list of Rubric Versions by hand (Form UI)")
    @PostMapping("/system/rubrics/{rubricId}/versions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> addSystemRubricVersions(
            @PathVariable("rubricId") UUID rubricId,
            @Valid @RequestBody AddRubricVersionsRequest request
    ) {
        AddSystemRubricVersionsCommand command = AddRubricVersionsRequest.toAddSystemRubricVersionsCommand(rubricId, request);
        List<UUID> newVersionIds = addSystemRubricVersionsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Successfully added " + newVersionIds.size() + " new versions to the Rubric.", newVersionIds));
    }

    @Operation(summary = "Creates one Rubric (criteria set) for a school from the UI")
    @PostMapping("/schools/{schoolId}/rubrics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolRubric(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CreateSchoolRubricRequest request
    ) {
        CreateSchoolRubricCommand command = CreateSchoolRubricRequest.toCommand(schoolId, request);
        UUID response = createSchoolRubricUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Draft criteria set created successfully", response));
    }

    /**
     * Copies a PUBLISHED system sample criteria set to become a school's own.
     *
     * <p>Returns the id of the VERSION just created, not the rubric id: that is what the UI needs to go straight
     * to the version page, and also what the school attaches the assessment policy to in the very next step.
     *
     * <p>The copy is always in DRAFT status, so the following flow is identical to when the school writes its own:
     * attach policy -> publish policy -> publish version.
     */
    @Operation(summary = "Copies the system sample criteria set to a school")
    @PostMapping("/schools/{schoolId}/rubrics/clone-from-system")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> cloneSystemRubricToSchool(
            @PathVariable("schoolId") UUID schoolId,
            @Valid @RequestBody CloneSystemRubricRequest request
    ) {
        CloneSystemRubricToSchoolCommand command = CloneSystemRubricRequest.toCommand(schoolId, request);
        UUID versionId = cloneSystemRubricToSchoolUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success(
                "Sample criteria set copied successfully. The new version is in draft status.", versionId));
    }


    @Operation(summary = "Uploads an Excel/CSV file of Rubric Versions for a school (Step 1: Preview)")
    @PostMapping(value = "/schools/{schoolId}/rubrics/{rubricId}/versions/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewRubricVersionImportResponse>> previewSchoolRubricVersionImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("rubricId") UUID rubricId,
            @RequestParam("file") MultipartFile file) {

        PreviewSchoolRubricVersionImportCommand command = PreviewRubricVersionImportFromFileCommandMapper.fromSchoolRequest(schoolId, rubricId, file);
        PreviewRubricVersionImportResponse sessionId = previewSchoolRubricVersionImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Version list file loaded successfully. Session created.", sessionId));
    }

    @Operation(summary = "Confirms and imports Rubric Version data for a school from the draft Session (Step 2: Accept)")
    @PostMapping(value = "/schools/{schoolId}/rubrics/versions/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptRubricVersionImportResponse>> acceptSchoolRubricVersionImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {
        AcceptSchoolRubricVersionImportCommand command = AcceptImportRequest.toAcceptSchoolRubricVersionImportCommand(schoolId, sessionId, request);
        AcceptRubricVersionImportResponse response = acceptSchoolRubricVersionImportUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("School Version import request has been queued for background processing.", response));
    }

    @Operation(summary = "Adds a list of Rubric Versions by hand (Form UI) for a school")
    @PostMapping("/schools/{schoolId}/rubrics/{rubricId}/versions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> addSchoolRubricVersions(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("rubricId") UUID rubricId,
            @Valid @RequestBody AddRubricVersionsRequest request
    ) {
        AddSchoolRubricVersionsCommand command = AddRubricVersionsRequest.toAddSchoolRubricVersionsCommand(schoolId, rubricId, request);
        List<UUID> newVersionIds = addSchoolRubricVersionsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Successfully added " + newVersionIds.size() + " new versions to the school Rubric.", newVersionIds));
    }


    @Operation(summary = "Handles a Rubric version (deleted if DRAFT, archived if PUBLISHED)")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricVersion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId
    ) {
        DeleteSchoolRubricVersionCommand command = new DeleteSchoolRubricVersionCommand(schoolId, versionId);
        deleteSchoolRubricVersionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Rubric version deleted successfully"));
    }


    @Operation(summary = "Deletes a whole school Rubric (only allowed when it has no version inside)")
    @DeleteMapping("/schools/{schoolId}/rubrics/{rubricId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubric(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("rubricId") UUID rubricId
    ) {
        DeleteSchoolRubricCommand command = new DeleteSchoolRubricCommand(schoolId, rubricId);
        deleteSchoolRubricUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("School Rubric deleted successfully"));
    }


    @Operation(summary = "Deletes a whole system Rubric (only allowed when no version is PUBLISHED/ARCHIVED)")
    @DeleteMapping("/system/rubrics/{rubricId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubric(
            @PathVariable("rubricId") UUID rubricId
    ) {
        DeleteSystemRubricCommand command = new DeleteSystemRubricCommand(rubricId);
        deleteSystemRubricUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System Rubric deleted successfully"));
    }


    @Operation(summary = "Deletes a system rubric version")
    @DeleteMapping("/system/rubric-versions/{versionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> manageSystemRubricVersion(
            @PathVariable("versionId") UUID versionId
    ) {
        DeleteSystemRubricVersionCommand command = new DeleteSystemRubricVersionCommand(versionId);
        deleteSystemRubricVersionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System version handled successfully"));
    }

    @Operation(summary = "Changes the status of a System Rubric Version (e.g. DRAFT -> PUBLISHED, ARCHIVED)")
    @PatchMapping("/system/rubric-versions/{versionId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> changeSystemRubricVersionStatus(
            @PathVariable("versionId") UUID versionId,
            @RequestParam(name = "status") RubricStatus status
    ) {
        ChangeSystemRubricVersionStatusCommand command = new ChangeSystemRubricVersionStatusCommand(versionId, status);
        UUID responseId = changeSystemRubricVersionStatusUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Version status changed successfully!", responseId));
    }

    @Operation(summary = "Changes the status of a School Rubric Version (e.g. DRAFT -> PUBLISHED, ARCHIVED)")
    @PatchMapping("/schools/{schoolId}/rubric-versions/{versionId}/status")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> changeSchoolRubricVersionStatus(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @RequestParam(name = "status") RubricStatus status
    ) {
        ChangeSchoolRubricVersionStatusCommand command = new ChangeSchoolRubricVersionStatusCommand(schoolId, versionId, status);
        UUID responseId = changeSchoolRubricVersionStatusUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Version status changed successfully!", responseId));
    }

    @Operation(summary = "Archives a PUBLISHED System Rubric Version")
    @PatchMapping("/system/rubric-versions/{versionId}/archive")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archiveSystemRubricVersion(
            @PathVariable("versionId") UUID versionId
    ) {
        UUID responseId = archiveSystemRubricVersionUseCase.execute(new ArchiveSystemRubricVersionCommand(versionId));
        return ResponseEntity.ok(ApiResponse.success("System Rubric version archived successfully", responseId));
    }

    @Operation(summary = "Archives a PUBLISHED School Rubric Version")
    @PatchMapping("/schools/{schoolId}/rubric-versions/{versionId}/archive")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archiveSchoolRubricVersion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId
    ) {
        UUID responseId = archiveSchoolRubricVersionUseCase.execute(new ArchiveSchoolRubricVersionCommand(schoolId, versionId));
        return ResponseEntity.ok(ApiResponse.success("School Rubric version archived successfully", responseId));
    }


    //=====================RUBRIC CRITERION===============================
    @Operation(summary = "Adds a Criterion to a Rubric Version (RubricVersion) for a school")
    @PostMapping("/schools/{schoolId}/rubric-versions/{versionId}/criteria")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSchoolRubricCriterion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CreateSchoolRubricCriteriaRequest request
    ) {
        CreateSchoolRubricCriteriaCommand command = CreateSchoolRubricCriteriaRequest.toCommand(schoolId, versionId, request);
        List<UUID> newCriterionId = createSchoolRubricCriterionUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Criterion added successfully", newCriterionId));
    }


    // Used when creating the entry assessment (personalised proficiency evaluation)
    @Operation(summary = "Adds a Rubric Criterion to a system Rubric version")
    @PostMapping("/system/rubric-versions/{versionId}/criteria")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSystemRubricCriterion(
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CreateSystemRubricCriteriaRequest request
    ) {
        CreateSystemRubricCriteriaCommand command = CreateSystemRubricCriteriaRequest.toCommand(versionId, request);
        List<UUID> result = createSystemRubricCriteriaUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System criterion added successfully", result));
    }


    @Operation(summary = "Upload/Preview an Excel/CSV file of Rubric Criteria for the system (Step 1: Preview)")
    @PostMapping(value = "/system/rubrics/versions/{versionId}/criterions/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewRubricCriterionImportResponse>> previewSystemRubricCriterionImport(
            @PathVariable("versionId") UUID versionId,
            @RequestParam("file") MultipartFile file) {

        PreviewRubricCriterionImportCommand command = PreviewRubricCriterionImportCommandMapper.fromSystemRequest(versionId, file);
        PreviewRubricCriterionImportResponse response = previewSystemRubricCriterionImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Criteria file loaded successfully. Please check the Preview screen.", response));
    }


    @Operation(summary = "Confirms and imports Criteria (Rubric Criterion) into the DB (Step 2: Accept)")
    @PostMapping(value = "/system/rubrics/criterions/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptRubricCriterionImportResponse>> acceptSystemRubricCriterionImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        AcceptRubricCriterionImportCommand command = AcceptImportRequest.toAcceptRubricCriterionImportCommand(sessionId, request);

        AcceptRubricCriterionImportResponse response = acceptSystemRubricCriterionImportUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("System Criteria import request has been queued for background processing.", response));
    }

    @Operation(summary = "Upload/Preview an Excel/CSV file of Rubric Criteria for a school (Step 1: Preview)")
    @PostMapping(value = "/schools/{schoolId}/rubric-versions/{versionId}/criterions/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<PreviewRubricCriterionImportResponse> previewSchoolRubricCriterionImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @RequestParam("file") MultipartFile file) {

        PreviewRubricCriterionImportCommand command = PreviewRubricCriterionImportCommandMapper.fromSchoolRequest(schoolId, versionId, file);
        PreviewRubricCriterionImportResponse response = previewSchoolRubricCriterionImportFromFileUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirms and imports Criteria (Rubric Criterion) into the DB (Step 2: Accept)")
    @PostMapping("/schools/{schoolId}/rubric-versions/import-sessions/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<AcceptRubricCriterionImportResponse> acceptSchoolRubricCriterionImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        AcceptRubricCriterionImportCommand command = AcceptImportRequest.toAcceptRubricCriterionImportCommand(schoolId, sessionId, request);

        AcceptRubricCriterionImportResponse response = acceptSchoolRubricCriterionImportUseCase.execute(command);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Deletes a Criterion from a school Rubric version")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricCriterion(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @PathVariable("criterionId") UUID criterionId
    ) {
        DeleteSchoolRubricCriterionCommand command = new DeleteSchoolRubricCriterionCommand(schoolId, versionId, criterionId);
        deleteSchoolRubricCriterionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Criterion deleted successfully"));
    }


    @Operation(summary = "Deletes a Criterion (Rubric Version) from a System Rubric version")
    @DeleteMapping("/system/rubric-versions/{versionId}/criteria/{criterionId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubricCriterion(
            @PathVariable("versionId") UUID versionId,
            @PathVariable("criterionId") UUID criterionId
    ) {
        DeleteSystemRubricCriterionCommand command = new DeleteSystemRubricCriterionCommand(versionId, criterionId);
        deleteSystemRubricCriterionUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System criterion deleted successfully"));
    }

    //=====================RUBRIC RESULT BAND===============================


    @Operation(summary = "Adds a result/rating Score band to a System Rubric")
    @PostMapping("/system/rubric-versions/{versionId}/result-bands")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSystemRubricResultBand(
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CreateSystemRubricResultBandsRequest request
    ) {
        CreateSystemRubricResultBandsCommand command = CreateSystemRubricResultBandsRequest.toCommand(versionId, request);
        List<UUID> resultId = createSystemRubricResultBandsUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System score band added successfully", resultId));
    }


    @Operation(summary = "Adds a result/rating Score band (Result Band) to a School Rubric version")
    @PostMapping("/schools/{schoolId}/rubric-versions/{versionId}/result-bands")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<List<UUID>>> createSchoolRubricResultBand(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody CreateSchoolRubricResultBandsRequest request
    ) {
        CreateSchoolRubricResultBandsCommand command = CreateSchoolRubricResultBandsRequest.toCommand(schoolId, versionId, request);
        List<UUID> newResultBandId = createSchoolRubricResultBandsUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("Result band added successfully", newResultBandId));
    }


    @Operation(summary = "Deletes one Result Band from a Rubric version")
    @DeleteMapping("/schools/{schoolId}/rubric-versions/{versionId}/result-bands/{resultBandId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolRubricResultBand(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @PathVariable("resultBandId") UUID resultBandId
    ) {
        DeleteSchoolRubricResultBandCommand command = new DeleteSchoolRubricResultBandCommand(schoolId, versionId, resultBandId);
        deleteSchoolRubricResultBandUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Score band deleted successfully"));
    }


    @Operation(summary = "Deletes a Score band from a System Rubric version")
    @DeleteMapping("/system/rubric-versions/{versionId}/result-bands/{resultBandId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemRubricResultBand(
            @PathVariable("versionId") UUID versionId,
            @PathVariable("resultBandId") UUID resultBandId
    ) {
        DeleteSystemRubricResultBandCommand command = new DeleteSystemRubricResultBandCommand(versionId, resultBandId);
        deleteSystemRubricResultBandUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("System score band deleted successfully"));
    }

    @Operation(summary = "Preview of the Result Band (rating) import file for the System")
    @PostMapping(value = "/system/versions/{versionId}/result-bands/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PreviewRubricResultBandImportResponse>> previewSystemRubricResultBandImport(
            @PathVariable("versionId") UUID versionId,
            @RequestParam("file") MultipartFile file) {

        PreviewRubricResultBandImportCommand command = PreviewRubricResultBandImportCommandMapper.fromSystemRequest(versionId, file);
        PreviewRubricResultBandImportResponse response = previewSystemRubricResultBandImportFromFileUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("System rating file structure loaded successfully.", response));
    }

    @Operation(summary = "Confirms and queues the System rating import request ")
    @PostMapping(value = "/system/versions/result-bands/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<AcceptRubricResultBandImportResponse>> acceptSystemRubricResultBandImport(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        AcceptRubricResultBandImportCommand command = AcceptImportRequest.toAcceptRubricResultBandImportCommand(sessionId, request);
        AcceptRubricResultBandImportResponse response = acceptSystemRubricResultBandImportUseCase.execute(command);

        return ResponseEntity.ok(ApiResponse.success("System rating import request has been queued for background processing.", response));
    }


    @Operation(
            summary = "Step 1: Load the file and preview the rating import data (Preview)",
            description = "Receives an Excel/CSV file, extracts the column structure and returns sample data so the School Admin can check before confirming the import into the DB."
    )
    @PostMapping(value = "/school/{schoolId}/rubric-versions/{versionId}/result-bands/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<PreviewRubricResultBandImportResponse> previewSchoolRubricResultBandImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("versionId") UUID versionId,
            @RequestParam("file") MultipartFile file) {

        PreviewRubricResultBandImportCommand command = PreviewRubricResultBandImportCommandMapper.fromSchoolRequest(schoolId, versionId, file);
        PreviewRubricResultBandImportResponse response = previewSchoolRubricResultBandImportFromFileUseCase.execute(command);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Step 2: Confirm the rating import and queue it in the background (Accept)",
            description = "The School Admin confirms the data is valid. The system moves the Session status to QUEUED to run the import in the background (Async) into the Database."
    )
    @PostMapping(value = "/school/{schoolId}/rubric-versions/result-bands/import/{sessionId}/accept")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<AcceptRubricResultBandImportResponse> acceptSchoolRubricResultBandImport(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody AcceptImportRequest request) {

        AcceptRubricResultBandImportCommand command = AcceptImportRequest.toAcceptRubricResultBandImportCommand(schoolId, sessionId, request);
        AcceptRubricResultBandImportResponse response = acceptSchoolRubricResultBandImportUseCase.execute(command);

        return ResponseEntity.ok(response);
    }

}
