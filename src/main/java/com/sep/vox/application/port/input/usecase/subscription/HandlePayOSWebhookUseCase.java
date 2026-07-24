package com.sep.vox.application.port.input.usecase.subscription;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PayOSPort;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
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

import vn.payos.exception.WebhookException;

// Internal service-to-service use case (webhook callback từ PayOS, xác thực bằng chữ ký thay vì JWT) —
// không dùng UserContextPort/ApproveRequestUseCase vì luồng này không có người dùng đăng nhập.
@Service
public class HandlePayOSWebhookUseCase implements IUseCase<Object, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlePayOSWebhookUseCase.class);
    private static final String SUCCESS_CODE = "00";

    private final PayOSPort payOSPort;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final TokenPurchaseRepository tokenPurchaseRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final FinancialEventRepository financialEventRepository;

    public HandlePayOSWebhookUseCase(
            PayOSPort payOSPort,
            InvoiceRepository invoiceRepository,
            SubscriptionRequestRepository subscriptionRequestRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            TokenPurchaseRepository tokenPurchaseRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            FinancialEventRepository financialEventRepository) {
        this.payOSPort = payOSPort;
        this.invoiceRepository = invoiceRepository;
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.tokenPurchaseRepository = tokenPurchaseRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.financialEventRepository = financialEventRepository;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public Void execute(Object rawWebhookBody) {
        var body = (Map<String, Object>) rawWebhookBody;
        var data = (Map<String, Object>) body.get("data");
        var signature = (String) body.get("signature");

        if (data == null || signature == null || !payOSPort.verifyWebhookSignature(data, signature)) {
            throw new WebhookException("Invalid signature");
        }

        var orderCode = ((Number) data.get("orderCode")).longValue();
        var code = (String) data.get("code");

        var invoiceOpt = invoiceRepository.findByPayosOrderCode(orderCode);
        if (invoiceOpt.isEmpty()) {
            // Chữ ký hợp lệ nhưng không khớp đơn hàng nào (vd: payload test cố định của PayOS) —
            // vẫn trả 200 để tránh PayOS hiểu lầm webhook lỗi rồi retry liên tục.
            LOGGER.warn("Nhận webhook PayOS hợp lệ nhưng không tìm thấy invoice cho orderCode={}", orderCode);
            return null;
        }
        var invoice = invoiceOpt.get();

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            return null; // webhook có thể được PayOS gọi lại nhiều lần, đã xử lý trước đó
        }

        var now = OffsetDateTime.now();
        if (!SUCCESS_CODE.equals(code)) {
            invoice.setStatus(InvoiceStatus.FAILED);
            invoiceRepository.save(invoice);
            return null;
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(now);

        if (invoice.getSourceType() == InvoiceSourceType.SUBSCRIPTION_REQUEST) {
            var savedSubscription = approveSubscriptionRequest(invoice.getSourceId(), now);
            invoice.setSubscriptionId(savedSubscription.getId());
        } else if (invoice.getSourceType() == InvoiceSourceType.TOKEN_PURCHASE) {
            finalizeTokenPurchase(invoice.getSourceId(), invoice.getSubscriptionId(), now);
        } else if (invoice.getSourceType() == InvoiceSourceType.SUBSCRIPTION) {
            var savedSubscription = renewSubscription(invoice.getSourceId(), now);
            invoice.setSubscriptionId(savedSubscription.getId());
        }

        invoiceRepository.save(invoice);
        return null;
    }

    private SchoolSubscription approveSubscriptionRequest(UUID requestId, OffsetDateTime now) {
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

        var startDate = now.toLocalDate();
        var savedSubscription = schoolSubscriptionRepository.save(new SchoolSubscription(
            request.getSchoolId(),
            plan.getId(),
            startDate,
            startDate.plusDays(plan.getValidityDays()),
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

    private SchoolSubscription renewSubscription(UUID subscriptionId, OffsetDateTime now) {
        var current = schoolSubscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (current.getStatus() != SubscriptionStatus.ACTIVE) {
            return schoolSubscriptionRepository.findActiveBySchoolId(current.getSchoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký đang hoạt động"));
        }

        var plan = subscriptionPlanRepository.findById(current.getPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        current.setStatus(SubscriptionStatus.EXPIRED);
        schoolSubscriptionRepository.save(current);

        var startDate = now.toLocalDate();
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

    private void finalizeTokenPurchase(UUID purchaseId, UUID subscriptionId, OffsetDateTime now) {
        var purchase = tokenPurchaseRepository.findById(purchaseId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn mua token"));
        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            return;
        }
        purchase.setStatus(PurchaseStatus.PAID);
        tokenPurchaseRepository.save(purchase);

        var items = tokenPurchaseItemRepository.findAllByPurchaseId(purchase.getId());
        var subscriptionQuotas = subscriptionQuotaRepository.findAllBySubscriptionId(subscriptionId).stream()
            .collect(Collectors.toMap(SubscriptionQuota::getQuotaType, Function.identity()));
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