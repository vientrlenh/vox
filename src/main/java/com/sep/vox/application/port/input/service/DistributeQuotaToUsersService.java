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
import com.sep.vox.application.port.input.command.AllocateUserQuotaAmountCommand;
import com.sep.vox.application.port.output.UserContextPort;
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
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
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

    public DistributeQuotaToUsersService(
            UserContextPort userContextPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository,
            SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            RoleRepository roleRepository,
            SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.roleRepository = roleRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    public QuotaUserAllocationSummaryResponse distribute(UUID schoolId, QuotaType quotaType, String roleCode,
            DistributionMode mode, List<AllocateUserQuotaAmountCommand> allocations) {
        requireSchoolAdminAccess(schoolId);

        var subscription = findActiveSubscription(schoolId);
        var pool = findPool(subscription.getId(), quotaType);
        var role = findRole(roleCode);
        var eligibleUserIds = fetchEligibleUserIds(schoolId, role.getId());
        if (eligibleUserIds.isEmpty()) {
            throw new IllegalArgumentException("Trường chưa có giáo viên/học sinh phù hợp để phân bổ hạn mức");
        }

        var existing = fetchExistingAllocationsByUserId(subscription.getId(), quotaType);

        var totalAllocated = orZero(pool.getTotalAllocatedAmountVnd());
        var targetAmounts = mode == DistributionMode.AUTO
            ? computeAutoSplit(eligibleUserIds, totalAllocated, existing)
            : computeManualAmounts(allocations, eligibleUserIds, existing, totalAllocated);

        targetAmounts.forEach((userId, amount) ->
            subscriptionQuotaUserAllocationRepository.upsertAllocation(subscription.getId(), quotaType, userId, amount));

        return buildSummary(subscription.getId(), quotaType, pool, eligibleUserIds);
    }

    public QuotaUserAllocationSummaryResponse view(UUID schoolId, QuotaType quotaType, String roleCode) {
        requireSchoolAdminAccess(schoolId);

        var subscription = findActiveSubscription(schoolId);
        var pool = findPool(subscription.getId(), quotaType);
        var role = findRole(roleCode);
        var eligibleUserIds = fetchEligibleUserIds(schoolId, role.getId());

        return buildSummary(subscription.getId(), quotaType, pool, eligibleUserIds);
    }

    private Map<UUID, BigDecimal> computeAutoSplit(List<UUID> eligibleUserIds, BigDecimal totalAllocated,
            Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing) {
        var count = eligibleUserIds.size();
        var base = totalAllocated.divide(BigDecimal.valueOf(count), 6, RoundingMode.DOWN);
        // Phần dư sau khi chia đều (không hết vì DOWN), quy đổi sang số đơn vị SMALLEST_UNIT để rải
        // cho count đầu tiên -- giữ đúng tổng totalAllocated, không "mất" hay "sinh thêm" tiền do làm tròn.
        var remainderUnits = totalAllocated.subtract(base.multiply(BigDecimal.valueOf(count)))
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
            Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing, BigDecimal totalAllocated) {
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
        if (sumInRequest.add(sumOthers).compareTo(totalAllocated) > 0) {
            throw new IllegalArgumentException("Tổng hạn mức phân bổ vượt quá hạn mức của trường");
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
