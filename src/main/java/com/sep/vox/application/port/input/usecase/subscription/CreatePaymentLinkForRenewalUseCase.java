package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreatePaymentLinkForRenewalCommand;
import com.sep.vox.application.port.input.service.PaymentPortResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PaymentPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.PaymentLinkDto;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class CreatePaymentLinkForRenewalUseCase implements IUseCase<CreatePaymentLinkForRenewalCommand, PaymentLinkDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentPort paymentPort;
    private final UserContextPort userContextPort;

    public CreatePaymentLinkForRenewalUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            InvoiceRepository invoiceRepository,
            PaymentPortResolver paymentPortResolver,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentPort = paymentPortResolver.resolve(PaymentMethod.PAYOS);
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PaymentLinkDto execute(CreatePaymentLinkForRenewalCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var subscription = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!subscription.getSchoolId().equals(input.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Gói đăng ký không ở trạng thái đang hoạt động");
        }

        var plan = subscriptionPlanRepository.findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));
        var renewalPlan = PlanReplacementResolver.resolveActive(subscriptionPlanRepository, plan);
        // Gói bị đổi (plan cũ đã archive, có gói thay thế) -> bắt buộc trường phải xem trước
        // (PreviewRenewalUseCase) và xác nhận đúng gói đó, chứ không được âm thầm đổi rồi thu tiền.
        if (!renewalPlan.getId().equals(plan.getId()) && !renewalPlan.getId().equals(input.acceptedPlanId())) {
            throw new IllegalStateException(
                "Gói đăng ký đã ngừng cung cấp và được thay thế bằng gói khác. Vui lòng xem trước và xác nhận gói mới trước khi gia hạn.");
        }
        plan = renewalPlan;

        var now = Instant.now();
        // Năm trong số hóa đơn phải lấy từ chính invoiceDate, không phải Year.now(): Year.now() đọc
        // múi giờ của JVM, nên trên server UTC một hóa đơn tạo lúc 06:00 ngày 01/01 giờ VN sẽ mang
        // số INV-2025-... nhưng ngày 2026-01-01.
        var invoiceDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var orderCode = System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
        var invoiceNumber = "INV-" + invoiceDate.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var invoice = invoiceRepository.save(new Invoice(
            invoiceNumber,
            subscription.getSchoolId(),
            subscription.getId(),
            InvoiceSourceType.SUBSCRIPTION,
            subscription.getId(),
            invoiceDate,
            plan.getPricePerYear(),
            InvoiceStatus.PENDING,
            orderCode,
            null,
            null,
            null,
            // Chốt sẵn plan đã báo giá ở đây — lúc settle (PayOSInvoiceSettlementService) phải dùng
            // lại đúng plan này, không resolve lại PlanReplacementResolver lần nữa. Nếu không, admin
            // đổi replacedByPlanId giữa lúc tạo invoice và lúc PayOS xác nhận thanh toán có thể khiến
            // trường bị tính tiền theo 1 gói nhưng lại được cấp quota theo gói khác.
            plan.getId()
        ));

        var result = paymentPort.createPaymentLink(orderCode, plan.getPricePerYear(), "VOX-" + orderCode);

        invoice.setPaymentLinkId(result.paymentLinkId());
        invoice.setCheckoutUrl(result.checkoutUrl());
        var savedInvoice = invoiceRepository.save(invoice);

        return new PaymentLinkDto(savedInvoice.getId(), orderCode, result.paymentLinkId(), result.checkoutUrl());
    }
}
