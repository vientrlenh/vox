package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.domain.model.school.SchoolUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.AllocateUserQuotaAmountCommand;
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
@RequiredArgsConstructor
public class DistributeQuotaToUsersService {

    private static final int MAX_ELIGIBLE_USERS_PAGE_SIZE = 10_000;

    // Smallest unit at the exact numeric(18,6) scale of the quota column (see V22__quota_unit_to_usd.sql,
    // precision widened in V27__widen_quota_columns_precision.sql, scale 6 unchanged).
    // Used to spread the remainder after an even split -- the equivalent of "1 second" in the old integer split logic, now
    // "one millionth of a dollar", so totalAllocated divides fully without rounding money away.
    private static final BigDecimal SMALLEST_UNIT = new BigDecimal("0.000001");

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final RoleRepository roleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;

    public QuotaUserAllocationSummaryResponse distribute(UUID schoolId, QuotaType quotaType, String roleCode,
            DistributionMode mode, List<AllocateUserQuotaAmountCommand> allocations, boolean confirmWalletDraw) {
        requireSchoolAdminAccess(schoolId);

        SchoolSubscription subscription = findActiveSubscription(schoolId);
        SchoolSubscriptionQuotaRecord pool = findPool(subscription.getId(), quotaType);
        Role role = findRole(roleCode);
        List<UUID> eligibleUserIds = requireEligibleUserIds(schoolId, role.getId());

        Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing = fetchExistingAllocationsByUserId(subscription.getId(), quotaType);
        BigDecimal distributableVnd = distributableAmountVnd(schoolId, quotaType, pool);

        Map<UUID, BigDecimal> targetAmounts = mode == DistributionMode.AUTO
            ? computeAutoSplit(eligibleUserIds, distributableVnd, existing)
            : computeManualAmounts(allocations, eligibleUserIds, existing, distributableVnd,
                walletHeadroomVnd(schoolId), confirmWalletDraw);

        saveAllocations(subscription.getId(), quotaType, targetAmounts);

        return buildSummary(subscription.getId(), quotaType, pool, eligibleUserIds);
    }

    private List<UUID> requireEligibleUserIds(UUID schoolId, UUID roleId) {
        List<UUID> eligibleUserIds = fetchEligibleUserIds(schoolId, roleId);
        if (eligibleUserIds.isEmpty()) {
            throw new IllegalArgumentException("Trường chưa có giáo viên/học sinh phù hợp để phân bổ hạn mức");
        }
        return eligibleUserIds;
    }

    // The distribution ceiling applies to BOTH modes, not only the even split: if it only blocked AUTO, the school
    // administrator would walk around it by editing people by hand one at a time -- and manual editing is exactly the
    // usual path since the quota distribution screen moved to a per-person dialog. A ceiling that can be walked
    // around is not a ceiling.
    private BigDecimal distributableAmountVnd(UUID schoolId, QuotaType quotaType, SchoolSubscriptionQuotaRecord pool) {
        SchoolQuotaPolicy policy = schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, quotaType);
        return policy.distributableAmountOf(orZero(pool.getTotalAllocatedAmountVnd()));
    }

    private void saveAllocations(UUID subscriptionId, QuotaType quotaType, Map<UUID, BigDecimal> targetAmounts) {
        targetAmounts.forEach((userId, amount) ->
            subscriptionQuotaUserAllocationRepository.upsertAllocation(subscriptionId, quotaType, userId, amount));
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
     * ONE PAGE of the quota distribution list, with the total already distributed over the whole set.
     *
     * <p>This is the ONLY read path of the quota distribution screen. The previous unpaged version gathered every
     * eligible person in one pass (up to {@value #MAX_ELIGIBLE_USERS_PAGE_SIZE} people), which for a school with a few
     * thousand students means a few thousand rows every time the page opens -- it was dropped together with the two
     * REST endpoints that called it.
     *
     * <p>Paging and search are pushed all the way down to the user query, not cut in memory: cutting at the Java
     * layer still has to read everybody first, so it saves nothing.
     */
    public QuotaUserAllocationPageResponse viewPage(
            UUID schoolId, QuotaType quotaType, String roleCode, String search, int page, int size) {
        requireSchoolAdminAccess(schoolId);

        SchoolSubscription subscription = findActiveSubscription(schoolId);
        SchoolSubscriptionQuotaRecord pool = findPool(subscription.getId(), quotaType);
        Role role = findRole(roleCode);

        PageResult<SchoolUser> userPage = schoolUserRepository.findBySchoolId(
            schoolId, blankToNull(search), role.getId(), UserStatus.ACTIVE.name(), null, false, page, size);

        return buildAllocationPage(subscription.getId(), quotaType, pool, schoolId, role.getId(), userPage);
    }

    private QuotaUserAllocationPageResponse buildAllocationPage(UUID subscriptionId, QuotaType quotaType,
            SchoolSubscriptionQuotaRecord pool, UUID schoolId, UUID roleId, PageResult<SchoolUser> userPage) {
        Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing = fetchExistingAllocationsByUserId(subscriptionId, quotaType);
        List<QuotaUserAllocationPageResponse.Row> rows = toAllocationRows(userPage, existing);
        BigDecimal distributed = distributedToEligibleUsers(subscriptionId, quotaType, schoolId, roleId);
        BigDecimal orphaned = orphanedAllocation(subscriptionId, quotaType, distributed);
        SchoolQuotaPolicy policy = schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, quotaType);

        return new QuotaUserAllocationPageResponse(
            SchoolSubscriptionQuotaRecordDto.toDto(pool),
            distributed,
            orphaned,
            policy.getDistributableRatio(),
            policy.distributableAmountOf(orZero(pool.getTotalAllocatedAmountVnd())),
            spendableFundsVnd(pool, schoolId),
            walletHeadroomVnd(schoolId),
            rows,
            userPage.page(),
            userPage.size(),
            userPage.totalElements(),
            userPage.totalPages()
        );
    }

    // null (not 0) for people who have NO allocation row yet: those two states behave oppositely at the
    // blocking gate, so merging them into the same 0 hides the information the school administrator needs most
    // -- see the javadoc of Row.
    private List<QuotaUserAllocationPageResponse.Row> toAllocationRows(
            PageResult<SchoolUser> userPage, Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing) {
        return userPage.content().stream()
            .map(schoolUser -> schoolUser.getUserId())
            .map(userId -> {
                SchoolSubscriptionQuotaUserAllocation allocation = existing.get(userId);
                return new QuotaUserAllocationPageResponse.Row(
                    userId,
                    allocation == null ? null : orZero(allocation.getAllocatedAmountVnd()),
                    allocation == null ? BigDecimal.ZERO : orZero(allocation.getUsedAmountVnd()));
            })
            .toList();
    }

    // Total over the WHOLE set, not the page being viewed -- the UI uses it to know how much can still be
    // distributed, and adding up the column of one page gives a wrong number from page two on.
    //
    // Summed in the DB rather than summing `existing` in memory: the sum must exclude people who are no longer
    // eligible (see sumAllocatedForEligibleUsers), and filtering that at the Java layer would require loading the
    // school's whole eligible list first -- exactly what paging this screen was created to avoid.
    private BigDecimal distributedToEligibleUsers(UUID subscriptionId, QuotaType quotaType, UUID schoolId, UUID roleId) {
        return subscriptionQuotaUserAllocationRepository.sumAllocatedForEligibleUsers(
            subscriptionId, quotaType, schoolId, roleId, UserStatus.ACTIVE.name());
    }

    private BigDecimal orphanedAllocation(UUID subscriptionId, QuotaType quotaType, BigDecimal distributed) {
        return subscriptionQuotaUserAllocationRepository
            .sumAllocated(subscriptionId, quotaType)
            .subtract(distributed)
            .max(BigDecimal.ZERO);
    }

    /**
     * Số tiền trường THẬT SỰ còn trả được cho loại hạn mức này = hạn mức kèm gói còn lại + số dư ví
     * tự nạp (âm kẹp về 0, vì âm là nợ và nợ đã bị chặn ở một cửa riêng).
     *
     * <p>Sinh ra để trả lời một câu mà trần phân phối KHÔNG trả lời được. Trần tính trên
     * {@code total_allocated} và không hề nhỏ đi khi ví bị tiêu, nên một trường đã tiêu cạn ví vẫn
     * chia tiếp được tới hết trần mà không có gì cản -- rồi giáo viên bị chặn lúc bấm mở kỳ thi, học
     * sinh bị chặn giữa buổi luyện, và cả hai đều vừa được báo là mình còn hạn mức.
     *
     * <p>CỐ Ý chỉ để cảnh báo, không đem chặn ở đây: trần chi cá nhân là một giới hạn nội bộ, ổn định
     * suốt kỳ, chứ không phải một phép giữ tiền. Nếu lấy nó làm điều kiện chia thì trần của mọi người
     * sẽ co lại mỗi khi có ai đó tiêu, và quản trị trường phải chia lại liên tục trong kỳ. Việc CHẶN
     * vẫn nằm đúng chỗ cũ, ngay trước lúc tốn tiền: ClassTestTokenQuotaGuardService cho đường thi,
     * findPracticeSpendableFundsVnd cho đường luyện.
     *
     * <p>Công thức phải khớp TỪNG ĐỒNG với {@code ClassTestTokenQuotaGuardService.spendableSchoolFundsVnd}
     * -- một cảnh báo lệch khỏi cửa chặn thật thì hoặc dọa người dùng về khoản vẫn chi được, hoặc để
     * họ yên tâm về khoản sắp bị từ chối. Ba nơi đang chép lại cùng công thức này (thêm
     * {@code SchoolFundingResponse}); gom về một chỗ là việc đáng làm khi có dịp sửa cả ba.
     */
    private BigDecimal spendableFundsVnd(SchoolSubscriptionQuotaRecord pool, UUID schoolId) {
        return orZero(pool.getTotalAllocatedAmountVnd())
            .subtract(orZero(pool.getUsedAmountVnd()))
            .add(walletHeadroomVnd(schoolId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Chia đều TOÀN BỘ phần được phép chia cho những người đủ điều kiện, ghi đè mọi phân bổ cũ của họ.
     *
     * <p>Không cần trừ đi phần của người KHÔNG còn đủ điều kiện, và đó là hệ quả trực tiếp của việc
     * sumOthers/distributedAmountVnd chỉ đếm người đủ điều kiện: sau lượt chia này, tổng trên tập
     * được đếm đúng bằng {@code distributableVnd}, không dư một đồng.
     *
     * <p>Trước khi có phép lọc đó thì chính hàm này đẩy trường vượt trần của chính mình -- nó phát
     * hết phần được phép chia cho người đủ điều kiện, trong khi dòng của người đã nghỉ vẫn được cộng
     * vào tổng, nên ngay sau một lần "chia đều tự động" là mọi lần sửa tay đều bị từ chối.
     */
    private Map<UUID, BigDecimal> computeAutoSplit(List<UUID> eligibleUserIds, BigDecimal distributableVnd,
            Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing) {
        int count = eligibleUserIds.size();
        BigDecimal base = distributableVnd.divide(BigDecimal.valueOf(count), 6, RoundingMode.DOWN);
        // Phần dư sau khi chia đều (không hết vì DOWN), quy đổi sang số đơn vị SMALLEST_UNIT để rải
        // cho count đầu tiên -- giữ đúng tổng phần được chia, không "mất" hay "sinh thêm" tiền do làm tròn.
        int remainderUnits = distributableVnd.subtract(base.multiply(BigDecimal.valueOf(count)))
            .divide(SMALLEST_UNIT, 0, RoundingMode.DOWN)
            .intValueExact();

        Map<UUID, BigDecimal> result = new LinkedHashMap<UUID, BigDecimal>();
        for (int i = 0; i < count; i++) {
            UUID userId = eligibleUserIds.get(i);
            BigDecimal amount = i < remainderUnits ? base.add(SMALLEST_UNIT) : base;
            BigDecimal used = usedQuantityOrZero(existing.get(userId));
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

        Set<UUID> eligibleSet = new HashSet<>(eligibleUserIds);
        Map<UUID, BigDecimal> result = collectRequestedAmounts(allocations, eligibleSet, existing);

        BigDecimal sumInRequest = result.values().stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal sumOthers = sumAllocatedToOtherEligibleUsers(existing, result, eligibleSet);

        requireWithinDistributableOrConfirmedWalletDraw(
            sumInRequest.add(sumOthers).subtract(distributableVnd), walletHeadroomVnd, confirmWalletDraw);

        return result;
    }

    private Map<UUID, BigDecimal> collectRequestedAmounts(List<AllocateUserQuotaAmountCommand> allocations,
            Set<UUID> eligibleSet, Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing) {
        Map<UUID, BigDecimal> result = new LinkedHashMap<UUID, BigDecimal>();

        for (AllocateUserQuotaAmountCommand item : allocations) {
            if (item.userId() == null || !eligibleSet.contains(item.userId())) {
                throw new IllegalArgumentException("Người dùng không thuộc trường hoặc không đúng vai trò: " + item.userId());
            }
            if (result.containsKey(item.userId())) {
                throw new IllegalArgumentException("Danh sách phân bổ chứa userId trùng lặp: " + item.userId());
            }
            if (item.amountVnd() == null || item.amountVnd().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Số lượng phân bổ không hợp lệ");
            }
            BigDecimal used = usedQuantityOrZero(existing.get(item.userId()));
            if (item.amountVnd().compareTo(used) < 0) {
                throw new IllegalArgumentException("Không thể đặt hạn mức nhỏ hơn số lượng đã sử dụng");
            }
            result.put(item.userId(), item.amountVnd());
        }
        return result;
    }

    // Only sums the share of people who are STILL eligible. Allocation rows are not deleted when a teacher leaves or
    // a student graduates, so counting those rows would let an amount nobody can reach occupy room under the ceiling --
    // and the school owner sees it on no page of the table, so cannot reclaim it. Enough such rows and every even
    // split is rejected with an error message that matches nothing in the numbers shown on screen.
    //
    // Same filter as fetchEligibleUserIds, and also exactly the set that viewPage sums to get distributedAmountVnd --
    // both sides must count the same set, otherwise the number the UI uses to compute "still distributable" would not
    // be the number the backend rejects with.
    private BigDecimal sumAllocatedToOtherEligibleUsers(Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing,
            Map<UUID, BigDecimal> requestedAmounts, Set<UUID> eligibleSet) {
        return existing.entrySet().stream()
            .filter(e -> !requestedAmounts.containsKey(e.getKey()))
            .filter(e -> eligibleSet.contains(e.getKey()))
            .map(e -> orZero(e.getValue().getAllocatedAmountVnd()))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    // Exceeding distributableVnd is no longer rejected outright: the excess may eat into the school's self-topped-up wallet (school_balances) --
    // it only loosens the CEILING and holds no money, exactly like ConsumeQuotaService already
    // draws from the wallet automatically when real spending exceeds the pool. confirmWalletDraw is required because that wallet is shared by both
    // ASSESSMENT and PRACTICE, and the administrator must know they are eating into that shared part.
    private void requireWithinDistributableOrConfirmedWalletDraw(BigDecimal overDistributableVnd,
            BigDecimal walletHeadroomVnd, boolean confirmWalletDraw) {
        if (overDistributableVnd.signum() > 0) {
            if (overDistributableVnd.compareTo(walletHeadroomVnd) > 0) {
                throw new IllegalArgumentException(
                    "Tổng hạn mức phân bổ vượt quá phần được phép chia của trường lẫn số dư ví tự nạp");
            }
            if (!confirmWalletDraw) {
                throw new BusinessRuleViolationException(
                    walletDrawConfirmationMessage(overDistributableVnd, walletHeadroomVnd));
            }
        }
    }

    private static String walletDrawConfirmationMessage(BigDecimal walletDrawVnd, BigDecimal walletBalanceVnd) {
        return String.format(
            "Phân bổ này vượt phần chia từ gói, sẽ trích thêm %s từ ví tự nạp của trường (đang còn %s). "
                + "Xác nhận để tiếp tục.",
            formatVnd(walletDrawVnd), formatVnd(walletBalanceVnd));
    }

    private static String formatVnd(BigDecimal amountVnd) {
        return amountVnd.toPlainString() + "đ";
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
        Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing = fetchExistingAllocationsByUserId(subscriptionId, quotaType);

        // Return EVERY eligible person, including those who have been granted nothing (a virtual ZERO allocation, not written to
        // the DB): the quota distribution screen must also show people who currently have 0 so that the school admin can see who
        // has not been given a share yet.
        List<SchoolSubscriptionQuotaUserAllocationDto> allocationDtos = eligibleUserIds.stream()
            .map(userId -> existing.getOrDefault(userId, SchoolSubscriptionQuotaUserAllocation.unallocated(subscriptionId, quotaType, userId)))
            .map(SchoolSubscriptionQuotaUserAllocationDto::toDto)
            .toList();

        return new QuotaUserAllocationSummaryResponse(SchoolSubscriptionQuotaRecordDto.toDto(pool), allocationDtos);
    }

    private Map<UUID, SchoolSubscriptionQuotaUserAllocation> fetchExistingAllocationsByUserId(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaUserAllocationRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType).stream()
            .collect(Collectors.toMap(allocation -> allocation.getUserId(), allocation -> allocation));
    }

    private List<UUID> fetchEligibleUserIds(UUID schoolId, UUID roleId) {
        PageResult<SchoolUser> page = schoolUserRepository.findBySchoolId(
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
            .orElseThrow(() -> new ResourceNotFoundException("Trường chưa có gói subscription đang hoạt động"));
    }

    private SchoolSubscriptionQuotaRecord findPool(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạn mức của gói đăng ký"));
    }

    private Role findRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò: " + roleCode));
    }
}
