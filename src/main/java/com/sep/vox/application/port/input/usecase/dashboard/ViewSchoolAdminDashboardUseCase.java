package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.dashboard.ExamAppealStatsResponse;
import com.sep.vox.application.response.input.dashboard.ExamStatusCountResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.SchoolMonthlySpendingResponse;
import com.sep.vox.application.response.input.dashboard.SchoolSubscriptionRenewalResponse;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Phần CHI TIÊU của màn này đọc từ ĐƠN HÀNG đã thu được tiền, không còn từ hóa đơn: hóa đơn giờ chỉ
 * là chứng từ phát hành sau khi tiền về (không còn status/amount/sourceType), còn thứ trả lời được
 * "trường đã trả bao nhiêu, cho cái gì" là Order -- xem InvoiceRepository và OrderSettlementService.
 */
@Service
public class ViewSchoolAdminDashboardUseCase implements IUseCase<Void, SchoolAdminDashboardSummaryResponse> {

    /** Số tháng vẽ trên biểu đồ chi tiêu, kể cả tháng hiện tại. */
    private static final int SPENDING_MONTHS = 12;

    private final UserContextPort userContextPort;
    private final ExamRepository examRepository;
    private final ExamResultAppealRepository examResultAppealRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrderRepository orderRepository;

    public ViewSchoolAdminDashboardUseCase(UserContextPort userContextPort, ExamRepository examRepository,
            ExamResultAppealRepository examResultAppealRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository,
            SubscriptionPlanRepository subscriptionPlanRepository, OrderRepository orderRepository) {
        this.userContextPort = userContextPort;
        this.examRepository = examRepository;
        this.examResultAppealRepository = examResultAppealRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolAdminDashboardSummaryResponse execute(Void input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();

        var activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId);
        var examQuota = activeExamQuota(activeSubscription);
        var paidOrders = fetchPaidOrders(schoolId);

        return new SchoolAdminDashboardSummaryResponse(
            buildExamStatusCounts(currentUserId, schoolId),
            buildAppealStats(schoolId),
            sumAmount(paidOrders),
            buildMonthlySpending(paidOrders),
            examQuota.map(quota -> quota.getTotalAllocatedAmountVnd()).orElse(BigDecimal.ZERO),
            examQuota.map(quota -> quota.getUsedAmountVnd()).orElse(BigDecimal.ZERO),
            buildSubscriptionRenewal(activeSubscription)
        );
    }

    private ExamStatusCountResponse buildExamStatusCounts(UUID currentUserId, UUID schoolId) {
        var draft = countExamsByStatus(currentUserId, schoolId, ExamStatus.DRAFT);
        var scheduled = countExamsByStatus(currentUserId, schoolId, ExamStatus.SCHEDULED);
        var inProgress = countExamsByStatus(currentUserId, schoolId, ExamStatus.IN_PROGRESS);
        var closed = countExamsByStatus(currentUserId, schoolId, ExamStatus.CLOSED);
        var resultsPublished = countExamsByStatus(currentUserId, schoolId, ExamStatus.RESULTS_PUBLISHED);
        var cancelled = countExamsByStatus(currentUserId, schoolId, ExamStatus.CANCELLED);
        var total = draft + scheduled + inProgress + closed + resultsPublished + cancelled;
        return new ExamStatusCountResponse(total, draft, scheduled, inProgress, closed, resultsPublished, cancelled);
    }

    private long countExamsByStatus(UUID currentUserId, UUID schoolId, ExamStatus status) {
        return examRepository.findAccessible(
            currentUserId, schoolId, false, true, schoolId, null, null, status, null, 0, 1
        ).totalElements();
    }

    private ExamAppealStatsResponse buildAppealStats(UUID schoolId) {
        return new ExamAppealStatsResponse(
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(ExamAppealStatus.PENDING)),
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId,
                EnumSet.of(ExamAppealStatus.APPROVED, ExamAppealStatus.GRADING)),
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(ExamAppealStatus.PUBLISHED)),
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(ExamAppealStatus.REJECTED)),
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(ExamAppealStatus.WITHDRAWN))
        );
    }

    /**
     * Đơn đã thu được tiền của trường. SUCCESS là trạng thái DUY NHẤT có tiền thật: PENDING mới chỉ
     * là ý định mua, còn FAILED/CANCELLED/EXPIRED thì không đồng nào về (xem OrderSettlementService).
     */
    private List<Order> fetchPaidOrders(UUID schoolId) {
        var now = Instant.now();
        // Mốc đầu = 00:00 ngày 1 của tháng CŨ NHẤT mà biểu đồ vẽ, tính theo giờ VN -- giống hệt
        // ViewSystemAdminDashboardUseCase.
        //
        // KHÔNG dùng now.minus(SPENDING_MONTHS, ChronoUnit.MONTHS): Instant chỉ nhận đơn vị tới DAYS
        // nên câu đó ném UnsupportedTemporalTypeException, tức là màn quản trị của trường 500 ở mọi
        // lượt gọi. Mà kể cả nếu chạy được thì nó cũng sai: cửa sổ trượt bắt đầu từ giữa tháng sẽ cắt
        // mất phần đầu của tháng cũ nhất, trong khi buildMonthlySpending gom theo NGUYÊN tháng -- cột
        // xa nhất của biểu đồ vì thế thiếu tiền so với chính nó.
        var from = YearMonth.now(ZoneConstant.BUSINESS_ZONE)
            .minusMonths(SPENDING_MONTHS - 1L)
            .atDay(1)
            .atStartOfDay(ZoneConstant.BUSINESS_ZONE)
            .toInstant();
        // findBySchoolIdAndStatusInRange chứ KHÔNG phải findByStatusInRange: bản kia không lọc trường
        // nào cả (nó sinh ra cho màn System Admin), nên dùng ở đây là đưa đơn của mọi trường vào tổng
        // chi và biểu đồ 12 tháng của đúng một trường đang đăng nhập.
        return orderRepository.findBySchoolIdAndStatusInRange(schoolId, OrderStatus.SUCCESS, from, now);
    }

    /**
     * Tổng chi lấy {@code totalAmountVnd} -- số trường THẬT SỰ trả, đã gồm phí dịch vụ và đã trừ
     * khoản bù nâng cấp. Đây là màn của người trả tiền, nên con số phải khớp với sao kê ngân hàng
     * của họ chứ không phải với giá niêm yết của gói.
     */
    private static BigDecimal sumAmount(List<Order> orders) {
        return orders.stream()
            .map(order -> order.getTotalAmountVnd())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
    }

    /**
     * Chi tiêu 12 tháng gần nhất (kể cả tháng hiện tại), xếp cũ -> mới, tháng không có đơn nào trả về
     * 0. Tách riêng phần chi cho gói (đăng ký/gia hạn/nâng cấp) và phần nạp thêm số dư -- gói thường
     * trả theo năm nên hầu hết các tháng chỉ có nạp thêm (hoặc không có gì); tách hai phần giúp biểu
     * đồ phân biệt được tháng "chỉ nạp thêm" với tháng "đến kỳ gia hạn gói".
     *
     * <p>Gom theo tháng của {@code createdAt} chứ không phải lúc tiền về: đơn chỉ sống tối đa 24
     * tiếng (Order.PENDING_TTL) nên hai mốc chỉ lệch nhau khi đơn đặt vào ngày cuối tháng, và đây là
     * mốc mà {@code sumTotalAmountByStatusInRange} đang dùng -- hai chỗ soi khác mốc thì tổng của
     * biểu đồ và tổng của màn quản trị sẽ không khớp nhau.
     *
     * <p>Quy về giờ Việt Nam trước khi cắt tháng: cắt theo UTC sẽ đẩy 7 giờ đầu tiên của mỗi tháng
     * sang tháng trước -- xem ZoneConstant.
     */
    private static List<SchoolMonthlySpendingResponse> buildMonthlySpending(List<Order> orders) {
        var currentMonth = YearMonth.now(ZoneConstant.BUSINESS_ZONE);
        var subscriptionByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        var topUpByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        for (var i = SPENDING_MONTHS - 1; i >= 0; i--) {
            var month = currentMonth.minusMonths(i);
            subscriptionByMonth.put(month, BigDecimal.ZERO);
            topUpByMonth.put(month, BigDecimal.ZERO);
        }

        for (var order : orders) {
            var orderMonth = YearMonth.from(order.getCreatedAt().atZone(ZoneConstant.BUSINESS_ZONE));
            var targetMap = order.getType() == OrderType.TOPUP ? topUpByMonth : subscriptionByMonth;
            // computeIfPresent: đơn cũ hơn cửa sổ 12 tháng bị bỏ qua, không tự sinh thêm cột.
            targetMap.computeIfPresent(orderMonth, (month, total) -> total.add(order.getTotalAmountVnd()));
        }

        return subscriptionByMonth.keySet().stream()
            .map(month -> {
                var subscriptionAmount = subscriptionByMonth.get(month);
                var topUpAmount = topUpByMonth.get(month);
                return new SchoolMonthlySpendingResponse(
                    month.toString(),
                    subscriptionAmount.add(topUpAmount),
                    subscriptionAmount,
                    topUpAmount
                );
            })
            .toList();
    }

    private Optional<SchoolSubscriptionQuotaRecord> activeExamQuota(Optional<SchoolSubscription> activeSubscription) {
        return activeSubscription.flatMap(subscription -> subscriptionQuotaRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscription.getId(), QuotaType.EXAM));
    }

    private SchoolSubscriptionRenewalResponse buildSubscriptionRenewal(Optional<SchoolSubscription> activeSubscription) {
        if (activeSubscription.isEmpty()) {
            return null;
        }
        var subscription = activeSubscription.get();
        var planName = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .map(plan -> plan.getName())
            .orElse(null);
        return new SchoolSubscriptionRenewalResponse(
            planName, subscription.getStatus().name(), subscription.getEndDate().toString());
    }

}
