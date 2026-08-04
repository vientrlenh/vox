package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.event.InvoicePaidEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.PurchaseStatus;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.RequestType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

// Logic "chốt" kết quả thanh toán 1 invoice (PAID -> approve subscription/renew/finalize token purchase,
// hoặc FAILED), dùng chung cho cả webhook PayOS (HandlePayOSWebhookUseCase) lẫn luồng đồng bộ theo yêu cầu
// (SyncInvoicePaymentStatusUseCase) và job quét định kỳ, để business logic chỉ nằm ở đúng 1 chỗ.
@Service
public class PayOSInvoiceSettlementService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final TokenPurchaseRepository tokenPurchaseRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final FinancialEventRepository financialEventRepository;
    private final EventPublisherPort eventPublisherPort;

    public PayOSInvoiceSettlementService(
            InvoiceRepository invoiceRepository,
            SubscriptionRequestRepository subscriptionRequestRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            TokenPurchaseRepository tokenPurchaseRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            FinancialEventRepository financialEventRepository,
            EventPublisherPort eventPublisherPort) {
        this.invoiceRepository = invoiceRepository;
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.tokenPurchaseRepository = tokenPurchaseRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.financialEventRepository = financialEventRepository;
        this.eventPublisherPort = eventPublisherPort;
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
    // cùng 1 invoice (vd: FE gọi sync-status 2 lần do StrictMode, hoặc sync-status đua với webhook PayOS)
    // đều có thể đọc thấy PENDING trước khi bên kia commit, dẫn tới chốt thanh toán trùng 2 lần.
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

        if (invoice.getSourceType() == InvoiceSourceType.SUBSCRIPTION_REQUEST) {
            var savedSubscription = approveSubscriptionRequest(invoice.getSourceId(), now);
            invoice.setSubscriptionId(savedSubscription.getId());
        } else if (invoice.getSourceType() == InvoiceSourceType.TOKEN_PURCHASE) {
            finalizeTokenPurchase(invoice.getSourceId(), invoice.getSubscriptionId(), now);
        } else if (invoice.getSourceType() == InvoiceSourceType.SUBSCRIPTION) {
            var savedSubscription = renewSubscription(invoice.getSourceId(), invoice.getResolvedPlanId(), now);
            invoice.setSubscriptionId(savedSubscription.getId());
        }

        invoiceRepository.save(invoice);

        eventPublisherPort.publish(new InvoicePaidEvent(
            invoice.getSchoolId(), invoice.getSubscriptionId(), invoice.getSourceId(), invoice.getInvoiceNumber(),
            invoice.getAmount(), invoice.getPaidAt(), invoice.getSourceType()
        ));
    }

    private SchoolSubscription approveSubscriptionRequest(UUID requestId, Instant now) {
        var request = subscriptionRequestRepository.findById(requestId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu"));
        if (request.getStatus() != RequestStatus.PENDING) {
            return schoolSubscriptionRepository.findActiveBySchoolId(request.getSchoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký đang hoạt động"));
        }

        var plan = subscriptionPlanRepository.findById(request.getRequestedPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        schoolSubscriptionRepository.findActiveBySchoolId(request.getSchoolId()).ifPresent(current -> {
            current.setStatus(SubscriptionStatus.EXPIRED);
            schoolSubscriptionRepository.save(current);
        });

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
                0
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
            request.getAmount(), "VND", PaymentMethod.PAYOS, null, null, now
        ));

        return savedSubscription;
    }

    private SchoolSubscription renewSubscription(UUID subscriptionId, UUID resolvedPlanId, Instant now) {
        var current = schoolSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (current.getStatus() != SubscriptionStatus.ACTIVE) {
            return schoolSubscriptionRepository.findActiveBySchoolId(current.getSchoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký đang hoạt động"));
        }

        // Dùng ĐÚNG plan đã chốt giá lúc tạo invoice/payment link (CreatePaymentLinkForRenewalUseCase),
        // không resolve lại PlanReplacementResolver ở đây — nếu resolve lại, admin đổi replacedByPlanId
        // giữa lúc tạo invoice và lúc PayOS xác nhận thanh toán có thể khiến trường bị tính tiền theo 1
        // plan nhưng lại được cấp quota/hạn theo plan khác. resolvedPlanId chỉ null cho invoice cũ tạo
        // trước khi field này tồn tại — fallback resolve lại để không vỡ luồng cũ.
        var plan = resolvedPlanId != null
            ? subscriptionPlanRepository.findById(resolvedPlanId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"))
            : PlanReplacementResolver.resolveActive(subscriptionPlanRepository,
                subscriptionPlanRepository.findById(current.getPlanId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy gói")));

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
                0
            ))
        );

        financialEventRepository.save(new FinancialEvent(
            current.getSchoolId(), savedSubscription.getId(), FinancialEventType.SUB_RENEWED,
            plan.getPricePerYear(), "VND", PaymentMethod.PAYOS, null, null, now
        ));

        return savedSubscription;
    }

    private void finalizeTokenPurchase(UUID purchaseId, UUID subscriptionId, Instant now) {
        var purchase = tokenPurchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn mua token"));
        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            return;
        }
        purchase.setStatus(PurchaseStatus.PAID);
        tokenPurchaseRepository.save(purchase);

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
            purchase.getTotalAmount(), "VND", PaymentMethod.PAYOS, null, null, now
        ));
    }
}