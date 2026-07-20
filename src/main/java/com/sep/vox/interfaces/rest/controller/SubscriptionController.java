package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.ApproveRequestCommand;
import com.sep.vox.application.port.input.command.ArchivePlanCommand;
import com.sep.vox.application.port.input.command.CancelSubscriptionCommand;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.command.RejectRequestCommand;
import com.sep.vox.application.port.input.command.RenewSubscriptionCommand;
import com.sep.vox.application.port.input.command.SubmitRequestCommand;
import com.sep.vox.application.port.input.usecase.subscription.ApproveRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ArchivePlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.BuyTokensUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CancelSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ConsumeQuotaUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreatePlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.RejectRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.RenewSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.SubmitRequestUseCase;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.interfaces.rest.dto.request.BuyTokensRequest;
import com.sep.vox.interfaces.rest.dto.request.ConsumeQuotaRequest;
import com.sep.vox.interfaces.rest.dto.request.CreatePlanRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitRequestRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.BuyTokensCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreatePlanCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class SubscriptionController {

    private final CreatePlanUseCase createPlanUseCase;
    private final ArchivePlanUseCase archivePlanUseCase;
    private final RenewSubscriptionUseCase renewSubscriptionUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final SubmitRequestUseCase submitRequestUseCase;
    private final ApproveRequestUseCase approveRequestUseCase;
    private final RejectRequestUseCase rejectRequestUseCase;
    private final BuyTokensUseCase buyTokensUseCase;
    private final ConsumeQuotaUseCase consumeQuotaUseCase;

    public SubscriptionController(
            CreatePlanUseCase createPlanUseCase,
            ArchivePlanUseCase archivePlanUseCase,
            RenewSubscriptionUseCase renewSubscriptionUseCase,
            CancelSubscriptionUseCase cancelSubscriptionUseCase,
            SubmitRequestUseCase submitRequestUseCase,
            ApproveRequestUseCase approveRequestUseCase,
            RejectRequestUseCase rejectRequestUseCase,
            BuyTokensUseCase buyTokensUseCase,
            ConsumeQuotaUseCase consumeQuotaUseCase) {
        this.createPlanUseCase = createPlanUseCase;
        this.archivePlanUseCase = archivePlanUseCase;
        this.renewSubscriptionUseCase = renewSubscriptionUseCase;
        this.cancelSubscriptionUseCase = cancelSubscriptionUseCase;
        this.submitRequestUseCase = submitRequestUseCase;
        this.approveRequestUseCase = approveRequestUseCase;
        this.rejectRequestUseCase = rejectRequestUseCase;
        this.buyTokensUseCase = buyTokensUseCase;
        this.consumeQuotaUseCase = consumeQuotaUseCase;
    }

    @PostMapping("/plans")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        var data = createPlanUseCase.execute(CreatePlanCommandMapper.fromRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo gói đăng ký thành công", data));
    }

    @DeleteMapping("/plans/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> archivePlan(@PathVariable UUID id) {
        var data = archivePlanUseCase.execute(new ArchivePlanCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Lưu trữ gói đăng ký thành công", data));
    }

    @PostMapping("/schools/{schoolId}/subscriptions/{id}/renew")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolSubscriptionDto>> renewSubscription(
            @PathVariable UUID schoolId,
            @PathVariable UUID id) {
        var data = renewSubscriptionUseCase.execute(new RenewSubscriptionCommand(schoolId, id));
        return ResponseEntity.ok(ApiResponse.success("Gia hạn gói đăng ký thành công", data));
    }

    @PostMapping("/schools/{schoolId}/subscriptions/{id}/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolSubscriptionDto>> cancelSubscription(
            @PathVariable UUID schoolId,
            @PathVariable UUID id) {
        var data = cancelSubscriptionUseCase.execute(new CancelSubscriptionCommand(schoolId, id));
        return ResponseEntity.ok(ApiResponse.success("Hủy gói đăng ký thành công", data));
    }

    @PostMapping("/schools/{schoolId}/subscription-requests")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionRequestDto>> submitRequest(
            @PathVariable UUID schoolId,
            @Valid @RequestBody SubmitRequestRequest request) {
        var data = submitRequestUseCase.execute(new SubmitRequestCommand(
            schoolId, request.requestType(), request.currentPlanId(), request.requestedPlanId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Gửi yêu cầu thành công", data));
    }

    @PostMapping("/subscription-requests/{id}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionRequestDto>> approveRequest(
            @PathVariable UUID id) {
        var data = approveRequestUseCase.execute(new ApproveRequestCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Duyệt yêu cầu thành công", data));
    }

    @PostMapping("/subscription-requests/{id}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionRequestDto>> rejectRequest(@PathVariable UUID id) {
        var data = rejectRequestUseCase.execute(new RejectRequestCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu thành công", data));
    }

    @PostMapping("/schools/{schoolId}/token-purchases")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<TokenPurchaseDto>> buyTokens(
            @PathVariable UUID schoolId,
            @Valid @RequestBody BuyTokensRequest request) {
        var data = buyTokensUseCase.execute(BuyTokensCommandMapper.fromRequest(schoolId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Mua token thành công", data));
    }

    @PostMapping("/internal/subscriptions/consume")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> consumeQuota(@Valid @RequestBody ConsumeQuotaRequest request) {
        consumeQuotaUseCase.execute(new ConsumeQuotaCommand(
            request.subscriptionId(), request.examSessionId(), request.quotaType(), request.amount()
        ));
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận sử dụng hạn mức thành công"));
    }
}
