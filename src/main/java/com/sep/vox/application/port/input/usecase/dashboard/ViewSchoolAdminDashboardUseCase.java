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

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.application.response.input.dashboard.AssessmentAppealStatsResponse;
import com.sep.vox.application.response.input.dashboard.AssessmentAwaitingPublishResponse;
import com.sep.vox.application.response.input.dashboard.AssessmentStatusCountResponse;
import com.sep.vox.application.response.input.dashboard.SchoolAdminDashboardSummaryResponse;
import com.sep.vox.application.response.input.dashboard.SchoolFundingResponse;
import com.sep.vox.application.response.input.dashboard.SchoolMonthlySpendingResponse;
import com.sep.vox.application.response.input.dashboard.SchoolSubscriptionRenewalResponse;
import com.sep.vox.application.response.input.dashboard.UnscoredWorkloadResponse;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentResultAppealRepository;
import com.sep.vox.domain.shared.BusinessDays;
import com.sep.vox.domain.shared.ZoneConstant;

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
        var now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        UUID schoolId = userContextPort.getCurrentSchoolId()
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));

        var activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId);
        var assessmentQuota = activeAssessmentQuota(activeSubscription);
        var paidOrders = fetchPaidOrders(schoolId);
        var quotaTotal = assessmentQuota.map(quota -> quota.getTotalAllocatedAmountVnd()).orElse(BigDecimal.ZERO);
        var quotaUsed = assessmentQuota.map(quota -> quota.getUsedAmountVnd()).orElse(BigDecimal.ZERO);
        var committedToUsers = committedToUsers(activeSubscription);

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
        var draft = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.DRAFT);
        var scheduled = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.SCHEDULED);
        var inProgress = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.IN_PROGRESS);
        var closed = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.CLOSED);
        var resultsPublished = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.RESULTS_PUBLISHED);
        var cancelled = countAssessmentsByStatus(currentUserId, schoolId, AssessmentStatus.CANCELLED);
        var total = draft + scheduled + inProgress + closed + resultsPublished + cancelled;
        return new AssessmentStatusCountResponse(total, draft, scheduled, inProgress, closed, resultsPublished, cancelled);
    }

    /**
     * Chỗ này chỉ cần {@code totalElements()} nên nội dung trang không quan trọng -- lấy trang
     * đầu (chỉ số 0), cỡ 1, để đếm mà không kéo về dòng nào thừa.
     */
    private long countAssessmentsByStatus(UUID currentUserId, UUID schoolId, AssessmentStatus status) {
        return assessmentRepository.findAccessible(
            currentUserId, schoolId, false, true, schoolId, null, null, status, null, 0, 1
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


    private List<Order> fetchPaidOrders(UUID schoolId) {
        var now = Instant.now();
        var from = YearMonth.now(ZoneConstant.BUSINESS_ZONE)
            .minusMonths(SPENDING_MONTHS - 1L)
            .atDay(1)
            .atStartOfDay(ZoneConstant.BUSINESS_ZONE)
            .toInstant();
        return orderRepository.findBySchoolIdAndStatusInRange(schoolId, OrderStatus.SUCCESS, from, now);
    }

    private static BigDecimal sumAmount(List<Order> orders) {
        return orders.stream()
            .map(order -> order.getTotalAmountVnd())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
    }


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

    private Optional<SchoolSubscriptionQuotaRecord> activeAssessmentQuota(Optional<SchoolSubscription> activeSubscription) {
        return activeSubscription.flatMap(subscription -> subscriptionQuotaRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscription.getId(), QuotaType.ASSESSMENT));
    }


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
        var subscription = activeSubscription.get();
        var planName = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .map(plan -> plan.getName())
            .orElse(null);
        return new SchoolSubscriptionRenewalResponse(
            planName, subscription.getStatus().name(), subscription.getEndDate().toString());
    }

}
