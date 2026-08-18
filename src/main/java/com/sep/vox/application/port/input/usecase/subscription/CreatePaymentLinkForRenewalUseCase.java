package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreatePaymentLinkForRenewalCommand;
import com.sep.vox.application.port.input.service.InvoiceSettlementService;
import com.sep.vox.application.port.input.service.PaymentProcessResolver;
import com.sep.vox.application.port.input.service.RenewalProrationService;
import com.sep.vox.application.port.input.service.SubscriptionPlanResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.CheckoutAction;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
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
    private final PaymentProcessResolver paymentProcessResolver;
    private final SubscriptionPlanResolver subscriptionPlanResolver;
    private final RenewalProrationService renewalProrationService;
    private final InvoiceSettlementService invoiceSettlementService;
    private final UserContextPort userContextPort;

    public CreatePaymentLinkForRenewalUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            InvoiceRepository invoiceRepository,
            PaymentProcessResolver paymentPortResolver,
            SubscriptionPlanResolver subscriptionPlanResolver,
            RenewalProrationService renewalProrationService,
            InvoiceSettlementService invoiceSettlementService,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentProcessResolver = paymentPortResolver;
        this.subscriptionPlanResolver = subscriptionPlanResolver;
        this.renewalProrationService = renewalProrationService;
        this.invoiceSettlementService = invoiceSettlementService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public PaymentLinkDto execute(CreatePaymentLinkForRenewalCommand input) {
        var command = normalize(input);

        if (!userContextPort.isSystemAdmin() && !command.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
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
        var renewalPlan = subscriptionPlanResolver.resolveActivePlan(plan);
        var planChanged = !renewalPlan.getId().equals(plan.getId());
        // Gói bị đổi (plan cũ đã archive, có gói thay thế) -> bắt buộc trường phải xem trước
        // (PreviewRenewalUseCase) và xác nhận đúng gói đó, chứ không được âm thầm đổi rồi thu tiền.
        if (planChanged && !renewalPlan.getId().equals(input.acceptedPlanId())) {
            throw new IllegalStateException(
                "Gói đăng ký đã ngừng cung cấp và được thay thế bằng gói khác. Vui lòng xem trước và xác nhận gói mới trước khi gia hạn.");
        }
        plan = renewalPlan;

        var now = Instant.now();
        // Năm trong số hóa đơn phải lấy từ chính invoiceDate, không phải Year.now(): Year.now() đọc
        // múi giờ của JVM, nên trên server UTC một hóa đơn tạo lúc 06:00 ngày 01/01 giờ VN sẽ mang
        // số INV-2025-... nhưng ngày 2026-01-01.
        var invoiceDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);

        // Bị ép đổi gói giữa chu kỳ (System Admin archive gói đang dùng) -> bù giá trị những ngày
        // gói cũ chưa dùng vào hóa đơn gói mới, khớp CHÍNH XÁC con số trường đã thấy ở
        // PreviewRenewalUseCase (cùng RenewalProrationService, cùng công thức).
        var unusedCredit = renewalProrationService.calculateUnusedCredit(
            subscription, plan.getPricePerYear(), planChanged, invoiceDate);
        var amountDue = plan.getPricePerYear().subtract(unusedCredit);
        var invoiceNumber = "INV-" + invoiceDate.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if (amountDue.compareTo(BigDecimal.ZERO) == 0) {
            // Bù trừ đã chi trả đủ 100% giá gói mới -- không còn gì để thu, khỏi cần cổng thanh toán
            // nào (PayOS/SePay đều từ chối tạo link 0đ). Tái dùng nguyên settle() (khoá invoice FOR
            // UPDATE, set PAID, gọi renewSubscription(), publish outbox InvoicePaid/email) -- đúng 1
            // chỗ chốt renewal, không viết lại logic ở đây.
            var freeInvoice = invoiceRepository.save(new Invoice(
                invoiceNumber,
                subscription.getSchoolId(),
                subscription.getId(),
                InvoiceSourceType.SUBSCRIPTION,
                subscription.getId(),
                invoiceDate,
                amountDue,
                InvoiceStatus.PENDING,
                PaymentMethod.MANUAL,
                null,
                null,
                null,
                null,
                plan.getId()
            ));
            invoiceSettlementService.settle(freeInvoice, true);
            return new PaymentLinkDto(freeInvoice.getId(), null, CheckoutAction.NONE.name(), "", null, null);
        }

        var paymentMethod = PaymentMethod.resolveOnlineGateway(command.paymentMethod());
        // Mã đơn do adapter của cổng quyết định, không phải use case: PayOS đòi orderCode dạng số,
        // còn SePay dùng thẳng invoiceNumber dạng chuỗi.
        var paymentProcessPort = paymentProcessResolver.resolve(paymentMethod);
        var orderRef = paymentProcessPort.newOrderRef(invoiceNumber);

        var invoice = invoiceRepository.save(new Invoice(
            invoiceNumber,
            subscription.getSchoolId(),
            subscription.getId(),
            InvoiceSourceType.SUBSCRIPTION,
            subscription.getId(),
            invoiceDate,
            amountDue,
            InvoiceStatus.PENDING,
            paymentMethod,
            orderRef,
            null,
            null,
            null,
            plan.getId()
        ));

        var result = paymentProcessPort.createPaymentLink(new CreatePaymentLinkCommand(
            orderRef, amountDue, "VOX-" + orderRef));

        invoice.setPaymentLinkId(result.paymentLinkId());
        invoice.setCheckoutUrl(result.actionUrl()); 
        var savedInvoice = invoiceRepository.save(invoice);

        return new PaymentLinkDto(
            savedInvoice.getId(), orderRef, result.action().name(), result.actionUrl(), result.paymentLinkId(), result.fields());
    }
    
    private CreatePaymentLinkForRenewalCommand normalize(CreatePaymentLinkForRenewalCommand input) {
        return new CreatePaymentLinkForRenewalCommand(
            input.schoolId(), 
            input.subscriptionId(), 
            input.acceptedPlanId(), 
            StringNormalization.normalizeCode(input.paymentMethod())
        );
    }
}
