package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
import com.sep.vox.application.response.input.dashboard.PlatformBusinessHealthResponse;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.repository.OrderRepository;

/**
 * Trường còn gói / sắp rụng / đang nợ, cùng doanh thu đặt cạnh giá vốn AI.
 *
 * <p>Doanh thu đọc từ ĐƠN HÀNG đã thu tiền ({@code OrderStatus.SUCCESS}) — cùng nguồn với
 * {@link ViewSystemAdminDashboardUseCase}, để hai chỗ trên cùng một màn hình không nói hai con số
 * khác nhau.
 */
@Service
public class ViewPlatformBusinessHealthUseCase
        implements IUseCase<ViewPlatformBusinessHealthUseCase.Query, PlatformBusinessHealthResponse> {

    /** Ngưỡng "sắp hết hạn" mặc định của dashboard. */
    static final int EXPIRING_SOON_DAYS = 30;

    private final PlatformBusinessHealthQueryRepository platformBusinessHealthQueryRepository;
    private final OrderRepository orderRepository;

    public ViewPlatformBusinessHealthUseCase(
            PlatformBusinessHealthQueryRepository platformBusinessHealthQueryRepository,
            OrderRepository orderRepository) {
        this.platformBusinessHealthQueryRepository = platformBusinessHealthQueryRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * @param dateFrom mốc đầu BAO GỒM; bỏ trống = đầu tháng hiện tại theo giờ nghiệp vụ
     * @param dateTo   mốc cuối KHÔNG bao gồm; bỏ trống = ngay lúc này
     */
    public record Query(Instant dateFrom, Instant dateTo) {
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformBusinessHealthResponse execute(Query input) {
        var zone = ZoneConstant.BUSINESS_ZONE;
        var now = Instant.now();

        // Ngưỡng trượt theo mốc hiện tại, KHÔNG neo vào ngày lịch: hai cột end_date/start_date là
        // timestamptz, nên "30 ngày nữa" tính bằng instant là phép so duy nhất không phải đoán múi giờ.
        var health = platformBusinessHealthQueryRepository.countSchoolSubscriptionHealth(
            now, now.plus(EXPIRING_SOON_DAYS, ChronoUnit.DAYS));
        var schoolsInDebt = platformBusinessHealthQueryRepository.countSchoolsInDebt();

        var to = input == null || input.dateTo() == null ? now : input.dateTo();
        var from = input == null || input.dateFrom() == null
            ? YearMonth.now(zone).atDay(1).atStartOfDay(zone).toInstant()
            : input.dateFrom();

        if (!from.isBefore(to)) {
            // Khoảng rỗng/ngược: phần đếm trường vẫn đúng vì nó không phụ thuộc cửa sổ, phần tiền về 0.
            return new PlatformBusinessHealthResponse(
                health.subscribedSchools(), health.expiringSoonSchools(), health.lapsedSchools(),
                health.suspendedSchools(), schoolsInDebt,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        }

        var revenue = zeroIfNull(orderRepository.sumTotalAmountByStatusInRange(OrderStatus.SUCCESS, from, to));
        var aiCost = zeroIfNull(platformBusinessHealthQueryRepository.sumAiCostVnd(from, to));

        // Kỳ so sánh là một khoảng CÙNG ĐỘ DÀI ngay trước đó, không phải tháng lịch trước. Mặc định
        // của màn hình là "từ đầu tháng tới giờ"; đem 10 ngày đầu tháng so với trọn tháng trước thì
        // tháng này luôn trông như sụp đổ, và đó là lỗi của phép so chứ không phải của việc kinh doanh.
        var windowLength = Duration.between(from, to);
        var previousFrom = from.minus(windowLength);
        var previousRevenue = zeroIfNull(orderRepository.sumTotalAmountByStatusInRange(
            OrderStatus.SUCCESS, previousFrom, from));

        // Giá vốn của kỳ trước chỉ để dựng ra BIÊN của kỳ trước, nên không trả nó ra ngoài: màn hình
        // hỏi "biên tháng này hơn kém tháng trước mấy điểm", không hỏi chi phí kỳ trước là bao nhiêu.
        var previousAiCost = zeroIfNull(platformBusinessHealthQueryRepository.sumAiCostVnd(previousFrom, from));

        return new PlatformBusinessHealthResponse(
            health.subscribedSchools(), health.expiringSoonSchools(), health.lapsedSchools(),
            health.suspendedSchools(), schoolsInDebt,
            revenue, previousRevenue, aiCost,
            grossMarginPercent(revenue, aiCost),
            grossMarginPercent(previousRevenue, previousAiCost));
    }

    /**
     * {@code null} khi chưa thu được đồng nào: biên lợi nhuận của doanh thu 0 không phải là 0%, và
     * cũng không phải -∞ — nó không tồn tại. Ép thành 0% sẽ vẽ ra một tháng "biên 0%" trông như đang
     * lỗ, trong khi thật ra chỉ là chưa có hóa đơn nào rơi vào cửa sổ.
     */
    private static Double grossMarginPercent(BigDecimal revenue, BigDecimal aiCost) {
        if (revenue.signum() <= 0) {
            return null;
        }
        return revenue.subtract(aiCost)
            .multiply(BigDecimal.valueOf(100))
            .divide(revenue, 1, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
