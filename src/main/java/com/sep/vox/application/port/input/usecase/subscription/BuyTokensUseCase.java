package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BuyTokensCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.domain.mapper.TokenPurchaseDtoMapper;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.PurchaseStatus;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.domain.model.subscription.TokenPurchaseItem;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

@Service
public class BuyTokensUseCase implements IUseCase<BuyTokensCommand, TokenPurchaseDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final TokenPurchaseRepository tokenPurchaseRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final FinancialEventRepository financialEventRepository;
    private final UserContextPort userContextPort;

    public BuyTokensUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PlanQuotaRepository planQuotaRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            TokenPurchaseRepository tokenPurchaseRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            InvoiceRepository invoiceRepository,
            FinancialEventRepository financialEventRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.tokenPurchaseRepository = tokenPurchaseRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.financialEventRepository = financialEventRepository;
        this.userContextPort = userContextPort;
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

        var planQuotas = planQuotaRepository.findAllByPlanId(subscription.getPlanId());
        var subscriptionQuotas = subscriptionQuotaRepository.findAllBySubscriptionId(subscription.getId()).stream()
            .collect(Collectors.toMap(quota -> quota.getQuotaType(), Function.identity()));
        var now = Instant.now();
        var total = BigDecimal.ZERO;

        var purchase = tokenPurchaseRepository.save(new TokenPurchase(subscription.getId(), BigDecimal.ZERO, PurchaseStatus.PAID, now));

        for (var item : command.items()) {
            var planQuota = planQuotas.stream()
                .filter(pq -> pq.getQuotaType() == item.quotaType())
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn giá cho loại quota này"));
            var subtotal = planQuota.getTokenUnitPrice().multiply(item.quantity());
            total = total.add(subtotal);

            tokenPurchaseItemRepository.save(new TokenPurchaseItem(
                purchase.getId(), item.quotaType(), item.quantity(), planQuota.getTokenUnitPrice(), subtotal
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
            null
        ));

        financialEventRepository.save(new FinancialEvent(
            input.schoolId(), subscription.getId(), FinancialEventType.TOKEN_PURCHASED,
            total, "VND", paymentMethod, userContextPort.getCurrentAuthenticatedUserId(), null, now
        ));

        return TokenPurchaseDtoMapper.toDto(savedPurchase, tokenPurchaseItemRepository.findAllByPurchaseId(savedPurchase.getId()));
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
