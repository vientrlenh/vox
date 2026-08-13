package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.event.InvoicePaidPayloadV1;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.PurchaseStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.RequestType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

// Logic "chốt" kết quả thanh toán 1 invoice (PAID -> approve subscription/renew/finalize token purchase,
// hoặc FAILED). Không phụ thuộc cổng thanh toán nào: dùng chung cho callback của mọi provider
// (ProcessPaymentCallbackUseCase) và job quét định kỳ (PendingInvoiceReconciler) — để business logic
// chỉ nằm ở đúng 1 chỗ.
@Service
public class InvoiceSettlementService {

    private static final String SCHOOL_ADMIN_ROLE_CODE = "SCHOOL_ADMIN";

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final TokenPurchaseRepository tokenPurchaseRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final FinancialEventRepository financialEventRepository;
    private final SubscriptionPlanResolver subscriptionPlanResolver;
    private final SchoolUserRepository schoolUserRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;
    private final SchoolDebtNotificationService schoolDebtNotificationService;

    public InvoiceSettlementService(
            InvoiceRepository invoiceRepository,
            SubscriptionRequestRepository subscriptionRequestRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            TokenPurchaseRepository tokenPurchaseRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            FinancialEventRepository financialEventRepository,
            SubscriptionPlanResolver subscriptionPlanResolver,
            SchoolUserRepository schoolUserRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService,
            SchoolDebtNotificationService schoolDebtNotificationService) {
        this.invoiceRepository = invoiceRepository;
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.tokenPurchaseRepository = tokenPurchaseRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.financialEventRepository = financialEventRepository;
        this.subscriptionPlanResolver = subscriptionPlanResolver;
        this.schoolUserRepository = schoolUserRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
    }

    // Idempotent: nếu invoice không còn PENDING (đã được chốt trước đó, kể cả bởi lần gọi khác) thì bỏ qua.
    // Trả về false nếu không có gì thay đổi.
    // Webhook PayOS không phân biệt được lý do thất bại cụ thể nên mặc định FAILED.
    @Transactional
    public void settle(Invoice invoice, boolean success) {
        settle(invoice, success, InvoiceStatus.FAILED);
    }

    // failureStatus: dùng khi biết chính xác lý do (vd CANCELLED từ PaymentLinkRemoteStatus của PayOS),
    // thay vì luôn gộp chung thành FAILED.
    //
    // Lock lại invoice bằng SELECT ... FOR UPDATE trước khi check PENDING: invoice truyền vào có thể đã
    // được load từ trước (không lock) nên nếu chỉ check trên object đó, 2 lời gọi settle() song song cho
    // cùng 1 invoice (vd: webhook PayOS đua với PendingInvoiceReconciler quét cùng lúc) đều có thể đọc
    // thấy PENDING trước khi bên kia commit, dẫn tới chốt thanh toán trùng 2 lần.
    @Transactional
    public void settle(Invoice invoice, boolean success, InvoiceStatus failureStatus) {
        invoice = invoiceRepository.findByIdForUpdate(invoice.getId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn"));
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            return;
        }

        var now = Instant.now();
        if (!success) {
            invoice.setStatus(failureStatus);
            if (failureStatus == InvoiceStatus.CANCELLED) {
                invoice.setPaidAt(now);
            }
            invoiceRepository.save(invoice);
            return;
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(now);

        // Ghi sổ theo đúng cổng đã thu tiền cho hóa đơn này, không mặc định PayOS: financial_event
        // là nguồn để đối soát ngược với dashboard của từng cổng, gắn sai nhãn thì mọi giao dịch
        // SePay sẽ nằm lẫn trong nhóm PayOS và không tra ra được.
        var paymentProvider = invoice.getPaymentProvider();

        if (invoice.getSourceType() == InvoiceSourceType.SUBSCRIPTION_REQUEST) {
            var savedSubscription = approveSubscriptionRequest(invoice.getSourceId(), paymentProvider, now);
            invoice.setSubscriptionId(savedSubscription.getId());
        } else if (invoice.getSourceType() == InvoiceSourceType.TOKEN_PURCHASE) {
            finalizeTokenPurchase(invoice.getSourceId(), invoice.getSubscriptionId(), paymentProvider, now);
        } else if (invoice.getSourceType() == InvoiceSourceType.SUBSCRIPTION) {
            var savedSubscription = renewSubscription(
                invoice.getSourceId(), invoice.getResolvedPlanId(), paymentProvider, now);
            invoice.setSubscriptionId(savedSubscription.getId());
        }

        invoiceRepository.save(invoice);

        publishInvoicePaid(invoice);
    }

    /**
     * Ghi outbox trong CÙNG transaction với việc chốt hóa đơn, thay cho một
     * {@code @TransactionalEventListener} chạy sau commit như trước đây.
     *
     * <p>Đường cũ mất thông báo vĩnh viễn nếu tiến trình chết ngay sau commit, và tệ hơn:
     * lỗi ném ra từ listener sau commit sẽ dội ngược thành 500 cho webhook cổng thanh toán,
     * trong khi lần gọi lại lại thoát sớm ở guard {@code status != PENDING} nên không phát
     * lại sự kiện. Outbox row nằm cùng transaction thì không có khe hở đó.
     *
     * <p>Danh sách người nhận được chốt tại đây -- xem javadoc của
     * {@link InvoicePaidPayloadV1} về lý do không resolve ở consumer.
     */
    private void publishInvoicePaid(Invoice invoice) {
        var schoolAdminIds = schoolUserRepository
            .findBySchoolIdWithRole(invoice.getSchoolId(), SCHOOL_ADMIN_ROLE_CODE)
            .stream()
            .map(schoolUser -> schoolUser.getUserId())
            .toList();

        var payload = jsonSerializationPort.toJson(new InvoicePaidPayloadV1(
            schoolAdminIds,
            invoice.getSchoolId(),
            invoice.getSubscriptionId(),
            invoice.getSourceId(),
            invoice.getInvoiceNumber(),
            invoice.getAmount(),
            invoice.getPaidAt(),
            invoice.getSourceType()
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.INVOICE,
            invoice.getId(),
            EventTypeConstant.INVOICE_PAID,
            payload,
            invoice.getPaidAt()
        ));
    }

    private SchoolSubscription approveSubscriptionRequest(UUID requestId, PaymentMethod paymentProvider, Instant now) {
        var request = subscriptionRequestRepository.findById(requestId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu"));
        if (request.getStatus() != RequestStatus.PENDING) {
            return schoolSubscriptionRepository.findActiveBySchoolId(request.getSchoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký đang hoạt động"));
        }

        var plan = subscriptionPlanRepository.findById(request.getRequestedPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // Chụp bucket nào của gói CŨ (nếu có) đang vượt hạn mức trước khi expire nó -- gói mới tạo
        // bên dưới luôn có SubscriptionQuota tinh khôi (usedQuantity=0) nên chắc chắn không khóa,
        // không cần check lại "sau" như checkDebtCapTransition (ConsumeQuotaUseCase).
        var oldSubscriptionId = schoolSubscriptionRepository.findActiveBySchoolId(request.getSchoolId())
            .map(current -> {
                current.setStatus(SubscriptionStatus.EXPIRED);
                schoolSubscriptionRepository.save(current);
                return current.getId();
            })
            .orElse(null);
        var wasOverGrading = oldSubscriptionId != null
            && schoolSubscriptionDebtGuardService.isQuotaOverLimit(oldSubscriptionId, QuotaType.GRADING);
        var wasOverClassTest = oldSubscriptionId != null
            && schoolSubscriptionDebtGuardService.isQuotaOverLimit(oldSubscriptionId, QuotaType.CLASS_TEST);

        var startDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var savedSubscription = schoolSubscriptionRepository.save(new SchoolSubscription(
            request.getSchoolId(),
            plan.getId(),
            startDate,
            startDate.plus(plan.getValidityDays(), ChronoUnit.DAYS),
            SubscriptionStatus.ACTIVE,
            request.getAmount(),
            null,
            now
        ));

        planQuotaRepository.findAllByPlanId(plan.getId()).forEach(planQuota ->
            subscriptionQuotaRepository.save(new SubscriptionQuota(
                savedSubscription.getId(),
                planQuota.getQuotaType(),
                planQuota.getIncludedQuantity(),
                BigDecimal.ZERO
            ))
        );

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedAt(now);
        subscriptionRequestRepository.save(request);

        var eventType = request.getRequestType() == RequestType.UPGRADE
            ? FinancialEventType.SUB_UPGRADED
            : FinancialEventType.SUB_PURCHASED;
        financialEventRepository.save(new FinancialEvent(
            request.getSchoolId(), savedSubscription.getId(), eventType,
            request.getAmount(), "VND", paymentProvider, null, null, now
        ));

        reportDebtClearedIfNeeded(wasOverGrading, savedSubscription, QuotaType.GRADING, now);
        reportDebtClearedIfNeeded(wasOverClassTest, savedSubscription, QuotaType.CLASS_TEST, now);

        return savedSubscription;
    }

    /**
     * Báo SchoolDebtCleared cho ĐÚNG 1 bucket vừa hết nợ (gói mới luôn tinh khôi nên chắc chắn hết
     * nợ nếu bucket đó trước đó có nợ) -- dùng ở cả approveSubscriptionRequest lẫn renewSubscription,
     * 2 nơi tạo subscription mới thay thế subscription cũ.
     */
    private void reportDebtClearedIfNeeded(boolean wasOver, SchoolSubscription newSubscription, QuotaType quotaType, Instant now) {
        if (!wasOver) {
            return;
        }
        subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(newSubscription.getId(), quotaType)
            .ifPresent(quota -> schoolDebtNotificationService.publishSchoolDebtCleared(
                newSubscription.getId(), newSubscription.getSchoolId(), quotaType,
                quota.getTotalAllocated(), quota.getUsedQuantity(), now
            ));
    }

    private SchoolSubscription renewSubscription(
            UUID subscriptionId, UUID resolvedPlanId, PaymentMethod paymentProvider, Instant now) {
        var current = schoolSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (current.getStatus() != SubscriptionStatus.ACTIVE) {
            return schoolSubscriptionRepository.findActiveBySchoolId(current.getSchoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký đang hoạt động"));
        }

        // Dùng ĐÚNG plan đã chốt giá lúc tạo invoice/payment link (CreatePaymentLinkForRenewalUseCase),
        // không resolve lại chuỗi thay thế ở đây — nếu resolve lại, admin đổi replacedByPlanId giữa
        // lúc tạo invoice và lúc cổng thanh toán xác nhận có thể khiến trường bị tính tiền theo 1
        // plan nhưng lại được cấp quota/hạn theo plan khác. resolvedPlanId chỉ null cho invoice cũ
        // tạo trước khi field này tồn tại — fallback resolve lại để không vỡ luồng cũ.
        var plan = resolvedPlanId != null
            ? subscriptionPlanRepository.findById(resolvedPlanId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"))
            : subscriptionPlanResolver.resolveActivePlan(subscriptionPlanRepository.findById(current.getPlanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói")));

        // Chụp bucket nào của gói CŨ đang vượt hạn mức trước khi expire nó -- cùng lý do như
        // approveSubscriptionRequest.
        var wasOverGrading = schoolSubscriptionDebtGuardService.isQuotaOverLimit(current.getId(), QuotaType.GRADING);
        var wasOverClassTest = schoolSubscriptionDebtGuardService.isQuotaOverLimit(current.getId(), QuotaType.CLASS_TEST);

        current.setStatus(SubscriptionStatus.EXPIRED);
        schoolSubscriptionRepository.save(current);

        var startDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var savedSubscription = schoolSubscriptionRepository.save(new SchoolSubscription(
            current.getSchoolId(),
            plan.getId(),
            startDate,
            startDate.plusDays(plan.getValidityDays()),
            SubscriptionStatus.ACTIVE,
            plan.getPricePerYear(),
            null,
            now
        ));

        planQuotaRepository.findAllByPlanId(plan.getId()).forEach(planQuota ->
            subscriptionQuotaRepository.save(new SubscriptionQuota(
                savedSubscription.getId(),
                planQuota.getQuotaType(),
                planQuota.getIncludedQuantity(),
                BigDecimal.ZERO
            ))
        );

        financialEventRepository.save(new FinancialEvent(
            current.getSchoolId(), savedSubscription.getId(), FinancialEventType.SUB_RENEWED,
            plan.getPricePerYear(), "VND", paymentProvider, null, null, now
        ));

        reportDebtClearedIfNeeded(wasOverGrading, savedSubscription, QuotaType.GRADING, now);
        reportDebtClearedIfNeeded(wasOverClassTest, savedSubscription, QuotaType.CLASS_TEST, now);

        return savedSubscription;
    }

    private void finalizeTokenPurchase(
            UUID purchaseId, UUID subscriptionId, PaymentMethod paymentProvider, Instant now) {
        var purchase = tokenPurchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn mua token"));
        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            return;
        }
        purchase.setStatus(PurchaseStatus.PAID);
        tokenPurchaseRepository.save(purchase);

        var wasOverGrading = schoolSubscriptionDebtGuardService.isQuotaOverLimit(subscriptionId, QuotaType.GRADING);
        var wasOverClassTest = schoolSubscriptionDebtGuardService.isQuotaOverLimit(subscriptionId, QuotaType.CLASS_TEST);

        var items = tokenPurchaseItemRepository.findAllByPurchaseId(purchase.getId());
        var subscriptionQuotas = subscriptionQuotaRepository.findAllBySubscriptionId(subscriptionId).stream()
            .collect(Collectors.toMap(quota -> quota.getQuotaType(), Function.identity()));
        for (var item : items) {
            var subscriptionQuota = subscriptionQuotas.get(item.getQuotaType());
            if (subscriptionQuota == null) {
                throw new NotFoundException("Không tìm thấy hạn mức của gói đăng ký");
            }
            subscriptionQuotaRepository.addAllocation(subscriptionQuota.getId(), item.getQuantity());
        }

        var subscription = schoolSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        financialEventRepository.save(new FinancialEvent(
            subscription.getSchoolId(), subscriptionId, FinancialEventType.TOKEN_PURCHASED,
            purchase.getTotalAmount(), "VND", paymentProvider, null, null, now
        ));

        // Báo SchoolDebtCleared cho TỪNG bucket riêng vừa hết nợ (mua thêm token có thể chỉ top-up 1
        // trong 2 bucket) -- khác trước đây chỉ báo gộp khi CẢ 2 bucket cùng hết nợ.
        reportDebtClearedIfStillWithinLimit(wasOverGrading, subscriptionId, subscription.getSchoolId(), QuotaType.GRADING, now);
        reportDebtClearedIfStillWithinLimit(wasOverClassTest, subscriptionId, subscription.getSchoolId(), QuotaType.CLASS_TEST, now);
    }

    private void reportDebtClearedIfStillWithinLimit(
            boolean wasOver, UUID subscriptionId, UUID schoolId, QuotaType quotaType, Instant now) {
        if (!wasOver || schoolSubscriptionDebtGuardService.isQuotaOverLimit(subscriptionId, quotaType)) {
            return;
        }
        subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .ifPresent(quota -> schoolDebtNotificationService.publishSchoolDebtCleared(
                subscriptionId, schoolId, quotaType, quota.getTotalAllocated(), quota.getUsedQuantity(), now
            ));
    }

}