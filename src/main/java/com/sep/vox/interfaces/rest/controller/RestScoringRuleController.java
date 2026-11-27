package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.command.scoringrule.CreateSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.scoringrule.CreateScoringRuleCommand;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.scoringrule.DeleteSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.scoringrule.DeleteScoringRuleCommand;
import com.sep.vox.application.port.input.usecase.scoringrule.CreateSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.CreateSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.DeleteSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.DeleteSystemScoringRuleUseCase;
import com.sep.vox.interfaces.rest.dto.request.scoringrule.CreateScoringRuleRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

// API to manage Scoring Rules (rules that automatically grade/penalize when a violation is detected)
// attached to one Assessment Policy (system or school). A ScoringRule is always the child of exactly
// one Policy and does not exist on its own.
//
// Note: Create and Delete are in REST. Viewing/searching the (paginated) list and Update have MOVED
// to GraphQL - see
// com.sep.vox.interfaces.graphql.controller.GraphQLScoringRuleController
//   - query    searchSystemScoringRules / searchSchoolScoringRules
//   - mutation updateSystemScoringRule / updateSchoolScoringRule
@RestController
@RequestMapping("/api/v1/assessment-policies")
@RequiredArgsConstructor
public class RestScoringRuleController {

    private final CreateSystemScoringRuleUseCase createSystemScoringRuleUseCase;
    private final CreateSchoolScoringRuleUseCase createSchoolScoringRuleUseCase;
    private final DeleteSystemScoringRuleUseCase deleteSystemScoringRuleUseCase;
    private final DeleteSchoolScoringRuleUseCase deleteSchoolScoringRuleUseCase;

    @Operation(summary = "Creates a Scoring Rule for a system Assessment Policy (only when the Policy is in DRAFT status)")
    @PostMapping("/system/{policyId}/scoring-rules")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSystemScoringRule(
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody CreateScoringRuleRequest request
    ) {
        CreateScoringRuleCommand command = CreateScoringRuleRequest.toCreateScoringRuleCommand(policyId, request);
        UUID ruleId = createSystemScoringRuleUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Scoring Rule created successfully", ruleId));
    }

    @Operation(summary = "Creates a Scoring Rule for a school Assessment Policy (only when the Policy is in DRAFT status)")
    @PostMapping("/schools/{schoolId}/{policyId}/scoring-rules")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolScoringRule(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody CreateScoringRuleRequest request
    ) {
        CreateSchoolScoringRuleCommand command = CreateScoringRuleRequest.toCreateSchoolScoringRuleCommand(schoolId, policyId, request);
        UUID ruleId = createSchoolScoringRuleUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("School Scoring Rule created successfully", ruleId));
    }

    @Operation(summary = "Deletes a Scoring Rule from a system Assessment Policy (only when the Policy is in DRAFT status)")
    @DeleteMapping("/system/{policyId}/scoring-rules/{ruleId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemScoringRule(
            @PathVariable("policyId") UUID policyId,
            @PathVariable("ruleId") UUID ruleId
    ) {
        deleteSystemScoringRuleUseCase.execute(new DeleteScoringRuleCommand(policyId, ruleId));
        return ResponseEntity.ok(ApiResponse.success("Scoring Rule deleted successfully"));
    }

    @Operation(summary = "Deletes a Scoring Rule from a school Assessment Policy (only when the Policy is in DRAFT status)")
    @DeleteMapping("/schools/{schoolId}/{policyId}/scoring-rules/{ruleId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolScoringRule(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("ruleId") UUID ruleId
    ) {
        deleteSchoolScoringRuleUseCase.execute(new DeleteSchoolScoringRuleCommand(schoolId, policyId, ruleId));
        return ResponseEntity.ok(ApiResponse.success("School Scoring Rule deleted successfully"));
    }
}