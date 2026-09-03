package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.WalletDrawConfirmationRequiredException;
import com.sep.vox.application.port.input.command.AllocateUserQuotaAmountCommand;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationPageResponse;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.domain.common.DistributionMode;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

@Service
public class DistributeQuotaToUsersService {

    private static final int MAX_ELIGIBLE_USERS_PAGE_SIZE = 10_000;

    // Đơn vị nhỏ nhất theo đúng scale numeric(18,6) của cột quota (xem V22__quota_unit_to_usd.sql,
    // precision nới lên ở V27__widen_quota_columns_precision.sql, scale 6 giữ nguyên).
    // Dùng để rải phần dư sau khi chia đều -- tương đương "1 giây" ở logic chia số nguyên cũ, giờ là
    // "1 phần triệu đô" để chia hết totalAllocated mà không làm tròn mất tiền.
    private static final BigDecimal SMALLEST_UNIT = new BigDecimal("0.000001");

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final RoleRepository roleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;

    public DistributeQuotaToUsersService(
            UserContextPort userContextPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository,
            SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            RoleRepository roleRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolQuotaPolicyRepository schoolQuotaPolicyRepository,
            SchoolBalanceRepository schoolBalanceRepository) {
        this.userContextPort = userContextPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.roleRepository = roleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolQuotaPolicyRepository = schoolQuotaPolicyRepository;
        this.schoolBalanceRepository = schoolBalanceRepository;
    }

    public QuotaUserAllocationSummaryResponse distribute(UUID schoolId, QuotaType quotaType, String roleCode,
            DistributionMode mode, List<AllocateUserQuotaAmountCommand> allocations, boolean confirmWalletDraw) {
        requireSchoolAdminAccess(schoolId);

        var subscription = findActiveSubscription(schoolId);
        var pool = findPool(subscription.getId(), quotaType);
        var role = findRole(roleCode);
        var eligibleUserIds = fetchEligibleUserIds(schoolId, role.getId());
        if (eligibleUserIds.isEmpty()) {
            throw new IllegalArgumentException("Trường chưa có giáo viên/học sinh phù hợp để phân bổ hạn mức");
        }

        var existing = fetchExistingAllocationsByUserId(subscription.getId(), quotaType);

        // Trần phân phối áp cho CẢ HAI chế độ, không riêng chia đều: nếu chỉ chặn ở AUTO thì quản
        // trị viên đi vòng qua bằng cách sửa tay từng người -- mà sửa tay chính là đường thường dùng
        // sau khi màn chia hạn mức chuyển sang hộp thoại từng người. Một trần đi vòng được không phải
        // là trần.
        var policy = schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, quotaType);
        var distributableVnd = policy.distributableAmountOf(orZero(pool.getTotalAllocatedAmountVnd()));

        var targetAmounts = mode == DistributionMode.AUTO
            ? computeAutoSplit(eligibleUserIds, distributableVnd, existing)
            : computeManualAmounts(allocations, eligibleUserIds, existing, distributableVnd,
                walletHeadroomVnd(schoolId), confirmWalletDraw);

        targetAmounts.forEach((userId, amount) ->
            subscriptionQuotaUserAllocationRepository.upsertAllocation(subscription.getId(), quotaType, userId, amount));

        return buildSummary(subscription.getId(), quotaType, pool, eligibleUserIds);
    }

    /**
     * Phần ví tự nạp CÓ THỂ ăn thêm ngoài {@code distributableVnd} khi nới trần cá nhân của MỘT
     * người -- không phải một túi tiền dành riêng, chỉ là trần soft-ceiling giống hệt cách pool đã
     * vận hành (xem ConsumeQuotaService.chargeOverage, nơi tiền thật sự bị trừ). Kẹp về 0: âm là nợ
     * của trường, không phải phần chia được thêm.
     */
    private BigDecimal walletHeadroomVnd(UUID schoolId) {
        return schoolBalanceRepository.findBySchoolId(schoolId)
            .map(balance -> orZero(balance.getBalanceVnd()))
            .orElse(BigDecimal.ZERO)
            .max(BigDecimal.ZERO);
    }

    /**
     * MỘT TRANG của danh sách chia hạn mức, kèm tổng đã chia trên toàn bộ tập.
     *
     * <p>Đây là đường đọc DUY NHẤT của màn chia hạn mức. Bản không phân trang trước đây gom mọi người
     * đủ điều kiện trong một lượt (tới {@value #MAX_ELIGIBLE_USERS_PAGE_SIZE} người), mà một trường
     * vài nghìn học sinh thì đó là vài nghìn dòng cho mỗi lần mở trang -- nó đã bị bỏ cùng với hai
     * endpoint REST gọi tới nó.
     *
     * <p>Phân trang và tìm kiếm đẩy xuống tận truy vấn người dùng, không cắt trong bộ nhớ: cắt ở
     * tầng Java vẫn phải đọc đủ mọi người trước đã, tức là không tiết kiệm được gì.
     */
    public QuotaUserAllocationPageResponse viewPage(
            UUID schoolId, QuotaType quotaType, String roleCode, String search, int page, int size) {
        requireSchoolAdminAccess(schoolId);

        var subscription = findActiveSubscription(schoolId);
        var pool = findPool(subscription.getId(), quotaType);
        var role = findRole(roleCode);

        var userPage = schoolUserRepository.findBySchoolId(
            schoolId, blankToNull(search), role.getId(), UserStatus.ACTIVE.name(), null, false, page, size);

        var existing = fetchExistingAllocationsByUserId(subscription.getId(), quotaType);

        var rows = userPage.content().stream()
            .map(schoolUser -> schoolUser.getUserId())
            .map(userId -> {
                var allocation = existing.get(userId);
                return new QuotaUserAllocationPageResponse.Row(
                    userId,
                    allocation == null ? BigDecimal.ZERO : orZero(allocation.getAllocatedAmountVnd()),
                    allocation == null ? BigDecimal.ZERO : orZero(allocation.getUsedAmountVnd()));
            })
            .toList();

        // Tổng trên TOÀN BỘ tập, không phải trang đang xem -- giao diện dùng nó để biết còn chia được
        // bao nhiêu, mà cộng cột của một trang thì ra số sai ngay từ trang thứ hai.
        var distributed = existing.values().stream()
            .map(allocation -> orZero(allocation.getAllocatedAmountVnd()))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        var policy = schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, quotaType);

        return new QuotaUserAllocationPageResponse(
            SchoolSubscriptionQuotaRecordDto.toDto(pool),
            distributed,
            policy.getDistributableRatio(),
            policy.distributableAmountOf(orZero(pool.getTotalAllocatedAmountVnd())),
            walletHeadroomVnd(schoolId),
            rows,
            userPage.page(),
            userPage.size(),
            userPage.totalElements(),
            userPage.totalPages()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<UUID, BigDecimal> computeAutoSplit(List<UUID> eligibleUserIds, BigDecimal distributableVnd,
            Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing) {
        var count = eligibleUserIds.size();
        var base = distributableVnd.divide(BigDecimal.valueOf(count), 6, RoundingMode.DOWN);
        // Phần dư sau khi chia đều (không hết vì DOWN), quy đổi sang số đơn vị SMALLEST_UNIT để rải
        // cho count đầu tiên -- giữ đúng tổng phần được chia, không "mất" hay "sinh thêm" tiền do làm tròn.
        var remainderUnits = distributableVnd.subtract(base.multiply(BigDecimal.valueOf(count)))
            .divide(SMALLEST_UNIT, 0, RoundingMode.DOWN)
            .intValueExact();

        var result = new LinkedHashMap<UUID, BigDecimal>();
        for (int i = 0; i < count; i++) {
            var userId = eligibleUserIds.get(i);
            var amount = i < remainderUnits ? base.add(SMALLEST_UNIT) : base;
            var used = usedQuantityOrZero(existing.get(userId));
            if (amount.compareTo(used) < 0) {
                throw new IllegalArgumentException(
                    "Không thể chia đều vì có người dùng đã sử dụng vượt mức chia mới, hãy dùng chế độ thủ công");
            }
            result.put(userId, amount);
        }
        return result;
    }

    private Map<UUID, BigDecimal> computeManualAmounts(List<AllocateUserQuotaAmountCommand> allocations, List<UUID> eligibleUserIds,
            Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing, BigDecimal distributableVnd,
            BigDecimal walletHeadroomVnd, boolean confirmWalletDraw) {
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("Danh sách phân bổ không được để trống");
        }

        var eligibleSet = new HashSet<>(eligibleUserIds);
        var result = new LinkedHashMap<UUID, BigDecimal>();

        for (var item : allocations) {
            if (item.userId() == null || !eligibleSet.contains(item.userId())) {
                throw new IllegalArgumentException("Người dùng không thuộc trường hoặc không đúng vai trò: " + item.userId());
            }
            if (result.containsKey(item.userId())) {
                throw new IllegalArgumentException("Danh sách phân bổ chứa userId trùng lặp: " + item.userId());
            }
            if (item.amountVnd() == null || item.amountVnd().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Số lượng phân bổ không hợp lệ");
            }
            var used = usedQuantityOrZero(existing.get(item.userId()));
            if (item.amountVnd().compareTo(used) < 0) {
                throw new IllegalArgumentException("Không thể đặt hạn mức nhỏ hơn số lượng đã sử dụng");
            }
            result.put(item.userId(), item.amountVnd());
        }

        var sumInRequest = result.values().stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        var sumOthers = existing.entrySet().stream()
            .filter(e -> !result.containsKey(e.getKey()))
            .map(e -> orZero(e.getValue().getAllocatedAmountVnd()))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        // Vượt distributableVnd không còn bị từ chối thẳng nữa: phần vượt có thể ăn vào ví tự nạp của
        // trường (school_balances) -- CHỈ nới TRẦN, không giữ tiền, y hệt cách ConsumeQuotaService đã
        // tự động rút ví khi tiêu thật vượt pool. Đòi confirmWalletDraw vì ví đó dùng chung cho cả
        // EXAM lẫn PRACTICE, quản trị viên phải biết mình đang ăn vào phần chung đó.
        var overDistributableVnd = sumInRequest.add(sumOthers).subtract(distributableVnd);
        if (overDistributableVnd.signum() > 0) {
            if (overDistributableVnd.compareTo(walletHeadroomVnd) > 0) {
                throw new IllegalArgumentException(
                    "Tổng hạn mức phân bổ vượt quá phần được phép chia của trường lẫn số dư ví tự nạp");
            }
            if (!confirmWalletDraw) {
                throw new WalletDrawConfirmationRequiredException(overDistributableVnd, walletHeadroomVnd);
            }
        }

        return result;
    }

    private static BigDecimal usedQuantityOrZero(SchoolSubscriptionQuotaUserAllocation allocation) {
        if (allocation == null || allocation.getUsedAmountVnd() == null) {
            return BigDecimal.ZERO;
        }
        return allocation.getUsedAmountVnd();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private QuotaUserAllocationSummaryResponse buildSummary(UUID subscriptionId, QuotaType quotaType,
            SchoolSubscriptionQuotaRecord pool, List<UUID> eligibleUserIds) {
        var existing = fetchExistingAllocationsByUserId(subscriptionId, quotaType);

        // Trả về ĐỦ mọi người đủ điều kiện, kể cả người chưa được cấp gì (allocation ZERO ảo, không
        // ghi xuống DB): màn chia hạn mức phải hiện được cả người đang có 0 thì quản trị trường mới
        // biết còn ai chưa chia.
        var allocationDtos = eligibleUserIds.stream()
            .map(userId -> existing.getOrDefault(userId, new SchoolSubscriptionQuotaUserAllocation(
                subscriptionId, quotaType, userId, BigDecimal.ZERO, BigDecimal.ZERO)))
            .map(SchoolSubscriptionQuotaUserAllocationDto::toDto)
            .toList();

        return new QuotaUserAllocationSummaryResponse(SchoolSubscriptionQuotaRecordDto.toDto(pool), allocationDtos);
    }

    private Map<UUID, SchoolSubscriptionQuotaUserAllocation> fetchExistingAllocationsByUserId(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaUserAllocationRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType).stream()
            .collect(Collectors.toMap(allocation -> allocation.getUserId(), allocation -> allocation));
    }

    private List<UUID> fetchEligibleUserIds(UUID schoolId, UUID roleId) {
        var page = schoolUserRepository.findBySchoolId(
            schoolId, null, roleId, UserStatus.ACTIVE.name(), null, false, 1, MAX_ELIGIBLE_USERS_PAGE_SIZE);
        return page.content().stream().map(schoolUser -> schoolUser.getUserId()).sorted().toList();
    }

    private void requireSchoolAdminAccess(UUID schoolId) {
        if (!userContextPort.isSystemAdmin() && !schoolId.equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private SchoolSubscription findActiveSubscription(UUID schoolId) {
        return schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new NotFoundException("Trường chưa có gói subscription đang hoạt động"));
    }

    private SchoolSubscriptionQuotaRecord findPool(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));
    }

    private Role findRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò: " + roleCode));
    }
}
