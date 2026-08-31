package com.sep.vox.application.port.input.usecase.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.dashboard.MonthlyRevenueResponse;
import com.sep.vox.application.response.input.dashboard.SystemAdminDashboardSummaryResponse;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

/**
 * Doanh thu đọc từ ĐƠN HÀNG đã thu được tiền (Order.status = SUCCESS) thay vì từ hóa đơn: hóa đơn
 * giờ chỉ là chứng từ phát hành sau khi tiền về và không còn cột status lẫn amount -- xem
 * InvoiceRepository. Đây cũng là con số ĐÚNG hơn bản cũ: hóa đơn chỉ tồn tại cho đơn đã thành công,
 * nên "lọc hóa đơn PAID" trước đây là lọc một điều kiện luôn đúng.
 */
@Service
public class ViewSystemAdminDashboardUseCase implements IUseCase<Void, SystemAdminDashboardSummaryResponse> {

    private static final String STUDENT_CODE = "STUDENT";
    private static final String TEACHER_CODE = "TEACHER";
    private static final String SCHOOL_ADMIN_CODE = "SCHOOL_ADMIN";

    /** Số tháng vẽ trên biểu đồ doanh thu, kể cả tháng hiện tại. */
    private static final int REVENUE_MONTHS = 24;

    private final SchoolRepository schoolRepository;
    private final RegisterFormRepository registerFormRepository;
    private final OrderRepository orderRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final FrameworkRepository frameworkRepository;
    private final RubricRepository rubricRepository;

    public ViewSystemAdminDashboardUseCase(SchoolRepository schoolRepository,
            RegisterFormRepository registerFormRepository, OrderRepository orderRepository,
            RoleRepository roleRepository, UserRoleRepository userRoleRepository,
            FrameworkRepository frameworkRepository, RubricRepository rubricRepository) {
        this.schoolRepository = schoolRepository;
        this.registerFormRepository = registerFormRepository;
        this.orderRepository = orderRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.frameworkRepository = frameworkRepository;
        this.rubricRepository = rubricRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SystemAdminDashboardSummaryResponse execute(Void input) {
        var now = Instant.now();
        var totalSchools = schoolRepository.countAll();
        var activeSchools = schoolRepository.countByIsActiveTrue();

        // Cộng dồn ở DB chứ không nạp đơn về đếm: đây là con số TỪ TRƯỚC TỚI NAY, danh sách đứng sau
        // nó chỉ có tăng. Mốc dưới là EPOCH vì "tất cả" -- khoảng nửa mở [from, to) nên mốc trên là
        // now, tức tính tới đúng thời điểm mở màn hình.
        var totalRevenue = orderRepository.sumTotalAmountByStatusInRange(OrderStatus.SUCCESS, Instant.EPOCH, now);

        var activeFrameworkCount = frameworkRepository.findAllActive(1, 1).totalElements();
        var systemRubricCount = rubricRepository.findAllByOwnerType(RubricOwnerType.SYSTEM, 1, 1).totalElements();

        return new SystemAdminDashboardSummaryResponse(
            totalSchools,
            activeSchools,
            totalSchools - activeSchools,
            registerFormRepository.countByStatus(RegisterFormStatus.PENDING),
            oldestPendingRegistrationDays(now),
            registerFormRepository.countByCreatedAtAfter(now.minus(30, ChronoUnit.DAYS)),
            registerFormRepository.countByCreatedAtAfter(now.minus(90, ChronoUnit.DAYS)),
            totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
            buildMonthlyRevenue(now),
            countUsersByRole(STUDENT_CODE),
            countUsersByRole(TEACHER_CODE),
            countUsersByRole(SCHOOL_ADMIN_CODE),
            activeFrameworkCount,
            systemRubricCount
        );
    }

    /**
     * Đơn chờ lâu nhất đã nằm trong hàng đợi bao nhiêu NGÀY LỊCH, theo giờ nghiệp vụ.
     *
     * <p>Đếm theo ngày lịch chứ không phải elapsed chia 24 giờ: đơn nộp 23:00 hôm qua mà bây giờ là
     * 01:00 thì người trực đọc là "đã sang ngày thứ hai", trong khi phép chia cho 24 giờ vẫn trả 0 và
     * làm thẻ KPI trông như hàng đợi vẫn sạch.
     *
     * <p>{@code null} khi không còn đơn nào chờ — cùng quy ước với {@code grossMarginPercent}: thiếu
     * dữ liệu và "bằng 0" là hai chuyện khác nhau, và ở đây 0 lại là trạng thái TỐT nhất nên nhập
     * nhèm hai cái sẽ vẽ ra một hàng đợi rỗng trông y hệt một hàng đợi vừa được xử lý xong.
     */
    private Integer oldestPendingRegistrationDays(Instant now) {
        return registerFormRepository.findOldestCreatedAtByStatus(RegisterFormStatus.PENDING)
            .map(oldest -> (int) ChronoUnit.DAYS.between(
                oldest.atZone(ZoneConstant.BUSINESS_ZONE).toLocalDate(),
                now.atZone(ZoneConstant.BUSINESS_ZONE).toLocalDate()))
            .orElse(null);
    }

    private long countUsersByRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
            .map(role -> userRoleRepository.countByRoleId(role.getId()))
            .orElse(0L);
    }

    /**
     * Doanh thu 24 tháng gần nhất (kể cả tháng hiện tại), xếp cũ -> mới, tháng không có đơn nào trả
     * về 0.
     *
     * <p>Chỉ nạp đúng cửa sổ 24 tháng thay vì mọi đơn thành công: bản cũ nạp toàn bộ hóa đơn PAID
     * rồi vứt đi phần ngoài cửa sổ, tức là mỗi năm trôi qua màn hình lại nặng thêm mà kết quả không
     * đổi.
     *
     * <p>Gom theo tháng của {@code createdAt} -- cùng mốc với sumTotalAmountByStatusInRange ở trên,
     * để tổng của biểu đồ và con số tổng doanh thu không nói hai chuyện khác nhau. Quy về giờ Việt
     * Nam trước khi cắt tháng: cắt theo UTC sẽ đẩy 7 giờ đầu của mỗi tháng sang tháng trước.
     */
    private List<MonthlyRevenueResponse> buildMonthlyRevenue(Instant now) {
        var currentMonth = YearMonth.now(ZoneConstant.BUSINESS_ZONE);
        var totalsByMonth = new LinkedHashMap<YearMonth, BigDecimal>();
        for (var i = REVENUE_MONTHS - 1; i >= 0; i--) {
            totalsByMonth.put(currentMonth.minusMonths(i), BigDecimal.ZERO);
        }

        var windowStart = currentMonth.minusMonths(REVENUE_MONTHS - 1L)
            .atDay(1).atStartOfDay(ZoneConstant.BUSINESS_ZONE).toInstant();

        for (Order order : orderRepository.findByStatusInRange(OrderStatus.SUCCESS, windowStart, now)) {
            var orderMonth = YearMonth.from(order.getCreatedAt().atZone(ZoneConstant.BUSINESS_ZONE));
            totalsByMonth.computeIfPresent(orderMonth, (month, total) -> total.add(order.getTotalAmountVnd()));
        }

        return totalsByMonth.entrySet().stream()
            .map(entry -> new MonthlyRevenueResponse(entry.getKey().toString(), entry.getValue()))
            .toList();
    }

}
