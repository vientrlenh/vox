package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SystemAdminDashboardSummaryDto;
import com.sep.vox.domain.dto.SystemAdminDashboardSummaryDto.MonthlyRevenueDto;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.subscription.InvoiceStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

@Service
public class ViewSystemAdminDashboardUseCase implements IUseCase<Void, SystemAdminDashboardSummaryDto> {

    private static final String STUDENT_CODE = "STUDENT";
    private static final String TEACHER_CODE = "TEACHER";
    private static final String SCHOOL_ADMIN_CODE = "SCHOOL_ADMIN";

    private final SchoolRepository schoolRepository;
    private final RegisterFormRepository registerFormRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final FrameworkRepository frameworkRepository;
    private final RubricRepository rubricRepository;

    public ViewSystemAdminDashboardUseCase(SchoolRepository schoolRepository,
            RegisterFormRepository registerFormRepository, InvoiceRepository invoiceRepository,
            RoleRepository roleRepository, UserRoleRepository userRoleRepository,
            FrameworkRepository frameworkRepository, RubricRepository rubricRepository) {
        this.schoolRepository = schoolRepository;
        this.registerFormRepository = registerFormRepository;
        this.invoiceRepository = invoiceRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.frameworkRepository = frameworkRepository;
        this.rubricRepository = rubricRepository;
    }

    @Override
    public SystemAdminDashboardSummaryDto execute(Void input) {
        var now = Instant.now();
        var totalSchools = schoolRepository.countAll();
        var activeSchools = schoolRepository.countByIsActiveTrue();

        var totalRevenue = invoiceRepository.sumAmountByStatus(InvoiceStatus.PAID);

        var activeFrameworkCount = frameworkRepository.findAllActive(1, 1).totalElements();
        var systemRubricCount = rubricRepository.findAllByOwnerType(RubricOwnerType.SYSTEM, 1, 1).totalElements();

        return new SystemAdminDashboardSummaryDto(
            totalSchools,
            activeSchools,
            totalSchools - activeSchools,
            registerFormRepository.countByStatus(RegisterFormStatus.PENDING),
            registerFormRepository.countByCreatedAtAfter(now.minus(30, ChronoUnit.DAYS)),
            registerFormRepository.countByCreatedAtAfter(now.minus(90, ChronoUnit.DAYS)),
            totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
            buildMonthlyRevenue(),
            countUsersByRole(STUDENT_CODE),
            countUsersByRole(TEACHER_CODE),
            countUsersByRole(SCHOOL_ADMIN_CODE),
            activeFrameworkCount,
            systemRubricCount
        );
    }

    private long countUsersByRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
            .map(role -> userRoleRepository.countByRoleId(role.getId()))
            .orElse(0L);
    }

    /** Doanh thu 24 tháng gần nhất (kể cả tháng hiện tại), xếp cũ -> mới, tháng không có hóa đơn trả về 0. */
    private List<MonthlyRevenueDto> buildMonthlyRevenue() {
        var currentMonth = YearMonth.now(ZoneOffset.UTC);
        var totalsByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        for (var i = 23; i >= 0; i--) {
            totalsByMonth.put(currentMonth.minusMonths(i), BigDecimal.ZERO);
        }

        for (var invoice : invoiceRepository.findAllByStatus(InvoiceStatus.PAID)) {
            if (invoice.getPaidAt() == null) {
                continue;
            }
            var invoiceMonth = YearMonth.from(invoice.getPaidAt().atZone(ZoneOffset.UTC));
            totalsByMonth.computeIfPresent(invoiceMonth, (month, total) -> total.add(invoice.getAmount()));
        }

        return totalsByMonth.entrySet().stream()
            .map(entry -> new MonthlyRevenueDto(entry.getKey().toString(), entry.getValue()))
            .toList();
    }

}
