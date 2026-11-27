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

import lombok.RequiredArgsConstructor;
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
 * The SPENDING part of this screen is read from ORDERS that have been paid, no longer from invoices: an invoice is
 * now just a document issued after the money arrives (it no longer has status/amount/sourceType), and what can
 * answer "how much did the school pay, and for what" is the Order -- see InvoiceRepository and
 * OrderSettlementService.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolAdminDashboardUseCase implements IUseCase<Void, SchoolAdminDashboardSummaryResponse> {

    /** Number of months drawn on the spending chart, including the current month. */
    private static final int SPENDING_MONTHS = 12;

    /**
     * Maximum number of assessments drawn on the "results due soon" card.
     *
     * <p>This is a WARNING card, not a list of assessments: a school with three assessments waiting to be published
     * can read all three, while with thirty the problem is not seeing all thirty names. Cutting here
     * also keeps the query from ever returning a huge page for the overview screen.
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

    /**
     * ONE {@code now} instant for the whole page.
     *
     * <p>The total card and the per-period card read the same set of assessments and the same overdue test, so
     * two separate {@code Instant.now()} calls could put the same assessment "within the deadline" in one place
     * and "overdue" in another -- two numbers side by side on the same screen differing by a few milliseconds.
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

        return new SchoolAdminDashboardSummaryResponse(
            buildAssessmentStatusCounts(currentUserId, schoolId),
            buildAppealStats(schoolId),
            sumAmount(paidOrders),
            buildMonthlySpending(paidOrders),
            quotaTotal,
            quotaUsed,
            buildSubscriptionRenewal(activeSubscription),
            fundingResponse(schoolId, activeSubscription, quotaTotal, quotaUsed),
            UnscoredWorkloadResponse.of(schoolWorkloadQueryRepository.countUnscored(schoolId, now), now),
            awaitingPublishResponses(schoolId, now),
            oldestPendingAppealWaitedDays(schoolId, now)
        );
    }

    private SchoolFundingResponse fundingResponse(UUID schoolId, Optional<SchoolSubscription> activeSubscription,
            BigDecimal quotaTotal, BigDecimal quotaUsed) {
        return SchoolFundingResponse.of(quotaTotal, quotaUsed, committedToUsers(activeSubscription),
            schoolBalanceRepository.findBySchoolId(schoolId).orElse(null));
    }

    private List<AssessmentAwaitingPublishResponse> awaitingPublishResponses(UUID schoolId, Instant now) {
        return schoolWorkloadQueryRepository
            .findAssessmentsAwaitingPublish(schoolId, now, AWAITING_PUBLISH_LIMIT).stream()
            .map(AssessmentAwaitingPublishResponse::of)
            .toList();
    }

    private Integer oldestPendingAppealWaitedDays(UUID schoolId, Instant now) {
        return BusinessDays.waitedDaysSince(
            assessmentResultAppealRepository.findOldestPendingRequestedAt(schoolId), now);
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
     * The school's orders whose money was collected. SUCCESS is the ONLY status with real money: PENDING is only an
     * intent to buy, while FAILED/CANCELLED/EXPIRED bring in not a single dong (see OrderSettlementService).
     */
    private List<Order> fetchPaidOrders(UUID schoolId) {
        Instant now = Instant.now();
        // The start bound = 00:00 on the 1st of the OLDEST month the chart draws, in Vietnam time -- exactly as in
        // ViewSystemAdminDashboardUseCase.
        //
        // Do NOT use now.minus(SPENDING_MONTHS, ChronoUnit.MONTHS): Instant only accepts units up to DAYS, so that
        // call throws UnsupportedTemporalTypeException, i.e. the school admin screen would return 500 on every
        // call. And even if it ran it would be wrong: a sliding window starting mid-month would cut off the start
        // of the oldest month, while buildMonthlySpending groups by WHOLE months -- the farthest bar of the chart
        // would then be short of money compared with itself.
        Instant from = YearMonth.now(ZoneConstant.BUSINESS_ZONE)
            .minusMonths(SPENDING_MONTHS - 1L)
            .atDay(1)
            .atStartOfDay(ZoneConstant.BUSINESS_ZONE)
            .toInstant();
        // findBySchoolIdAndStatusInRange and NOT findByStatusInRange: the latter does not filter by school at all (it was
        // made for the System Admin screen), so using it here would put the orders of every school into the total
        // spending and the 12-month chart of the one school that is logged in.
        return orderRepository.findBySchoolIdAndStatusInRange(schoolId, OrderStatus.SUCCESS, from, now);
    }

    /**
     * Total spending uses {@code totalAmountVnd} -- what the school REALLY paid, including the service fee and
     * after subtracting the upgrade credit. This is the screen of the payer, so the number has to match their bank
     * statement, not the list price of the plan.
     */
    private static BigDecimal sumAmount(List<Order> orders) {
        return orders.stream()
            .map(order -> order.getTotalAmountVnd())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
    }

    /**
     * Spending of the last 12 months (including the current month), ordered old -> new; a month with no order
     * returns 0. Spending on the plan (subscribe/renew/upgrade) and balance top-ups are kept apart -- plans are
     * usually paid yearly, so most months only have a top-up (or nothing); splitting the two lets the chart tell a
     * "top-up only" month from a "plan renewal due" month.
     *
     * <p>Grouped by the month of {@code createdAt}, not by when the money arrived: an order lives at most 24
     * hours (Order.PENDING_TTL), so the two instants only differ for an order placed on the last day of a month,
     * and this is the instant that {@code sumTotalAmountByStatusInRange} uses -- if the two places looked at
     * different instants, the chart total and the admin screen total would not match.
     *
     * <p>Converted to Vietnam time before cutting the month: cutting by UTC would push the first 7 hours of each
     * month into the previous month -- see ZoneConstant.
     */
    private static List<SchoolMonthlySpendingResponse> buildMonthlySpending(List<Order> orders) {
        Map<YearMonth, BigDecimal> subscriptionByMonth = zeroedSpendingMonths();
        Map<YearMonth, BigDecimal> topUpByMonth = zeroedSpendingMonths();

        for (Order order : orders) {
            YearMonth orderMonth = YearMonth.from(order.getCreatedAt().atZone(ZoneConstant.BUSINESS_ZONE));
            Map<YearMonth, BigDecimal> targetMap = order.getType() == OrderType.TOPUP ? topUpByMonth : subscriptionByMonth;
            // computeIfPresent: an order older than the 12-month window is skipped and does not add a column.
            targetMap.computeIfPresent(orderMonth, (month, total) -> total.add(order.getTotalAmountVnd()));
        }

        return toMonthlySpendingResponses(subscriptionByMonth, topUpByMonth);
    }

    private static Map<YearMonth, BigDecimal> zeroedSpendingMonths() {
        YearMonth currentMonth = YearMonth.now(ZoneConstant.BUSINESS_ZONE);
        Map<YearMonth, BigDecimal> amountByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        for (int i = SPENDING_MONTHS - 1; i >= 0; i--) {
            amountByMonth.put(currentMonth.minusMonths(i), BigDecimal.ZERO);
        }
        return amountByMonth;
    }

    private static List<SchoolMonthlySpendingResponse> toMonthlySpendingResponses(
            Map<YearMonth, BigDecimal> subscriptionByMonth, Map<YearMonth, BigDecimal> topUpByMonth) {
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
            planName, subscription.getStatus().name(), subscription.getEndDate());
    }

}
