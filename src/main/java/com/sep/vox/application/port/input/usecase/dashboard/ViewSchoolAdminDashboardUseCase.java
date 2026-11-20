package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.application.response.input.dashboard.AssessmentAppealStatsResponse;
import com.sep.vox.application.response.input.dashboard.AssessmentAwaitingPublishResponse;
import com.sep.vox.application.response.input.dashboard.AssessmentStatusCountResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.SchoolFundingResponse;
import com.sep.vox.application.response.input.dashboard.SchoolMonthlySpendingResponse;
import com.sep.vox.application.response.input.dashboard.SchoolSubscriptionRenewalResponse;
import com.sep.vox.application.response.input.dashboard.UnscoredWorkloadResponse;
import com.sep.vox.domain.common.BusinessDays;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
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

    /**
     * Trần số kỳ thi vẽ trên thẻ "sắp công bố điểm".
     *
     * <p>Đây là một thẻ CẢNH BÁO, không phải danh sách kỳ thi: một trường có ba kỳ đang chờ công bố
     * thì đọc hết ba, còn ba mươi kỳ thì vấn đề không nằm ở chỗ nhìn đủ ba mươi cái tên. Cắt ở đây
     * cũng giữ cho câu truy vấn không bao giờ trả về một trang khổng lồ cho màn tổng quan.
     */
    private static final int AWAITING_PUBLISH_LIMIT = 5;

    private final UserContextPort userContextPort;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultAppealRepository assessmentResultAppealRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrderRepository orderRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolWorkloadQueryRepository schoolWorkloadQueryRepository;

    public ViewSchoolAdminDashboardUseCase(UserContextPort userContextPort, AssessmentRepository assessmentRepository,
            AssessmentResultAppealRepository assessmentResultAppealRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository,
            SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            SubscriptionPlanRepository subscriptionPlanRepository, OrderRepository orderRepository,
            SchoolBalanceRepository schoolBalanceRepository,
            SchoolWorkloadQueryRepository schoolWorkloadQueryRepository) {
        this.userContextPort = userContextPort;
        this.assessmentRepository = assessmentRepository;
        this.assessmentResultAppealRepository = assessmentResultAppealRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.orderRepository = orderRepository;
        this.schoolBalanceRepository = schoolBalanceRepository;
        this.schoolWorkloadQueryRepository = schoolWorkloadQueryRepository;
    }

    /**
     * MỘT mốc {@code now} cho cả trang.
     *
     * <p>Thẻ tổng và thẻ theo kỳ đọc cùng một tập bài và cùng một phép xét quá hạn, nên hai lần gọi
     * {@code Instant.now()} riêng có thể xếp cùng một bài vào "đang trong hạn" ở chỗ này và "quá
     * hạn" ở chỗ kia — hai con số cạnh nhau trên cùng màn hình lệch nhau vì vài mili giây.
     */
    @Override
    @Transactional(readOnly = true)
    public SchoolAdminDashboardSummaryResponse execute(Void input) {
        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = userContextPort.getCurrentSchoolId();

        Optional<SchoolSubscription> activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId);
        Optional<SchoolSubscriptionQuotaRecord> assessmentQuota = activeAssessmentQuota(activeSubscription);
        List<Order> paidOrders = fetchPaidOrders(schoolId);
        BigDecimal quotaTotal = assessmentQuota.map(quota -> quota.getTotalAllocatedAmountVnd()).orElse(BigDecimal.ZERO);
        BigDecimal quotaUsed = assessmentQuota.map(quota -> quota.getUsedAmountVnd()).orElse(BigDecimal.ZERO);
        BigDecimal committedToUsers = committedToUsers(activeSubscription);

        return new SchoolAdminDashboardSummaryResponse(
            buildAssessmentStatusCounts(currentUserId, schoolId),
            buildAppealStats(schoolId),
            sumAmount(paidOrders),
            buildMonthlySpending(paidOrders),
            quotaTotal,
            quotaUsed,
            buildSubscriptionRenewal(activeSubscription),
            SchoolFundingResponse.of(quotaTotal, quotaUsed, committedToUsers,
                schoolBalanceRepository.findBySchoolId(schoolId).orElse(null)),
            UnscoredWorkloadResponse.of(schoolWorkloadQueryRepository.countUnscored(schoolId, now), now),
            schoolWorkloadQueryRepository
                .findAssessmentsAwaitingPublish(schoolId, now, AWAITING_PUBLISH_LIMIT).stream()
                .map(AssessmentAwaitingPublishResponse::of)
                .toList(),
            BusinessDays.waitedDaysSince(
                assessmentResultAppealRepository.findOldestPendingRequestedAt(schoolId), now)
        );
    }

    private AssessmentStatusCountResponse buildAssessmentStatusCounts(UUID currentUserId, UUID schoolId) {
        long draft = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.DRAFT);
        long scheduled = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.SCHEDULED);
        long inProgress = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.IN_PROGRESS);
        long closed = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.CLOSED);
        long resultsPublished = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.RESULTS_PUBLISHED);
        long cancelled = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.CANCELLED);
        long total = draft + scheduled + inProgress + closed + resultsPublished + cancelled;
        return new AssessmentStatusCountResponse(total, draft, scheduled, inProgress, closed, resultsPublished, cancelled);
    }

    /**
     * Page 1 and not 0: every paging adapter takes a page number COUNTED FROM 1 and subtracts 1
     * before building the PageRequest (AssessmentRepositoryImpl.findAccessible). Passing 0 gives -1 and
     * Spring Data throws "Page index must not be less than zero" right inside the dashboard request.
     *
     * <p>Only {@code totalElements()} is needed here so the page content does not matter -- take the first
     * page, size 1, to count without pulling back any extra row.
     */
    private long countAssessmentsByStatus(UUID currentUserId, UUID schoolId, AssessmentStatus status) {
        return assessmentRepository.findAccessible(
            currentUserId, schoolId, false, true, schoolId, null, null, status, null, 1, 1
        ).totalElements();
    }

    private AssessmentAppealStatsResponse buildAppealStats(UUID schoolId) {
        return new AssessmentAppealStatsResponse(
            assessmentResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(AssessmentAppealStatus.PENDING)),
            assessmentResultAppealRepository.countBySchoolIdAndStatusIn(schoolId,
                EnumSet.of(AssessmentAppealStatus.APPROVED, AssessmentAppealStatus.GRADING)),
            assessmentResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(AssessmentAppealStatus.PUBLISHED)),
            assessmentResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(AssessmentAppealStatus.REJECTED)),
            assessmentResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(AssessmentAppealStatus.WITHDRAWN))
        );
    }

    /**
     * Đơn đã thu được tiền của trường. SUCCESS là trạng thái DUY NHẤT có tiền thật: PENDING mới chỉ
     * là ý định mua, còn FAILED/CANCELLED/EXPIRED thì không đồng nào về (xem OrderSettlementService).
     */
    private List<Order> fetchPaidOrders(UUID schoolId) {
        Instant now = Instant.now();
        // Mốc đầu = 00:00 ngày 1 của tháng CŨ NHẤT mà biểu đồ vẽ, tính theo giờ VN -- giống hệt
        // ViewSystemAdminDashboardUseCase.
        //
        // KHÔNG dùng now.minus(SPENDING_MONTHS, ChronoUnit.MONTHS): Instant chỉ nhận đơn vị tới DAYS
        // nên câu đó ném UnsupportedTemporalTypeException, tức là màn quản trị của trường 500 ở mọi
        // lượt gọi. Mà kể cả nếu chạy được thì nó cũng sai: cửa sổ trượt bắt đầu từ giữa tháng sẽ cắt
        // mất phần đầu của tháng cũ nhất, trong khi buildMonthlySpending gom theo NGUYÊN tháng -- cột
        // xa nhất của biểu đồ vì thế thiếu tiền so với chính nó.
        Instant from = YearMonth.now(ZoneConstant.BUSINESS_ZONE)
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
        YearMonth currentMonth = YearMonth.now(ZoneConstant.BUSINESS_ZONE);
        Map<YearMonth, BigDecimal> subscriptionByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        Map<YearMonth, BigDecimal> topUpByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        for (int i = SPENDING_MONTHS - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            subscriptionByMonth.put(month, BigDecimal.ZERO);
            topUpByMonth.put(month, BigDecimal.ZERO);
        }

        for (Order order : orders) {
            YearMonth orderMonth = YearMonth.from(order.getCreatedAt().atZone(ZoneConstant.BUSINESS_ZONE));
            Map<YearMonth, BigDecimal> targetMap = order.getType() == OrderType.TOPUP ? topUpByMonth : subscriptionByMonth;
            // computeIfPresent: đơn cũ hơn cửa sổ 12 tháng bị bỏ qua, không tự sinh thêm cột.
            targetMap.computeIfPresent(orderMonth, (month, total) -> total.add(order.getTotalAmountVnd()));
        }

        return subscriptionByMonth.keySet().stream()
            .map(month -> {
                BigDecimal subscriptionAmount = subscriptionByMonth.get(month);
                BigDecimal topUpAmount = topUpByMonth.get(month);
                return new SchoolMonthlySpendingResponse(
                    month.toString(),
                    subscriptionAmount.add(topUpAmount),
                    subscriptionAmount,
                    topUpAmount
                );
            })
            .toList();
    }

    private Optional<SchoolSubscriptionQuotaRecord> activeAssessmentQuota(Optional<SchoolSubscription> activeSubscription) {
        return activeSubscription.flatMap(subscription -> subscriptionQuotaRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscription.getId(), QuotaType.ASSESSMENT));
    }

    /**
     * The part of the assessment quota already promised to teachers that they have not yet spent.
     *
     * <p>Only the ASSESSMENT wallet: the student's spending ceiling lives in the PRACTICE wallet, and adding it here would make the "still free" amount of
     * assessments be reduced by a pocket of money that does not pay for assessment grading at all -- the same rule "two different
     * pockets, do not merge" that SchoolFundingResponse upholds.
     *
     * <p>With no running subscription period there is no ceiling to promise: the allocation is attached to
     * schoolSubscriptionId, and the quota record is rebuilt every period (OrderSettlementService
     * .seedQuotaRecords), so asking here would be asking about a period that does not exist.
     */
    private BigDecimal committedToUsers(Optional<SchoolSubscription> activeSubscription) {
        return activeSubscription
            .map(subscription -> subscriptionQuotaUserAllocationRepository
                .sumUnusedAllocation(subscription.getId(), QuotaType.ASSESSMENT))
            .orElse(BigDecimal.ZERO);
    }

    private SchoolSubscriptionRenewalResponse buildSubscriptionRenewal(Optional<SchoolSubscription> activeSubscription) {
        if (activeSubscription.isEmpty()) {
            return null;
        }
        SchoolSubscription subscription = activeSubscription.get();
        String planName = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .map(plan -> plan.getName())
            .orElse(null);
        return new SchoolSubscriptionRenewalResponse(
            planName, subscription.getStatus().name(), subscription.getEndDate().toString());
    }

}
