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

import com.sep.vox.application.port.input.query.ViewPlatformBusinessHealthQuery;
import com.sep.vox.application.port.input.usecase.dashboard.ViewPlatformBusinessHealthUseCase;
import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
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
        var result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        assertThat(result.subscribedSchools()).isEqualTo(96L);
        assertThat(result.expiringSoonSchools()).isEqualTo(14L);
        assertThat(result.lapsedSchools()).isEqualTo(5L);
        assertThat(result.suspendedSchools()).isEqualTo(2L);
        assertThat(result.schoolsInDebt()).isEqualTo(3L);
    }

    @Test
    void grossMarginIsRevenueMinusAiCostOverRevenue() {
        var result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        // (412.500.000 - 132.100.000) / 412.500.000 = 67,975... -> 68,0
        assertThat(result.grossMarginPercent()).isEqualTo(68.0);
        assertThat(result.revenueVnd()).isEqualByComparingTo("412500000");
        assertThat(result.aiCostVnd()).isEqualByComparingTo("132100000");
    }

    /**
     * Biên của doanh thu 0 KHÔNG phải 0%: ép thành 0 sẽ vẽ ra một kỳ trông như đang lỗ, trong khi
     * thật ra chỉ là chưa có hóa đơn nào rơi vào cửa sổ.
     */
    @Test
    void grossMarginIsNullWhenNoRevenueInWindow() {
        when(orderRepository.sumTotalAmountByStatusInRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        var result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        assertThat(result.grossMarginPercent()).isNull();
    }

    /** Repository trả null khi kỳ không có đơn nào — không được để null rơi ra tới GraphQL. */
    @Test
    void nullRevenueFromRepositoryBecomesZero() {
        when(orderRepository.sumTotalAmountByStatusInRange(any(), any(), any())).thenReturn(null);

        var result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        assertThat(result.revenueVnd()).isEqualByComparingTo("0");
        assertThat(result.previousRevenueVnd()).isEqualByComparingTo("0");
        assertThat(result.grossMarginPercent()).isNull();
    }

    /**
     * Kỳ so sánh phải CÙNG ĐỘ DÀI và nằm ngay trước cửa sổ. Đem 10 ngày đầu tháng so với trọn tháng
     * trước thì tháng này luôn trông như sụp đổ — lỗi của phép so, không phải của việc kinh doanh.
     */
    @Test
    void previousWindowHasSameLengthAndEndsWhereWindowStarts() {
        useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        var fromCaptor = ArgumentCaptor.forClass(Instant.class);
        var toCaptor = ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(orderRepository, org.mockito.Mockito.times(2))
            .sumTotalAmountByStatusInRange(eq(OrderStatus.SUCCESS), fromCaptor.capture(), toCaptor.capture());

        // Lần gọi thứ hai là kỳ trước: kết thúc đúng chỗ cửa sổ bắt đầu, dài đúng bằng cửa sổ.
        var previousFrom = fromCaptor.getAllValues().get(1);
        var previousTo = toCaptor.getAllValues().get(1);
        assertThat(previousTo).isEqualTo(FROM);
        assertThat(java.time.Duration.between(previousFrom, previousTo))
            .isEqualTo(java.time.Duration.between(FROM, TO));
    }

    /**
     * Ngưỡng "sắp hết hạn" là 30 ngày TRƯỢT kể từ bây giờ, tính bằng instant. Không neo vào ngày
     * lịch vì hai cột start_date/end_date là timestamptz — so instant với instant thì không phải
     * đoán múi giờ của session JDBC.
     */
    @Test
    void expiringSoonThresholdIsThirtyRollingDaysFromNow() {
        useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        var nowCaptor = ArgumentCaptor.forClass(Instant.class);
        var throughCaptor = ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(queryRepository)
            .countSchoolSubscriptionHealth(nowCaptor.capture(), throughCaptor.capture());

        assertThat(java.time.Duration.between(nowCaptor.getValue(), throughCaptor.getValue()))
            .isEqualTo(java.time.Duration.ofDays(30));
        assertThat(nowCaptor.getValue()).isCloseTo(Instant.now(), within(1, java.time.temporal.ChronoUnit.MINUTES));
    }

    /** Khoảng ngược: phần đếm trường vẫn đúng vì nó không phụ thuộc cửa sổ, phần tiền về 0. */
    @Test
    void invertedRangeKeepsSchoolCountsAndZeroesMoney() {
        var result = useCase.execute(new ViewPlatformBusinessHealthQuery(TO, FROM));

        assertThat(result.subscribedSchools()).isEqualTo(96L);
        assertThat(result.schoolsInDebt()).isEqualTo(3L);
        assertThat(result.revenueVnd()).isEqualByComparingTo("0");
        assertThat(result.aiCostVnd()).isEqualByComparingTo("0");
        assertThat(result.grossMarginPercent()).isNull();
        assertThat(result.previousGrossMarginPercent()).isNull();
    }

    /**
     * Biên kỳ trước dựng từ doanh thu VÀ giá vốn của CHÍNH kỳ trước — cả hai đều phải đọc ở cửa sổ
     * lùi, không phải lấy giá vốn kỳ này ghép với doanh thu kỳ trước.
     */
    @Test
    void previousGrossMarginUsesThePreviousWindowOnBothSides() {
        var previousFrom = FROM.minus(java.time.Duration.between(FROM, TO));
        when(orderRepository.sumTotalAmountByStatusInRange(eq(OrderStatus.SUCCESS), eq(previousFrom), eq(FROM)))
            .thenReturn(new BigDecimal("200000000"));
        when(queryRepository.sumAiCostVnd(previousFrom, FROM)).thenReturn(new BigDecimal("56000000"));

        var result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        // (200.000.000 - 56.000.000) / 200.000.000 = 72,0 -> chênh so với 68,0 là -4,0 ĐIỂM.
        assertThat(result.previousGrossMarginPercent()).isEqualTo(72.0);
        assertThat(result.grossMarginPercent()).isEqualTo(68.0);
    }

    /**
     * Kỳ trước trống thì KHÔNG có mức chênh để vẽ. Để client tự chia sẽ dựng ra một biên -∞ rồi in
     * thành cú sụt khổng lồ, trong khi thật ra chỉ là chưa có gì để so.
     */
    @Test
    void previousGrossMarginIsNullWhenPreviousWindowHadNoRevenue() {
        var previousFrom = FROM.minus(java.time.Duration.between(FROM, TO));
        when(orderRepository.sumTotalAmountByStatusInRange(eq(OrderStatus.SUCCESS), eq(previousFrom), eq(FROM)))
            .thenReturn(BigDecimal.ZERO);

        var result = useCase.execute(new ViewPlatformBusinessHealthQuery(FROM, TO));

        assertThat(result.previousGrossMarginPercent()).isNull();
        assertThat(result.grossMarginPercent()).isEqualTo(68.0);
    }
}
