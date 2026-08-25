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

import com.sep.vox.application.port.input.usecase.dashboard.ViewSystemAdminDashboardUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceSourceType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

class ViewSystemAdminDashboardUseCaseTests {

    private SchoolRepository schoolRepository;
    private RegisterFormRepository registerFormRepository;
    private InvoiceRepository invoiceRepository;
    private RoleRepository roleRepository;
    private UserRoleRepository userRoleRepository;
    private FrameworkRepository frameworkRepository;
    private RubricRepository rubricRepository;
    private ViewSystemAdminDashboardUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolRepository = mock(SchoolRepository.class);
        registerFormRepository = mock(RegisterFormRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        roleRepository = mock(RoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        frameworkRepository = mock(FrameworkRepository.class);
        rubricRepository = mock(RubricRepository.class);
        useCase = new ViewSystemAdminDashboardUseCase(
            schoolRepository, registerFormRepository, invoiceRepository,
            roleRepository, userRoleRepository, frameworkRepository, rubricRepository
        );

        when(schoolRepository.countAll()).thenReturn(0L);
        when(schoolRepository.countByIsActiveTrue()).thenReturn(0L);
        when(registerFormRepository.countByStatus(any())).thenReturn(0L);
        when(registerFormRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(roleRepository.findByCode(any())).thenReturn(Optional.empty());
        when(frameworkRepository.findAllActive(1, 1)).thenReturn(new PageResult<>(List.of(), 1, 1, 0, 0));
        when(rubricRepository.findAllByOwnerType(any(), any(Integer.class), any(Integer.class)))
            .thenReturn(new PageResult<>(List.of(), 1, 1, 0, 0));
        when(invoiceRepository.sumAmountByStatus(InvoiceStatus.PAID)).thenReturn(BigDecimal.ZERO);
    }

    private Invoice paidInvoice(BigDecimal amount, Instant paidAt) {
        return new Invoice(
            UUID.randomUUID(), "INV-" + UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            InvoiceSourceType.SUBSCRIPTION, UUID.randomUUID(), LocalDate.now(), amount,
            InvoiceStatus.PAID, null, null, null, null, paidAt, null, null
        );
    }

    @Test
    void should_bucket_paid_invoices_into_24_months_zero_filled() {
        var now = Instant.now();
        var thirteenMonthsAgo = now.minusSeconds(60L * 60 * 24 * 395);

        when(invoiceRepository.findAllByStatus(InvoiceStatus.PAID)).thenReturn(List.of(
            paidInvoice(new BigDecimal("200000"), now),
            paidInvoice(new BigDecimal("300000"), thirteenMonthsAgo)
        ));

        var result = useCase.execute(null);

        assertThat(result.monthlyRevenue()).hasSize(24);

        var currentMonth = YearMonth.now(ZoneOffset.UTC).toString();
        var pastMonth = YearMonth.from(thirteenMonthsAgo.atZone(ZoneOffset.UTC)).toString();

        assertThat(result.monthlyRevenue().stream().filter(m -> m.month().equals(currentMonth)).findFirst().orElseThrow().amount())
            .isEqualByComparingTo("200000");
        assertThat(result.monthlyRevenue().stream().filter(m -> m.month().equals(pastMonth)).findFirst().orElseThrow().amount())
            .isEqualByComparingTo("300000");

        var untouched = result.monthlyRevenue().stream()
            .filter(m -> !m.month().equals(currentMonth) && !m.month().equals(pastMonth))
            .toList();
        assertThat(untouched).allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void should_ignore_invoices_paid_more_than_24_months_ago() {
        var over24MonthsAgo = Instant.now().minusSeconds(60L * 60 * 24 * 800);
        when(invoiceRepository.findAllByStatus(InvoiceStatus.PAID))
            .thenReturn(List.of(paidInvoice(new BigDecimal("999999"), over24MonthsAgo)));

        var result = useCase.execute(null);

        assertThat(result.monthlyRevenue()).allSatisfy(m -> assertThat(m.amount()).isEqualByComparingTo(BigDecimal.ZERO));
    }
}
