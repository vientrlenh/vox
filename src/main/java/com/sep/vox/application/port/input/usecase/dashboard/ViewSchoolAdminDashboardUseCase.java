package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto.AppealStatsDto;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto.ExamStatusCountsDto;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto.MonthlySpendingDto;
import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto.SubscriptionRenewalDto;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.invoice.Invoice;
import com.sep.vox.domain.model.invoice.InvoiceSourceType;
import com.sep.vox.domain.model.invoice.InvoiceStatus;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

@Service
public class ViewSchoolAdminDashboardUseCase implements IUseCase<Void, SchoolAdminDashboardSummaryDto> {

    private final UserContextPort userContextPort;
    private final ExamRepository examRepository;
    private final ExamResultAppealRepository examResultAppealRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final InvoiceRepository invoiceRepository;

    public ViewSchoolAdminDashboardUseCase(UserContextPort userContextPort, ExamRepository examRepository,
            ExamResultAppealRepository examResultAppealRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository,
            SubscriptionPlanRepository subscriptionPlanRepository, InvoiceRepository invoiceRepository) {
        this.userContextPort = userContextPort;
        this.examRepository = examRepository;
        this.examResultAppealRepository = examResultAppealRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public SchoolAdminDashboardSummaryDto execute(Void input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();

        var activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId);
        var gradingQuota = activeGradingQuota(activeSubscription);
        var paidInvoices = fetchPaidInvoices(schoolId);

        return new SchoolAdminDashboardSummaryDto(
            buildExamStatusCounts(currentUserId, schoolId),
            buildAppealStats(schoolId),
            sumAmount(paidInvoices),
            buildMonthlySpending(paidInvoices),
            gradingQuota.map(quota -> quota.getTotalAllocated()).orElse(BigDecimal.ZERO),
            gradingQuota.map(quota -> quota.getUsedQuantity()).orElse(BigDecimal.ZERO),
            buildSubscriptionRenewal(activeSubscription)
        );
    }

    private ExamStatusCountsDto buildExamStatusCounts(UUID currentUserId, UUID schoolId) {
        var draft = countExamsByStatus(currentUserId, schoolId, ExamStatus.DRAFT);
        var scheduled = countExamsByStatus(currentUserId, schoolId, ExamStatus.SCHEDULED);
        var inProgress = countExamsByStatus(currentUserId, schoolId, ExamStatus.IN_PROGRESS);
        var closed = countExamsByStatus(currentUserId, schoolId, ExamStatus.CLOSED);
        var resultsPublished = countExamsByStatus(currentUserId, schoolId, ExamStatus.RESULTS_PUBLISHED);
        var cancelled = countExamsByStatus(currentUserId, schoolId, ExamStatus.CANCELLED);
        var total = draft + scheduled + inProgress + closed + resultsPublished + cancelled;
        return new ExamStatusCountsDto(total, draft, scheduled, inProgress, closed, resultsPublished, cancelled);
    }

    private long countExamsByStatus(UUID currentUserId, UUID schoolId, ExamStatus status) {
        return examRepository.findAccessible(
            currentUserId, schoolId, false, true, schoolId, null, null, status, null, 0, 1
        ).totalElements();
    }

    private AppealStatsDto buildAppealStats(UUID schoolId) {
        return new AppealStatsDto(
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(ExamAppealStatus.PENDING)),
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId,
                EnumSet.of(ExamAppealStatus.APPROVED, ExamAppealStatus.GRADING)),
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(ExamAppealStatus.PUBLISHED)),
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(ExamAppealStatus.REJECTED)),
            examResultAppealRepository.countBySchoolIdAndStatusIn(schoolId, EnumSet.of(ExamAppealStatus.WITHDRAWN))
        );
    }

    private List<Invoice> fetchPaidInvoices(UUID schoolId) {
        var subscriptionIds = schoolSubscriptionRepository.findAllBySchoolId(schoolId).stream()
            .map(subscription -> subscription.getId())
            .toList();
        if (subscriptionIds.isEmpty()) {
            return List.of();
        }
        return invoiceRepository.findAllBySubscriptionIdIn(subscriptionIds).stream()
            .filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID)
            .toList();
    }

    private static BigDecimal sumAmount(List<Invoice> invoices) {
        return invoices.stream()
            .map(i -> i.getAmount())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    /**
     * Chi tiêu 12 tháng gần nhất (kể cả tháng hiện tại), xếp cũ -> mới, tháng không có hóa đơn trả về 0.
     * Tách riêng phần chi cho gói (đăng ký/gia hạn) và phần mua thêm token — gói thường trả theo năm
     * nên hầu hết các tháng chỉ có mua thêm token (hoặc không có gì); tách 2 phần giúp biểu đồ phân
     * biệt được tháng "chỉ mua thêm token" với tháng "đến kỳ gia hạn gói".
     */
    private static List<MonthlySpendingDto> buildMonthlySpending(List<Invoice> invoices) {
        var currentMonth = YearMonth.now(ZoneOffset.UTC);
        var subscriptionByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        var tokenTopUpByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        for (var i = 11; i >= 0; i--) {
            var month = currentMonth.minusMonths(i);
            subscriptionByMonth.put(month, BigDecimal.ZERO);
            tokenTopUpByMonth.put(month, BigDecimal.ZERO);
        }

        for (var invoice : invoices) {
            if (invoice.getPaidAt() == null) {
                continue;
            }
            var invoiceMonth = YearMonth.from(invoice.getPaidAt().atZone(ZoneOffset.UTC));
            var targetMap = invoice.getSourceType() == InvoiceSourceType.TOKEN_PURCHASE
                ? tokenTopUpByMonth
                : subscriptionByMonth;
            targetMap.computeIfPresent(invoiceMonth, (month, total) -> total.add(invoice.getAmount()));
        }

        return subscriptionByMonth.keySet().stream()
            .map(month -> {
                var subscriptionAmount = subscriptionByMonth.get(month);
                var tokenTopUpAmount = tokenTopUpByMonth.get(month);
                return new MonthlySpendingDto(
                    month.toString(),
                    subscriptionAmount.add(tokenTopUpAmount),
                    subscriptionAmount,
                    tokenTopUpAmount
                );
            })
            .toList();
    }

    private Optional<SchoolSubscriptionQuotaRecord> activeGradingQuota(Optional<SchoolSubscription> activeSubscription) {
        return activeSubscription.flatMap(subscription -> subscriptionQuotaRepository
            .findBySubscriptionIdAndQuotaType(subscription.getId(), QuotaType.GRADING));
    }

    private SubscriptionRenewalDto buildSubscriptionRenewal(Optional<SchoolSubscription> activeSubscription) {
        if (activeSubscription.isEmpty()) {
            return null;
        }
        var subscription = activeSubscription.get();
        var planName = subscriptionPlanRepository.findById(subscription.getPlanId())
            .map(plan -> plan.getName())
            .orElse(null);
        return new SubscriptionRenewalDto(planName, subscription.getStatus().name(), subscription.getEndDate().toString());
    }

}
