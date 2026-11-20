package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.DeleteSchoolScoringRuleCommand;
import com.sep.vox.application.port.input.command.DeleteScoringRuleCommand;
import com.sep.vox.application.port.input.usecase.scoringrule.CreateSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.CreateSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.DeleteSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.DeleteSystemScoringRuleUseCase;
import com.sep.vox.interfaces.rest.dto.request.CreateScoringRuleRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.ScoringRuleCommandMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

// API quản lý Scoring Rule (luật tự động chấm/phạt điểm khi phát hiện vi phạm) gắn với 1 Assessment Policy
// (hệ thống hoặc trường học). ScoringRule luôn là con của đúng 1 Policy, không tồn tại độc lập.
//
// Lưu ý: Create và Delete ở REST. Xem/Tìm kiếm danh sách (phân trang) và Cập nhật đã CHUYỂN sang GraphQL - xem
// com.sep.vox.interfaces.graphql.controller.ScoringRuleController
//   - query    searchSystemScoringRules / searchSchoolScoringRules
//   - mutation updateSystemScoringRule / updateSchoolScoringRule
@RestController
@RequestMapping("/api/v1/assessment-policies")
public class ScoringRuleController {

    private final CreateSystemScoringRuleUseCase createSystemScoringRuleUseCase;
    private final CreateSchoolScoringRuleUseCase createSchoolScoringRuleUseCase;
    private final DeleteSystemScoringRuleUseCase deleteSystemScoringRuleUseCase;
    private final DeleteSchoolScoringRuleUseCase deleteSchoolScoringRuleUseCase;

    public ScoringRuleController(
            CreateSystemScoringRuleUseCase createSystemScoringRuleUseCase,
            CreateSchoolScoringRuleUseCase createSchoolScoringRuleUseCase,
            DeleteSystemScoringRuleUseCase deleteSystemScoringRuleUseCase,
            DeleteSchoolScoringRuleUseCase deleteSchoolScoringRuleUseCase) {
        this.createSystemScoringRuleUseCase = createSystemScoringRuleUseCase;
        this.createSchoolScoringRuleUseCase = createSchoolScoringRuleUseCase;
        this.deleteSystemScoringRuleUseCase = deleteSystemScoringRuleUseCase;
        this.deleteSchoolScoringRuleUseCase = deleteSchoolScoringRuleUseCase;
    }

    // Tạo mới 1 Scoring Rule cho Assessment Policy hệ thống.
    // Chỉ tạo được khi Policy đang DRAFT (tránh thay đổi luật chấm điểm ngầm khi Policy đã PUBLISHED đang chấm bài thi thật).
    @Operation(summary = "Tạo mới Scoring Rule cho Assessment Policy hệ thống (chỉ khi Policy đang ở trạng thái DRAFT)")
    @PostMapping("/system/{policyId}/scoring-rules")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSystemScoringRule(
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody CreateScoringRuleRequest request
    ) {
        var command = ScoringRuleCommandMapper.fromCreateRequest(policyId, request);
        var ruleId = createSystemScoringRuleUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Tạo Scoring Rule thành công", ruleId));
    }

    // Tạo mới 1 Scoring Rule cho Assessment Policy của trường học.
    // Chỉ tạo được khi Policy đang DRAFT.
    @Operation(summary = "Tạo mới Scoring Rule cho Assessment Policy của trường học (chỉ khi Policy đang ở trạng thái DRAFT)")
    @PostMapping("/schools/{schoolId}/{policyId}/scoring-rules")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createSchoolScoringRule(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody CreateScoringRuleRequest request
    ) {
        var command = ScoringRuleCommandMapper.fromCreateSchoolRequest(schoolId, policyId, request);
        var ruleId = createSchoolScoringRuleUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Tạo Scoring Rule cho trường học thành công", ruleId));
    }

    // Xóa 1 Scoring Rule khỏi Assessment Policy hệ thống. Chỉ xóa được khi Policy đang DRAFT.
    @Operation(summary = "Xóa Scoring Rule khỏi Assessment Policy hệ thống (chỉ khi Policy đang ở trạng thái DRAFT)")
    @DeleteMapping("/system/{policyId}/scoring-rules/{ruleId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSystemScoringRule(
            @PathVariable("policyId") UUID policyId,
            @PathVariable("ruleId") UUID ruleId
    ) {
        deleteSystemScoringRuleUseCase.execute(new DeleteScoringRuleCommand(policyId, ruleId));
        return ResponseEntity.ok(ApiResponse.success("Xóa Scoring Rule thành công"));
    }

    // Xóa 1 Scoring Rule khỏi Assessment Policy của trường học. Chỉ xóa được khi Policy đang DRAFT.
    @Operation(summary = "Xóa Scoring Rule khỏi Assessment Policy của trường học (chỉ khi Policy đang ở trạng thái DRAFT)")
    @DeleteMapping("/schools/{schoolId}/{policyId}/scoring-rules/{ruleId}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSchoolScoringRule(
            @PathVariable("schoolId") UUID schoolId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("ruleId") UUID ruleId
    ) {
        deleteSchoolScoringRuleUseCase.execute(new DeleteSchoolScoringRuleCommand(schoolId, policyId, ruleId));
        return ResponseEntity.ok(ApiResponse.success("Xóa Scoring Rule cho trường học thành công"));
    }
}