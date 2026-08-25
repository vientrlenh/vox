package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sep.vox.domain.model.subscription.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BuyTokensCommand;
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.domain.mapper.TokenPurchaseDtoMapper;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

@Service
public class BuyTokensUseCase implements IUseCase<BuyTokensCommand, TokenPurchaseDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final QuotaPricingPort quotaPricingService;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final TokenPurchaseRepository tokenPurchaseRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final FinancialEventRepository financialEventRepository;
    private final UserContextPort userContextPort;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;
    private final SchoolDebtNotificationService schoolDebtNotificationService;

    public BuyTokensUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            QuotaPricingPort quotaPricingService,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            TokenPurchaseRepository tokenPurchaseRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            InvoiceRepository invoiceRepository,
            FinancialEventRepository financialEventRepository,
            UserContextPort userContextPort,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService,
            SchoolDebtNotificationService schoolDebtNotificationService) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.quotaPricingService = quotaPricingService;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.tokenPurchaseRepository = tokenPurchaseRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.financialEventRepository = financialEventRepository;
        this.userContextPort = userContextPort;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
    }

    @Override
    @Transactional
    public TokenPurchaseDto execute(BuyTokensCommand input) {
        var command = normalize(input);
        if (!userContextPort.isSystemAdmin() && !command.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        // Use case này cấp quota NGAY và ghi hóa đơn PAID mà không có cổng nào xác nhận tiền, nên chỉ
        // chấp nhận phương thức thu ngoài hệ thống. Nếu cho gắn nhãn PAYOS/SEPAY ở đây, financial_event
        // sẽ có dòng payment_method='PAYOS' kèm actor_id và provider_order_ref=NULL — không tra ngược
        // được sang dashboard cổng, mà lại nằm lẫn giữa các giao dịch cổng thật trong báo cáo.
        var paymentMethod = PaymentMethod.resolve(command.paymentMethod());
        if (paymentMethod.isOnlineGateway()) {
            throw new IllegalArgumentException(
                "Phương thức " + paymentMethod + " phải thanh toán qua link. "
                    + "Hãy dùng POST /schools/{schoolId}/token-purchases/payment-link.");
        }

        var subscription = schoolSubscriptionRepository.findById(command.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!subscription.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Gói đăng ký không ở trạng thái đang hoạt động");
        }

        var plan = subscriptionPlanRepository.findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));
        var planQuotas = planQuotaRepository.findAllByPlanId(subscription.getPlanId());
        // Tính theo tỷ giá USD->VND hiện tại (snapshot mới nhất do ExchangeRateRefreshJob cập nhật
        // hằng ngày), không dùng planQuota.getTokenUnitPrice() đã đóng băng lúc gói còn DRAFT -- xem
        // QuotaPricingService.tokenUnitPriceFor.
        var tokenUnitPrice = quotaPricingService.tokenUnitPriceFor(plan.getServiceFeeRatio());
        var subscriptionQuotas = subscriptionQuotaRepository.findAllBySubscriptionId(subscription.getId()).stream()
            .collect(Collectors.toMap(quota -> quota.getQuotaType(), Function.identity()));
        var now = Instant.now();
        var total = BigDecimal.ZERO;

        var purchase = tokenPurchaseRepository.save(new TokenPurchase(subscription.getId(), BigDecimal.ZERO, PurchaseStatus.PAID, now));

        // Chụp bucket nào đang vượt hạn mức TRƯỚC khi top-up -- báo SchoolDebtCleared riêng cho từng
        // bucket vừa hết nợ (mua thêm có thể chỉ chọn 1 trong 2 loại quota).
        var wasOverGrading = schoolSubscriptionDebtGuardService.isQuotaOverLimit(subscription.getId(), QuotaType.GRADING);
        var wasOverClassTest = schoolSubscriptionDebtGuardService.isQuotaOverLimit(subscription.getId(), QuotaType.CLASS_TEST);

        for (var item : command.items()) {
            planQuotas.stream()
                .filter(pq -> pq.getQuotaType() == item.quotaType())
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn giá cho loại quota này"));
            var subtotal = tokenUnitPrice.multiply(item.quantity());
            total = total.add(subtotal);

            tokenPurchaseItemRepository.save(new TokenPurchaseItem(
                purchase.getId(), item.quotaType(), item.quantity(), tokenUnitPrice, subtotal
            ));

            var subscriptionQuota = subscriptionQuotas.get(item.quotaType());
            if (subscriptionQuota == null) {
                throw new NotFoundException("Không tìm thấy hạn mức của gói đăng ký");
            }
            subscriptionQuotaRepository.addAllocation(subscriptionQuota.getId(), item.quantity());
        }

        purchase.setTotalAmount(total);
        var savedPurchase = tokenPurchaseRepository.save(purchase);
        // Năm trong số hóa đơn phải lấy từ chính invoiceDate, không phải Year.now(): Year.now() đọc
        // múi giờ của JVM, nên trên server UTC một hóa đơn tạo lúc 06:00 ngày 01/01 giờ VN sẽ mang
        // số INV-2025-... nhưng ngày 2026-01-01.
        var invoiceDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        invoiceRepository.save(new Invoice(
            // TODO: add a DB sequence about the invoice sequence
            "INV-" + invoiceDate.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            input.schoolId(),
            subscription.getId(),
            InvoiceSourceType.TOKEN_PURCHASE,
            savedPurchase.getId(),
            invoiceDate,
            total,
            InvoiceStatus.PAID,
            // Guard ở đầu use case đảm bảo đây luôn là phương thức thu ngoài hệ thống, nên không có
            // mã đơn phía cổng lẫn checkoutUrl.
            paymentMethod,
            null,
            null,
            null,
            now,
            null,
            userContextPort.getCurrentAuthenticatedUserId()
        ));

        financialEventRepository.save(new FinancialEvent(
            input.schoolId(), subscription.getId(), FinancialEventType.TOKEN_PURCHASED,
            total, "VND", paymentMethod, userContextPort.getCurrentAuthenticatedUserId(), null, now
        ));

        reportDebtClearedIfStillWithinLimit(wasOverGrading, subscription.getId(), input.schoolId(), QuotaType.GRADING, now);
        reportDebtClearedIfStillWithinLimit(wasOverClassTest, subscription.getId(), input.schoolId(), QuotaType.CLASS_TEST, now);

        return TokenPurchaseDtoMapper.toDto(savedPurchase, tokenPurchaseItemRepository.findAllByPurchaseId(savedPurchase.getId()));
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

    private BuyTokensCommand normalize(BuyTokensCommand input) {
        return new BuyTokensCommand(
            input.schoolId(),
            input.subscriptionId(),
            input.items(),
            StringNormalization.normalizeCode(input.paymentMethod())
        );
    }
}
