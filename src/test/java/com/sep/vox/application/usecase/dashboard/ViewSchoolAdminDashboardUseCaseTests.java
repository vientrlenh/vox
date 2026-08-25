package com.sep.vox.application.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.dashboard.ViewSchoolAdminDashboardUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.invoice.Invoice;
import com.sep.vox.domain.model.invoice.InvoiceSourceType;
import com.sep.vox.domain.model.invoice.InvoiceStatus;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;

class ViewSchoolAdminDashboardUseCaseTests {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();

    private UserContextPort userContextPort;
    private ExamRepository examRepository;
    private ExamResultAppealRepository examResultAppealRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SubscriptionQuotaRepository subscriptionQuotaRepository;
    private SubscriptionPlanRepository subscriptionPlanRepository;
    private InvoiceRepository invoiceRepository;
    private ViewSchoolAdminDashboardUseCase useCase;

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        examRepository = mock(ExamRepository.class);
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SubscriptionQuotaRepository.class);
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        useCase = new ViewSchoolAdminDashboardUseCase(
            userContextPort, examRepository, examResultAppealRepository,
            schoolSubscriptionRepository, subscriptionQuotaRepository, subscriptionPlanRepository, invoiceRepository
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(UUID.randomUUID());
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(examRepository.findAccessible(
            any(), any(), any(Boolean.class), any(Boolean.class), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)
        )).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        when(examResultAppealRepository.countBySchoolIdAndStatusIn(any(), any())).thenReturn(0L);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.empty());
        when(subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(any(), any())).thenReturn(Optional.empty());

        var subscription = new SchoolSubscription(
            subscriptionId, schoolId, UUID.randomUUID(), LocalDate.now(), null,
            SubscriptionStatus.ACTIVE, BigDecimal.ZERO, null, Instant.now(), null, null, null, null
        );
        when(schoolSubscriptionRepository.findAllBySchoolId(schoolId)).thenReturn(List.of(subscription));
    }

    private Invoice paidInvoice(BigDecimal amount, Instant paidAt) {
        return new Invoice(
            UUID.randomUUID(), "INV-" + UUID.randomUUID(), schoolId, subscriptionId,
            InvoiceSourceType.SUBSCRIPTION, UUID.randomUUID(), LocalDate.now(), amount,
            InvoiceStatus.PAID, null, null, null, null, paidAt, null
        );
    }

    @Test
    void should_bucket_paid_invoices_into_their_payment_month_and_zero_fill_the_rest() {
        var now = Instant.now();
        var twoMonthsAgo = now.minusSeconds(60L * 60 * 24 * 62);

        when(invoiceRepository.findAllBySubscriptionIdIn(List.of(subscriptionId))).thenReturn(List.of(
            paidInvoice(new BigDecimal("100000"), now),
            paidInvoice(new BigDecimal("50000"), now),
            paidInvoice(new BigDecimal("30000"), twoMonthsAgo)
        ));

        var result = useCase.execute(null);

        assertThat(result.revenue()).isEqualByComparingTo("180000");
        assertThat(result.monthlySpending()).hasSize(12);

        var currentMonth = YearMonth.now(ZoneOffset.UTC).toString();
        var twoMonthsAgoMonth = YearMonth.from(twoMonthsAgo.atZone(ZoneOffset.UTC)).toString();

        var currentMonthTotal = result.monthlySpending().stream()
            .filter(m -> m.month().equals(currentMonth))
            .findFirst()
            .orElseThrow();
        assertThat(currentMonthTotal.amount()).isEqualByComparingTo("150000");

        var pastMonthTotal = result.monthlySpending().stream()
            .filter(m -> m.month().equals(twoMonthsAgoMonth))
            .findFirst()
            .orElseThrow();
        assertThat(pastMonthTotal.amount()).isEqualByComparingTo("30000");

        // every other bucketed month with no invoices stays zero
        var untouchedMonths = result.monthlySpending().stream()
            .filter(m -> !m.month().equals(currentMonth) && !m.month().equals(twoMonthsAgoMonth))
            .toList();
        assertThat(untouchedMonths).allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void should_ignore_unpaid_invoices_and_invoices_outside_paid_at() {
        var unpaidInvoice = new Invoice(
            UUID.randomUUID(), "INV-UNPAID", schoolId, subscriptionId,
            InvoiceSourceType.SUBSCRIPTION, UUID.randomUUID(), LocalDate.now(), new BigDecimal("999999"),
            InvoiceStatus.PENDING, null, null, null, null, null, null
        );
        when(invoiceRepository.findAllBySubscriptionIdIn(List.of(subscriptionId))).thenReturn(List.of(unpaidInvoice));

        var result = useCase.execute(null);

        assertThat(result.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.monthlySpending()).allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }
}
