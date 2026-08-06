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
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;

@Service
public class ViewSchoolAdminDashboardUseCase implements IUseCase<Void, SchoolAdminDashboardSummaryDto> {

    private final UserContextPort userContextPort;
    private final ExamRepository examRepository;
    private final ExamResultAppealRepository examResultAppealRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final InvoiceRepository invoiceRepository;

    public ViewSchoolAdminDashboardUseCase(UserContextPort userContextPort, ExamRepository examRepository,
            ExamResultAppealRepository examResultAppealRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository, InvoiceRepository invoiceRepository) {
        this.userContextPort = userContextPort;
        this.examRepository = examRepository;
        this.examResultAppealRepository = examResultAppealRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public SchoolAdminDashboardSummaryDto execute(Void input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();

        var gradingQuota = activeGradingQuota(schoolId);
        var paidInvoices = fetchPaidInvoices(schoolId);

        return new SchoolAdminDashboardSummaryDto(
            buildExamStatusCounts(currentUserId, schoolId),
            buildAppealStats(schoolId),
            sumAmount(paidInvoices),
            buildMonthlySpending(paidInvoices),
            gradingQuota.map(quota -> toLong(quota.getTotalAllocated())).orElse(0L),
            gradingQuota.map(quota -> toLong(quota.getUsedQuantity())).orElse(0L)
        );
    }

    private static long toLong(Integer value) {
        return value == null ? 0L : value.longValue();
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
            .map(Invoice::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Chi tiêu 12 tháng gần nhất (kể cả tháng hiện tại), xếp cũ -> mới, tháng không có hóa đơn trả về 0. */
    private static List<MonthlySpendingDto> buildMonthlySpending(List<Invoice> invoices) {
        var currentMonth = YearMonth.now(ZoneOffset.UTC);
        var totalsByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        for (var i = 11; i >= 0; i--) {
            totalsByMonth.put(currentMonth.minusMonths(i), BigDecimal.ZERO);
        }

        for (var invoice : invoices) {
            if (invoice.getPaidAt() == null) {
                continue;
            }
            var invoiceMonth = YearMonth.from(invoice.getPaidAt().atZone(ZoneOffset.UTC));
            totalsByMonth.computeIfPresent(invoiceMonth, (month, total) -> total.add(invoice.getAmount()));
        }

        return totalsByMonth.entrySet().stream()
            .map(entry -> new MonthlySpendingDto(entry.getKey().toString(), entry.getValue()))
            .toList();
    }

    private Optional<SubscriptionQuota> activeGradingQuota(UUID schoolId) {
        return schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .flatMap(subscription -> subscriptionQuotaRepository
                .findBySubscriptionIdAndQuotaType(subscription.getId(), QuotaType.GRADING));
    }

}
