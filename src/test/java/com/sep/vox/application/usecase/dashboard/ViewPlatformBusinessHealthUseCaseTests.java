package com.sep.vox.application.usecase.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.query.dashboard.ViewPlatformBusinessHealthQuery;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformBusinessHealthUseCase;
import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
import com.sep.vox.application.response.input.dashboard.PlatformBusinessHealthResponse;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.repository.OrderRepository;

class ViewPlatformBusinessHealthUseCaseTests {

    private PlatformBusinessHealthQueryRepository queryRepository;
    private OrderRepository orderRepository;
    private ViewPlatformBusinessHealthUseCase useCase;

    private static final Instant FROM = LocalDate.of(2026, 8, 1)
        .atStartOfDay(ZoneConstant.BUSINESS_ZONE).toInstant();
    private static final Instant TO = LocalDate.of(2026, 8, 11)
        .atStartOfDay(ZoneConstant.BUSINESS_ZONE).toInstant();

    @BeforeEach
    void setUp() {
        queryRepository = mock(PlatformBusinessHealthQueryRepository.class);
        orderRepository = mock(OrderRepository.class);
        when(queryRepository.countSchoolSubscriptionHealth(any(), any()))
            .thenReturn(new SchoolSubscriptionHealthDto(96L, 14L, 5L, 2L));
        when(queryRepository.countSchoolsInDebt()).thenReturn(3L);
        when(queryRepository.sumAiCostVnd(any(), any())).thenReturn(new BigDecimal("132100000"));
        when(orderRepository.sumTotalAmountByStatusInRange(any(), any(), any()))
            .thenReturn(new BigDecimal("412500000"));
        useCase = new ViewPlatformBusinessHealthUseCase(queryRepository, orderRepository);
    }

    @Test
    void schoolCountsPassThroughUnchanged() {
        PlatformBusinessHealthResponse result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        assertThat(result.subscribedSchools()).isEqualTo(96L);
        assertThat(result.expiringSoonSchools()).isEqualTo(14L);
        assertThat(result.lapsedSchools()).isEqualTo(5L);
        assertThat(result.suspendedSchools()).isEqualTo(2L);
        assertThat(result.schoolsInDebt()).isEqualTo(3L);
    }

    @Test
    void grossMarginIsRevenueMinusAiCostOverRevenue() {
        PlatformBusinessHealthResponse result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        // (412.500.000 - 132.100.000) / 412.500.000 = 67,975... -> 68,0
        assertThat(result.grossMarginPercent()).isEqualTo(68.0);
        assertThat(result.revenueVnd()).isEqualByComparingTo("412500000");
        assertThat(result.aiCostVnd()).isEqualByComparingTo("132100000");
    }

    /**
     * The boundary of 0 revenue is NOT 0%: forcing it to 0 would draw a period that looks like a
     * loss, when in fact no invoice fell inside the window.
     */
    @Test
    void grossMarginIsNullWhenNoRevenueInWindow() {
        when(orderRepository.sumTotalAmountByStatusInRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        PlatformBusinessHealthResponse result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        assertThat(result.grossMarginPercent()).isNull();
    }

    /**
     * The repository returns null when the period has no orders at all -- the null must not leak
     * out to GraphQL.
     */
    @Test
    void nullRevenueFromRepositoryBecomesZero() {
        when(orderRepository.sumTotalAmountByStatusInRange(any(), any(), any())).thenReturn(null);

        PlatformBusinessHealthResponse result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        assertThat(result.revenueVnd()).isEqualByComparingTo("0");
        assertThat(result.previousRevenueVnd()).isEqualByComparingTo("0");
        assertThat(result.grossMarginPercent()).isNull();
    }

    /**
     * The comparison period must be the SAME LENGTH and sit right before the window. Comparing the
     * first 10 days of a month with the whole previous month makes this month always look like a
     * collapse -- a flaw of the comparison, not of the business.
     */
    @Test
    void previousWindowHasSameLengthAndEndsWhereWindowStarts() {
        useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(orderRepository, org.mockito.Mockito.times(2))
            .sumTotalAmountByStatusInRange(eq(OrderStatus.SUCCESS), fromCaptor.capture(), toCaptor.capture());

        // The second call is the previous period: it ends exactly where the window starts and is
        // exactly as long as the window.
        Instant previousFrom = fromCaptor.getAllValues().get(1);
        Instant previousTo = toCaptor.getAllValues().get(1);
        assertThat(previousTo).isEqualTo(FROM);
        assertThat(java.time.Duration.between(previousFrom, previousTo))
            .isEqualTo(java.time.Duration.between(FROM, TO));
    }

    /**
     * The "about to expire" threshold is a SLIDING 30 days from now, computed as an instant. It is
     * not anchored to a calendar day because the two columns start_date/end_date are timestamptz --
     * comparing instant with instant means no guessing the time zone of the JDBC session.
     */
    @Test
    void expiringSoonThresholdIsThirtyRollingDaysFromNow() {
        useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> throughCaptor = ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(queryRepository)
            .countSchoolSubscriptionHealth(nowCaptor.capture(), throughCaptor.capture());

        assertThat(java.time.Duration.between(nowCaptor.getValue(), throughCaptor.getValue()))
            .isEqualTo(java.time.Duration.ofDays(30));
        assertThat(nowCaptor.getValue()).isCloseTo(Instant.now(), within(1, java.time.temporal.ChronoUnit.MINUTES));
    }

    /**
     * Reversed range: the school count is still correct because it does not depend on the window,
     * while the money part goes to 0.
     */
    @Test
    void invertedRangeKeepsSchoolCountsAndZeroesMoney() {
        PlatformBusinessHealthResponse result = useCase.execute(new ViewPlatformBusinessHealthQuery(TO, FROM));

        assertThat(result.subscribedSchools()).isEqualTo(96L);
        assertThat(result.schoolsInDebt()).isEqualTo(3L);
        assertThat(result.revenueVnd()).isEqualByComparingTo("0");
        assertThat(result.aiCostVnd()).isEqualByComparingTo("0");
        assertThat(result.grossMarginPercent()).isNull();
        assertThat(result.previousGrossMarginPercent()).isNull();
    }

    /**
     * The previous-period boundary is built from the revenue AND the cost of the previous period
     * ITSELF -- both must be read in the back-shifted window, not this period's cost paired with
     * the previous period's revenue.
     */
    @Test
    void previousGrossMarginUsesThePreviousWindowOnBothSides() {
        Instant previousFrom = FROM.minus(java.time.Duration.between(FROM, TO));
        when(orderRepository.sumTotalAmountByStatusInRange(eq(OrderStatus.SUCCESS), eq(previousFrom), eq(FROM)))
            .thenReturn(new BigDecimal("200000000"));
        when(queryRepository.sumAiCostVnd(previousFrom, FROM)).thenReturn(new BigDecimal("56000000"));

        PlatformBusinessHealthResponse result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        // (200,000,000 - 56,000,000) / 200,000,000 = 72.0 -> a difference from 68.0 of -4.0 POINTS.
        assertThat(result.previousGrossMarginPercent()).isEqualTo(72.0);
        assertThat(result.grossMarginPercent()).isEqualTo(68.0);
    }

    /**
     * An empty previous period means there is NO delta to draw. If the client divides by itself, it
     * would produce an -infinity boundary and print it as a huge drop, when there is simply nothing
     * to compare.
     */
    @Test
    void previousGrossMarginIsNullWhenPreviousWindowHadNoRevenue() {
        Instant previousFrom = FROM.minus(java.time.Duration.between(FROM, TO));
        when(orderRepository.sumTotalAmountByStatusInRange(eq(OrderStatus.SUCCESS), eq(previousFrom), eq(FROM)))
            .thenReturn(BigDecimal.ZERO);

        PlatformBusinessHealthResponse result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        assertThat(result.previousGrossMarginPercent()).isNull();
        assertThat(result.grossMarginPercent()).isEqualTo(68.0);
    }
}
