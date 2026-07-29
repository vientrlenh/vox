package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.ApproveRequestCommand;
import com.sep.vox.application.port.input.command.ArchivePlanCommand;
import com.sep.vox.application.port.input.command.CancelSubscriptionCommand;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.command.CreatePaymentLinkForSubscriptionRequestCommand;
import com.sep.vox.application.port.input.command.RejectRequestCommand;
import com.sep.vox.application.port.input.command.RenewSubscriptionCommand;
import com.sep.vox.application.port.input.command.SubmitRequestCommand;
import com.sep.vox.application.port.input.query.ViewQuotaAllocationsQuery;
import com.sep.vox.application.port.input.usecase.subscription.AllocateClassTestQuotaToTeachersUseCase;
import com.sep.vox.application.port.input.usecase.subscription.AllocatePracticeQuotaToStudentsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ApproveRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ArchivePlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.BuyTokensUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CancelSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ConsumeQuotaUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreatePaymentLinkForRenewalUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreatePaymentLinkForSubscriptionRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreatePaymentLinkForTokenPurchaseUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreatePlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.RejectRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.RenewSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.SubmitRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.SyncInvoicePaymentStatusUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewClassTestQuotaAllocationsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewPracticeQuotaAllocationsUseCase;
import com.sep.vox.domain.dto.InvoiceDto;
import com.sep.vox.domain.dto.PaymentLinkDto;
import com.sep.vox.domain.dto.QuotaUserAllocationSummaryDto;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.interfaces.rest.dto.request.AllocateQuotaRequest;
import com.sep.vox.interfaces.rest.dto.request.BuyTokensRequest;
import com.sep.vox.interfaces.rest.dto.request.ConsumeQuotaRequest;
import com.sep.vox.interfaces.rest.dto.request.CreatePlanRequest;
import com.sep.vox.interfaces.rest.dto.request.SubmitRequestRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AllocateQuotaCommandMapper;
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
    private final CreatePaymentLinkForSubscriptionRequestUseCase createPaymentLinkForSubscriptionRequestUseCase;
    private final CreatePaymentLinkForTokenPurchaseUseCase createPaymentLinkForTokenPurchaseUseCase;
    private final CreatePaymentLinkForRenewalUseCase createPaymentLinkForRenewalUseCase;
    private final SyncInvoicePaymentStatusUseCase syncInvoicePaymentStatusUseCase;
    private final AllocateClassTestQuotaToTeachersUseCase allocateClassTestQuotaToTeachersUseCase;
    private final AllocatePracticeQuotaToStudentsUseCase allocatePracticeQuotaToStudentsUseCase;
    private final ViewClassTestQuotaAllocationsUseCase viewClassTestQuotaAllocationsUseCase;
    private final ViewPracticeQuotaAllocationsUseCase viewPracticeQuotaAllocationsUseCase;

    public SubscriptionController(
            CreatePlanUseCase createPlanUseCase,
            ArchivePlanUseCase archivePlanUseCase,
            RenewSubscriptionUseCase renewSubscriptionUseCase,
            CancelSubscriptionUseCase cancelSubscriptionUseCase,
            SubmitRequestUseCase submitRequestUseCase,
            ApproveRequestUseCase approveRequestUseCase,
            RejectRequestUseCase rejectRequestUseCase,
            BuyTokensUseCase buyTokensUseCase,
            ConsumeQuotaUseCase consumeQuotaUseCase,
            CreatePaymentLinkForSubscriptionRequestUseCase createPaymentLinkForSubscriptionRequestUseCase,
            CreatePaymentLinkForTokenPurchaseUseCase createPaymentLinkForTokenPurchaseUseCase,
            CreatePaymentLinkForRenewalUseCase createPaymentLinkForRenewalUseCase,
            SyncInvoicePaymentStatusUseCase syncInvoicePaymentStatusUseCase,
            AllocateClassTestQuotaToTeachersUseCase allocateClassTestQuotaToTeachersUseCase,
            AllocatePracticeQuotaToStudentsUseCase allocatePracticeQuotaToStudentsUseCase,
            ViewClassTestQuotaAllocationsUseCase viewClassTestQuotaAllocationsUseCase,
            ViewPracticeQuotaAllocationsUseCase viewPracticeQuotaAllocationsUseCase) {
        this.createPlanUseCase = createPlanUseCase;
        this.archivePlanUseCase = archivePlanUseCase;
        this.renewSubscriptionUseCase = renewSubscriptionUseCase;
        this.cancelSubscriptionUseCase = cancelSubscriptionUseCase;
        this.submitRequestUseCase = submitRequestUseCase;
        this.approveRequestUseCase = approveRequestUseCase;
        this.rejectRequestUseCase = rejectRequestUseCase;
        this.buyTokensUseCase = buyTokensUseCase;
        this.consumeQuotaUseCase = consumeQuotaUseCase;
        this.createPaymentLinkForSubscriptionRequestUseCase = createPaymentLinkForSubscriptionRequestUseCase;
        this.createPaymentLinkForTokenPurchaseUseCase = createPaymentLinkForTokenPurchaseUseCase;
        this.createPaymentLinkForRenewalUseCase = createPaymentLinkForRenewalUseCase;
        this.syncInvoicePaymentStatusUseCase = syncInvoicePaymentStatusUseCase;
        this.allocateClassTestQuotaToTeachersUseCase = allocateClassTestQuotaToTeachersUseCase;
        this.allocatePracticeQuotaToStudentsUseCase = allocatePracticeQuotaToStudentsUseCase;
        this.viewClassTestQuotaAllocationsUseCase = viewClassTestQuotaAllocationsUseCase;
        this.viewPracticeQuotaAllocationsUseCase = viewPracticeQuotaAllocationsUseCase;
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

    // PAYOS
    @PostMapping("/schools/{schoolId}/subscriptions/{id}/renew/payment-link")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentLinkDto>> createPaymentLinkForRenewal(
            @PathVariable UUID schoolId,
            @PathVariable UUID id) {
        var data = createPaymentLinkForRenewalUseCase.execute(new RenewSubscriptionCommand(schoolId, id));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo link thanh toán thành công", data));
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

    // PAYOS
    @PostMapping("/subscription-requests/{id}/payment-link")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentLinkDto>> createPaymentLinkForSubscriptionRequest(@PathVariable UUID id) {
        var data = createPaymentLinkForSubscriptionRequestUseCase.execute(
            new CreatePaymentLinkForSubscriptionRequestCommand(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo link thanh toán thành công", data));
    }

    // PAYOS
    @PostMapping("/schools/{schoolId}/token-purchases/payment-link")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentLinkDto>> createPaymentLinkForTokenPurchase(
            @PathVariable UUID schoolId,
            @Valid @RequestBody BuyTokensRequest request) {
        var data = createPaymentLinkForTokenPurchaseUseCase.execute(BuyTokensCommandMapper.fromRequest(schoolId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo link thanh toán thành công", data));
    }

    // PAYOS — FE gọi khi PayOS redirect user về returnUrl/cancelUrl, vì PayOS không gọi webhook
    // trong các trường hợp user tự hủy/không hoàn tất thanh toán trên checkout UI.
    @PostMapping("/invoices/{orderCode}/sync-status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceDto>> syncInvoicePaymentStatus(@PathVariable Long orderCode) {
        var data = syncInvoicePaymentStatusUseCase.execute(orderCode);
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ trạng thái hóa đơn thành công", data));
    }

    @PutMapping("/schools/{schoolId}/teachers/class-test-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryDto>> allocateClassTestQuotaToTeachers(
            @PathVariable UUID schoolId,
            @Valid @RequestBody AllocateQuotaRequest request) {
        var data = allocateClassTestQuotaToTeachersUseCase.execute(
            AllocateQuotaCommandMapper.toClassTestCommand(schoolId, request));
        return ResponseEntity.ok(ApiResponse.success("Phân bổ hạn mức kiểm tra lớp cho giáo viên thành công", data));
    }

    @GetMapping("/schools/{schoolId}/teachers/class-test-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryDto>> viewClassTestQuotaAllocations(
            @PathVariable UUID schoolId) {
        var data = viewClassTestQuotaAllocationsUseCase.execute(new ViewQuotaAllocationsQuery(schoolId));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hạn mức kiểm tra lớp thành công", data));
    }

    @PutMapping("/schools/{schoolId}/students/practice-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryDto>> allocatePracticeQuotaToStudents(
            @PathVariable UUID schoolId,
            @Valid @RequestBody AllocateQuotaRequest request) {
        var data = allocatePracticeQuotaToStudentsUseCase.execute(
            AllocateQuotaCommandMapper.toPracticeCommand(schoolId, request));
        return ResponseEntity.ok(ApiResponse.success("Phân bổ hạn mức luyện tập cho học sinh thành công", data));
    }

    @GetMapping("/schools/{schoolId}/students/practice-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryDto>> viewPracticeQuotaAllocations(
            @PathVariable UUID schoolId) {
        var data = viewPracticeQuotaAllocationsUseCase.execute(new ViewQuotaAllocationsQuery(schoolId));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hạn mức luyện tập thành công", data));
    }

    @PostMapping("/internal/subscriptions/consume")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> consumeQuota(@Valid @RequestBody ConsumeQuotaRequest request) {
        consumeQuotaUseCase.execute(new ConsumeQuotaCommand(
            request.subscriptionId(), request.examSessionId(), request.quotaType(), request.amount(), request.userId()
        ));
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận sử dụng hạn mức thành công"));
    }
}
