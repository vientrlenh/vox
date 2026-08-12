package com.sep.vox.infrastructure.event.internal.listener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.event.InvoicePaidEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.MailSendingPort;
import com.sep.vox.application.port.output.MailTemplatePort;
import com.sep.vox.application.query.repository.SchoolUserQueryRepository;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;

@Component
public class InvoicePaidEmailListener {

    private static final Map<QuotaType, String> QUOTA_LABELS = Map.of(
        QuotaType.GRADING, "Bài thi cần chấm",
        QuotaType.CLASS_TEST, "Bài kiểm tra trên lớp",
        QuotaType.PRACTICE, "Lượt ôn luyện cá nhân"
    );

    // DateTimeFormatter an toàn dùng chung giữa các thread; DecimalFormat thì không, nên nó được
    // tạo mới trong mỗi lần gọi handle() thay vì giữ làm field static.
    private static final DateTimeFormatter PAID_AT_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(DateMapper.DEFAULT_INPUT_ZONE);
    private static final DateTimeFormatter VALID_UNTIL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SchoolRepository schoolRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final SchoolUserQueryRepository schoolUserQueryRepository;
    private final MailSendingPort mailSendingPort;
    private final MailTemplatePort mailTemplatePort;

    public InvoicePaidEmailListener(
            SchoolRepository schoolRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            SchoolUserQueryRepository schoolUserQueryRepository,
            MailSendingPort mailSendingPort,
            MailTemplatePort mailTemplatePort) {
        this.schoolRepository = schoolRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.schoolUserQueryRepository = schoolUserQueryRepository;
        this.mailSendingPort = mailSendingPort;
        this.mailTemplatePort = mailTemplatePort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InvoicePaidEvent event) throws Exception {
        var school = schoolRepository.findById(event.schoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường"));
        var subscription = schoolSubscriptionRepository.findById(event.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        var plan = subscriptionPlanRepository.findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        String itemTitle;
        String itemsHtml;
        if (event.sourceType() == InvoiceSourceType.TOKEN_PURCHASE) {
            itemTitle = "Mua thêm hạn mức";
            var items = tokenPurchaseItemRepository.findAllByPurchaseId(event.sourceId());
            itemsHtml = buildItemsHtml(items.stream()
                .collect(Collectors.toMap(item -> item.getQuotaType(), item -> item.getQuantity())));
        } else {
            itemTitle = "Gói " + plan.getName();
            var quotas = subscriptionQuotaRepository.findAllBySubscriptionId(subscription.getId());
            itemsHtml = buildItemsHtml(quotas.stream()
                .collect(Collectors.toMap(quota -> quota.getQuotaType(), quota -> quota.getTotalAllocated())));
        }

        var amountFormatter = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.of("vi", "VN")));
        var amountLabel = amountFormatter.format(event.amount()) + " ₫";
        var paidAtLabel = event.paidAt() == null ? "-" : PAID_AT_FORMATTER.format(event.paidAt());
        var validUntilLabel = subscription.getEndDate() == null ? "-" : VALID_UNTIL_FORMATTER.format(subscription.getEndDate());

        var html = mailTemplatePort.renderInvoicePaidEmail(
            school.getName(), event.invoiceNumber(), itemTitle, itemsHtml, amountLabel, paidAtLabel, validUntilLabel);

        var schoolAdmins = schoolUserQueryRepository.findBySchoolIdAndRoleCodes(
            event.schoolId(),
            List.of("SCHOOL_ADMIN"),
            0,
            50
        );
        for (var admin : schoolAdmins.content()) {
            mailSendingPort.sendHtml(admin.email(), "Hóa đơn thanh toán #" + event.invoiceNumber(), html);
        }
    }

    // Quota tính bằng USD chi phí AI (xem V15__quota_unit_to_usd.sql), hiển thị 2 chữ số thập phân.
    private String buildItemsHtml(Map<QuotaType, BigDecimal> quantityByType) {
        var rows = new StringBuilder();
        for (var quotaType : QuotaType.values()) {
            var quantity = quantityByType.get(quotaType);
            if (quantity == null) {
                continue;
            }
            var usdLabel = quantity.setScale(2, RoundingMode.HALF_UP).toPlainString();
            rows.append("<tr><td style=\"padding:6px 20px; color:#475569; font-size:14px;\">")
                .append(QUOTA_LABELS.get(quotaType))
                .append("</td><td style=\"padding:6px 20px; text-align:right; font-weight:700; font-size:14px; color:#1e293b;\">")
                .append(usdLabel)
                .append(" USD</td></tr>");
        }
        return rows.toString();
    }
}
