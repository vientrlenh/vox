package com.sep.vox.application.port.input.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UserQuotaAmount;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuotaUserAllocationSummaryDto;
import com.sep.vox.domain.mapper.SubscriptionQuotaDtoMapper;
import com.sep.vox.domain.mapper.SubscriptionQuotaUserAllocationDtoMapper;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.DistributionMode;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionQuotaUserAllocation;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class DistributeQuotaToUsersService {

    private static final int MAX_ELIGIBLE_USERS_PAGE_SIZE = 10_000;

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final RoleRepository roleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;

    public DistributeQuotaToUsersService(
            UserContextPort userContextPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            RoleRepository roleRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository) {
        this.userContextPort = userContextPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.roleRepository = roleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
    }

    public QuotaUserAllocationSummaryDto distribute(UUID schoolId, QuotaType quotaType, String roleCode,
            DistributionMode mode, List<UserQuotaAmount> allocations) {
        requireSchoolAdminAccess(schoolId);

        var subscription = findActiveSubscription(schoolId);
        var pool = findPool(subscription.getId(), quotaType);
        var role = findRole(roleCode);
        var eligibleUserIds = fetchEligibleUserIds(schoolId, role.getId());
        if (eligibleUserIds.isEmpty()) {
            throw new IllegalArgumentException("Trường chưa có giáo viên/học sinh phù hợp để phân bổ hạn mức");
        }

        var existing = fetchExistingAllocationsByUserId(subscription.getId(), quotaType);

        var targetAmounts = mode == DistributionMode.AUTO
            ? computeAutoSplit(eligibleUserIds, pool.getTotalAllocated(), existing)
            : computeManualAmounts(allocations, eligibleUserIds, existing, pool.getTotalAllocated());

        targetAmounts.forEach((userId, amount) ->
            subscriptionQuotaUserAllocationRepository.upsertAllocation(subscription.getId(), quotaType, userId, amount));

        return buildSummary(subscription.getId(), quotaType, pool, eligibleUserIds);
    }

    public QuotaUserAllocationSummaryDto view(UUID schoolId, QuotaType quotaType, String roleCode) {
        requireSchoolAdminAccess(schoolId);

        var subscription = findActiveSubscription(schoolId);
        var pool = findPool(subscription.getId(), quotaType);
        var role = findRole(roleCode);
        var eligibleUserIds = fetchEligibleUserIds(schoolId, role.getId());

        return buildSummary(subscription.getId(), quotaType, pool, eligibleUserIds);
    }

    private Map<UUID, Integer> computeAutoSplit(List<UUID> eligibleUserIds, int totalAllocated,
            Map<UUID, SubscriptionQuotaUserAllocation> existing) {
        var count = eligibleUserIds.size();
        var base = totalAllocated / count;
        var remainder = totalAllocated % count;

        var result = new LinkedHashMap<UUID, Integer>();
        for (int i = 0; i < count; i++) {
            var userId = eligibleUserIds.get(i);
            var amount = base + (i < remainder ? 1 : 0);
            var used = existing.containsKey(userId) ? existing.get(userId).getUsedQuantity() : 0;
            if (amount < used) {
                throw new IllegalArgumentException(
                    "Không thể chia đều vì có người dùng đã sử dụng vượt mức chia mới, hãy dùng chế độ thủ công");
            }
            result.put(userId, amount);
        }
        return result;
    }

    private Map<UUID, Integer> computeManualAmounts(List<UserQuotaAmount> allocations, List<UUID> eligibleUserIds,
            Map<UUID, SubscriptionQuotaUserAllocation> existing, int totalAllocated) {
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("Danh sách phân bổ không được để trống");
        }

        var eligibleSet = new HashSet<>(eligibleUserIds);
        var result = new LinkedHashMap<UUID, Integer>();

        for (var item : allocations) {
            if (item.userId() == null || !eligibleSet.contains(item.userId())) {
                throw new IllegalArgumentException("Người dùng không thuộc trường hoặc không đúng vai trò: " + item.userId());
            }
            if (result.containsKey(item.userId())) {
                throw new IllegalArgumentException("Danh sách phân bổ chứa userId trùng lặp: " + item.userId());
            }
            if (item.amount() == null || item.amount() < 0) {
                throw new IllegalArgumentException("Số lượng phân bổ không hợp lệ");
            }
            var used = existing.containsKey(item.userId()) ? existing.get(item.userId()).getUsedQuantity() : 0;
            if (item.amount() < used) {
                throw new IllegalArgumentException("Không thể đặt hạn mức nhỏ hơn số lượng đã sử dụng");
            }
            result.put(item.userId(), item.amount());
        }

        var sumInRequest = result.values().stream().mapToInt(Integer::intValue).sum();
        var sumOthers = existing.entrySet().stream()
            .filter(e -> !result.containsKey(e.getKey()))
            .mapToInt(e -> e.getValue().getAllocatedQuantity())
            .sum();
        if (sumInRequest + sumOthers > totalAllocated) {
            throw new IllegalArgumentException("Tổng hạn mức phân bổ vượt quá hạn mức của trường");
        }

        return result;
    }

    private QuotaUserAllocationSummaryDto buildSummary(UUID subscriptionId, QuotaType quotaType,
            SubscriptionQuota pool, List<UUID> eligibleUserIds) {
        var existing = fetchExistingAllocationsByUserId(subscriptionId, quotaType);
        var names = userRepository.findByIdIn(eligibleUserIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u.getFullName().value()));

        var allocationDtos = eligibleUserIds.stream()
            .map(userId -> {
                var allocation = existing.getOrDefault(userId,
                    new SubscriptionQuotaUserAllocation(subscriptionId, quotaType, userId, 0, 0));
                return SubscriptionQuotaUserAllocationDtoMapper.toDto(allocation, names.get(userId));
            })
            .toList();

        return new QuotaUserAllocationSummaryDto(SubscriptionQuotaDtoMapper.toDto(pool), allocationDtos);
    }

    private Map<UUID, SubscriptionQuotaUserAllocation> fetchExistingAllocationsByUserId(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaUserAllocationRepository.findAllBySubscriptionIdAndQuotaType(subscriptionId, quotaType).stream()
            .collect(Collectors.toMap(SubscriptionQuotaUserAllocation::getUserId, Function.identity()));
    }

    private List<UUID> fetchEligibleUserIds(UUID schoolId, UUID roleId) {
        var page = schoolUserRepository.findBySchoolId(schoolId, null, roleId, UserStatus.ACTIVE.name(), 1, MAX_ELIGIBLE_USERS_PAGE_SIZE);
        return page.content().stream().map(SchoolUser::getUserId).sorted().toList();
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

    private SubscriptionQuota findPool(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));
    }

    private Role findRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò: " + roleCode));
    }
}
