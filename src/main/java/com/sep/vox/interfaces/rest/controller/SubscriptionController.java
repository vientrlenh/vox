package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
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

import com.sep.vox.application.port.input.command.ApproveRequestCommand;
import com.sep.vox.application.port.input.command.ArchiveSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.CancelSubscriptionCommand;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.command.ForceSuspendSubscriptionCommand;
import com.sep.vox.application.port.input.command.UnsuspendSubscriptionCommand;
import com.sep.vox.application.port.input.command.CreatePaymentLinkForRenewalCommand;
import com.sep.vox.application.port.input.command.CreatePaymentLinkForSubscriptionRequestCommand;
import com.sep.vox.application.port.input.command.DeleteDraftSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.PublishSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.RejectRequestCommand;
import com.sep.vox.application.port.input.command.RenewSubscriptionCommand;
import com.sep.vox.application.port.input.query.PreviewRenewalQuery;
import com.sep.vox.application.port.input.query.ViewQuotaAllocationsQuery;
import com.sep.vox.application.port.input.usecase.subscription.AllocateExamQuotaToTeachersUseCase;
import com.sep.vox.application.port.input.usecase.subscription.AllocatePracticeQuotaToStudentsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ApproveRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ArchiveSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CancelSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ConsumeQuotaUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreatePaymentLinkForRenewalUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreatePaymentLinkForSubscriptionRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreatePaymentLinkForTokenPurchaseUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreateSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.DeleteDraftSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ForceSuspendSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.PreviewRenewalUseCase;
import com.sep.vox.application.port.input.usecase.subscription.PublishSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.RejectRequestUseCase;
import com.sep.vox.application.port.input.usecase.subscription.RenewSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.UnsuspendSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewExamQuotaAllocationsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewPracticeQuotaAllocationsUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.domain.dto.RenewalPreviewDto;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.interfaces.rest.dto.request.AllocateQuotaRequest;
import com.sep.vox.interfaces.rest.dto.request.BuyTokensRequest;
import com.sep.vox.interfaces.rest.dto.request.ConsumeQuotaRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSubscriptionPlanRequest;
import com.sep.vox.interfaces.rest.dto.request.PaymentMethodRequest;
import com.sep.vox.interfaces.rest.dto.request.SuspendSubscriptionRequest;
import com.sep.vox.interfaces.rest.dto.request.UnsuspendSubscriptionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.AllocateQuotaCommandMapper;
import com.sep.vox.interfaces.rest.mapper.BuyTokensCommandMapper;
import com.sep.vox.interfaces.rest.mapper.CreatePlanCommandMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final CreateSubscriptionPlanUseCase createSubscriptionPlanUseCase;
    private final ArchiveSubscriptionPlanUseCase archiveSubscriptionPlanUseCase;
    private final PublishSubscriptionPlanUseCase publishSubscriptionPlanUseCase;
    private final DeleteDraftSubscriptionPlanUseCase deleteDraftSubscriptionPlanUseCase;
    private final RenewSubscriptionUseCase renewSubscriptionUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final ForceSuspendSubscriptionUseCase forceSuspendSubscriptionUseCase;
    private final UnsuspendSubscriptionUseCase unsuspendSubscriptionUseCase;
    private final ApproveRequestUseCase approveRequestUseCase;
    private final RejectRequestUseCase rejectRequestUseCase;
    private final ConsumeQuotaUseCase consumeQuotaUseCase;
    private final CreatePaymentLinkForSubscriptionRequestUseCase createPaymentLinkForSubscriptionRequestUseCase;
    private final CreatePaymentLinkForTokenPurchaseUseCase createPaymentLinkForTokenPurchaseUseCase;
    private final CreatePaymentLinkForRenewalUseCase createPaymentLinkForRenewalUseCase;
    private final PreviewRenewalUseCase previewRenewalUseCase;
    private final AllocateExamQuotaToTeachersUseCase allocateExamQuotaToTeachersUseCase;
    private final AllocatePracticeQuotaToStudentsUseCase allocatePracticeQuotaToStudentsUseCase;
    private final ViewExamQuotaAllocationsUseCase viewExamQuotaAllocationsUseCase;
    private final ViewPracticeQuotaAllocationsUseCase viewPracticeQuotaAllocationsUseCase;

    public SubscriptionController(
            CreateSubscriptionPlanUseCase createSubscriptionPlanUseCase,
            ArchiveSubscriptionPlanUseCase archiveSubscriptionPlanUseCase,
            PublishSubscriptionPlanUseCase publishSubscriptionPlanUseCase,
            DeleteDraftSubscriptionPlanUseCase deleteDraftSubscriptionPlanUseCase,
            RenewSubscriptionUseCase renewSubscriptionUseCase,
            CancelSubscriptionUseCase cancelSubscriptionUseCase,
            ForceSuspendSubscriptionUseCase forceSuspendSubscriptionUseCase,
            UnsuspendSubscriptionUseCase unsuspendSubscriptionUseCase,
            ApproveRequestUseCase approveRequestUseCase,
            RejectRequestUseCase rejectRequestUseCase,
            ConsumeQuotaUseCase consumeQuotaUseCase,
            CreatePaymentLinkForSubscriptionRequestUseCase createPaymentLinkForSubscriptionRequestUseCase,
            CreatePaymentLinkForTokenPurchaseUseCase createPaymentLinkForTokenPurchaseUseCase,
            CreatePaymentLinkForRenewalUseCase createPaymentLinkForRenewalUseCase,
            PreviewRenewalUseCase previewRenewalUseCase,
            AllocateExamQuotaToTeachersUseCase allocateExamQuotaToTeachersUseCase,
            AllocatePracticeQuotaToStudentsUseCase allocatePracticeQuotaToStudentsUseCase,
            ViewExamQuotaAllocationsUseCase viewExamQuotaAllocationsUseCase,
            ViewPracticeQuotaAllocationsUseCase viewPracticeQuotaAllocationsUseCase) {
        this.createSubscriptionPlanUseCase = createSubscriptionPlanUseCase;
        this.archiveSubscriptionPlanUseCase = archiveSubscriptionPlanUseCase;
        this.publishSubscriptionPlanUseCase = publishSubscriptionPlanUseCase;
        this.deleteDraftSubscriptionPlanUseCase = deleteDraftSubscriptionPlanUseCase;
        this.renewSubscriptionUseCase = renewSubscriptionUseCase;
        this.cancelSubscriptionUseCase = cancelSubscriptionUseCase;
        this.forceSuspendSubscriptionUseCase = forceSuspendSubscriptionUseCase;
        this.unsuspendSubscriptionUseCase = unsuspendSubscriptionUseCase;
        this.approveRequestUseCase = approveRequestUseCase;
        this.rejectRequestUseCase = rejectRequestUseCase;
        this.consumeQuotaUseCase = consumeQuotaUseCase;
        this.createPaymentLinkForSubscriptionRequestUseCase = createPaymentLinkForSubscriptionRequestUseCase;
        this.createPaymentLinkForTokenPurchaseUseCase = createPaymentLinkForTokenPurchaseUseCase;
        this.createPaymentLinkForRenewalUseCase = createPaymentLinkForRenewalUseCase;
        this.previewRenewalUseCase = previewRenewalUseCase;
        this.allocateExamQuotaToTeachersUseCase = allocateExamQuotaToTeachersUseCase;
        this.allocatePracticeQuotaToStudentsUseCase = allocatePracticeQuotaToStudentsUseCase;
        this.viewExamQuotaAllocationsUseCase = viewExamQuotaAllocationsUseCase;
        this.viewPracticeQuotaAllocationsUseCase = viewPracticeQuotaAllocationsUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createPlan(@Valid @RequestBody CreateSubscriptionPlanRequest request) {
        var data = createSubscriptionPlanUseCase.execute(CreatePlanCommandMapper.fromRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo gói đăng ký thành công", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archivePlan(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "replacedByPlanId", required = false) UUID replacedByPlanId) {
        var data = archiveSubscriptionPlanUseCase.execute(new ArchiveSubscriptionPlanCommand(id, replacedByPlanId));
        return ResponseEntity.ok(ApiResponse.success("Lưu trữ gói đăng ký thành công", data));
    }

    // PATCH chứ không phải POST: xuất bản là đổi MỘT trường status của gói đã tồn tại (DRAFT ->
    // ACTIVE), không tạo ra tài nguyên mới nào. Cũng vì thế mà idempotent theo nghĩa người dùng
    // quan tâm -- bấm hai lần thì lần sau báo "chỉ xuất bản được gói nháp" chứ không sinh thêm gì.
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> publishPlan(@PathVariable(name = "id") UUID id) {
        var data = publishSubscriptionPlanUseCase.execute(new PublishSubscriptionPlanCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xuất bản gói đăng ký thành công", data));
    }

    @DeleteMapping("/{id}/draft")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDraftPlan(@PathVariable(name = "id") UUID id) {
        deleteDraftSubscriptionPlanUseCase.execute(new DeleteDraftSubscriptionPlanCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa gói nháp thành công"));
    }

    @PostMapping("/schools/{schoolId}/subscriptions/{id}/renew")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolSubscriptionDto>> renewSubscription(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "id") UUID id) {
        var data = renewSubscriptionUseCase.execute(new RenewSubscriptionCommand(schoolId, id));
        return ResponseEntity.ok(ApiResponse.success("Gia hạn gói đăng ký thành công", data));
    }

    @GetMapping("/schools/{schoolId}/subscriptions/{id}/renewal-preview")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<RenewalPreviewDto>> previewRenewal(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "id") UUID id) {
        var data = previewRenewalUseCase.execute(new PreviewRenewalQuery(schoolId, id));
        return ResponseEntity.ok(ApiResponse.success("Xem trước gia hạn thành công", data));
    }

    @PostMapping("/schools/{schoolId}/subscriptions/{id}/renew/payment-link")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentLinkDto>> createPaymentLinkForRenewal(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "acceptedPlanId", required = false) UUID acceptedPlanId, 
            @RequestBody @Valid PaymentMethodRequest request
        ) {
        var data = createPaymentLinkForRenewalUseCase.execute(
            new CreatePaymentLinkForRenewalCommand(schoolId, id, acceptedPlanId, request.paymentMethod()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo link thanh toán thành công", data));
    }

    @PostMapping("/schools/{schoolId}/subscriptions/{id}/cancel")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolSubscriptionDto>> cancelSubscription(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "id") UUID id) {
        var data = cancelSubscriptionUseCase.execute(new CancelSubscriptionCommand(schoolId, id));
        return ResponseEntity.ok(ApiResponse.success("Hủy gói đăng ký thành công", data));
    }

    // Chỉ SYSTEM_ADMIN -- khác cancel/renew, đây là hành động cưỡng chế, School Admin không được tự làm.
    @PostMapping("/schools/{schoolId}/subscriptions/{id}/suspend")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolSubscriptionDto>> suspendSubscription(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "id") UUID id,
            @RequestBody @Valid SuspendSubscriptionRequest request) {
        var data = forceSuspendSubscriptionUseCase.execute(
            new ForceSuspendSubscriptionCommand(schoolId, id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Đình chỉ gói đăng ký thành công", data));
    }

    @PostMapping("/schools/{schoolId}/subscriptions/{id}/unsuspend")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SchoolSubscriptionDto>> unsuspendSubscription(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "id") UUID id,
            @RequestBody @Valid UnsuspendSubscriptionRequest request) {
        var data = unsuspendSubscriptionUseCase.execute(
            new UnsuspendSubscriptionCommand(schoolId, id, request.note()));
        return ResponseEntity.ok(ApiResponse.success("Gỡ đình chỉ gói đăng ký thành công", data));
    }

    // Đặt đơn đăng ký gói đã chuyển sang POST /api/v1/orders/subscription (OrderController): yêu cầu
    // đăng ký giờ là một Order chứ không còn là SubscriptionRequest riêng.

    @PostMapping("/subscription-requests/{id}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionRequestDto>> approveRequest(
            @PathVariable(name = "id") UUID id) {
        var data = approveRequestUseCase.execute(new ApproveRequestCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Duyệt yêu cầu thành công", data));
    }

    @PostMapping("/subscription-requests/{id}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionRequestDto>> rejectRequest(@PathVariable(name = "id") UUID id) {
        var data = rejectRequestUseCase.execute(new RejectRequestCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu thành công", data));
    }

    // Nạp thêm số dư đã chuyển sang POST /api/v1/orders/topup (OrderController). Khác hẳn endpoint cũ:
    // nó chỉ đặt đơn PENDING chứ không cộng hạn mức ngay, nên không còn phải cấm phương thức online.

    @PostMapping("/subscription-requests/{id}/payment-link")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentLinkDto>> createPaymentLinkForSubscriptionRequest(@PathVariable(name = "id") UUID id, @RequestBody @Valid PaymentMethodRequest request) {
        var data = createPaymentLinkForSubscriptionRequestUseCase.execute(
            new CreatePaymentLinkForSubscriptionRequestCommand(id, request.paymentMethod()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo link thanh toán thành công", data));
    }

    @PostMapping("/schools/{schoolId}/token-purchases/payment-link")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentLinkDto>> createPaymentLinkForTokenPurchase(
            @PathVariable(name = "schoolId") UUID schoolId,
            @Valid @RequestBody BuyTokensRequest request) {
        var data = createPaymentLinkForTokenPurchaseUseCase.execute(BuyTokensCommandMapper.fromRequest(schoolId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo link thanh toán thành công", data));
    }

    @PutMapping("/schools/{schoolId}/teachers/exam-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryResponse>> allocateExamQuotaToTeachers(
            @PathVariable(name = "schoolId") UUID schoolId,
            @Valid @RequestBody AllocateQuotaRequest request) {
        var data = allocateExamQuotaToTeachersUseCase.execute(
            AllocateQuotaCommandMapper.toExamCommand(schoolId, request));
        return ResponseEntity.ok(ApiResponse.success("Phân bổ hạn mức thi cho giáo viên thành công", data));
    }

    @GetMapping("/schools/{schoolId}/teachers/exam-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryResponse>> viewExamQuotaAllocations(
            @PathVariable(name = "schoolId") UUID schoolId) {
        var data = viewExamQuotaAllocationsUseCase.execute(new ViewQuotaAllocationsQuery(schoolId));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hạn mức thi của giáo viên thành công", data));
    }

    @PutMapping("/schools/{schoolId}/students/practice-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryResponse>> allocatePracticeQuotaToStudents(
            @PathVariable(name = "schoolId") UUID schoolId,
            @Valid @RequestBody AllocateQuotaRequest request) {
        var data = allocatePracticeQuotaToStudentsUseCase.execute(
            AllocateQuotaCommandMapper.toPracticeCommand(schoolId, request));
        return ResponseEntity.ok(ApiResponse.success("Phân bổ hạn mức luyện tập cho học sinh thành công", data));
    }

    @GetMapping("/schools/{schoolId}/students/practice-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryResponse>> viewPracticeQuotaAllocations(
            @PathVariable(name = "schoolId") UUID schoolId) {
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
