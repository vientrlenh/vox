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
            throw new IllegalArgumentException("The school has no suitable teachers/students to allocate quota to");
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
     * The part of the self-funded balance that CAN be taken in addition to {@code distributableVnd} when loosening the personal cap of ONE
     * person -- not a dedicated pocket, only a soft-ceiling cap exactly like how the pool
     * works (see ConsumeQuotaService.chargeOverage, where the money is actually deducted). Clamped to 0: negative is the
     * school's debt, not an extra share.
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
     * The amount the school can REALLY still pay for this quota type = remaining plan quota + self-funded balance
     * (negative clamped to 0, because negative is debt and debt is already blocked at a separate gate).
     *
     * <p>Created to answer a question that the distribution cap CANNOT answer. The cap is computed on
     * {@code total_allocated} and does not shrink when the balance is spent, so a school that has spent its balance down to nothing can still
     * keep distributing up to the cap with nothing stopping it -- then teachers get blocked when they open an assessment, students
     * get blocked mid-practice, and both were just told they still have quota.
     *
     * <p>DELIBERATELY only a warning, not used as a block here: the personal spending cap is an internal limit, stable
     * throughout the period, not a hold on money. If it were a condition for distributing, everyone's cap
     * would shrink every time someone spends, and the school admin would have to redistribute constantly during the period. The BLOCKING
     * stays where it was, right before money is spent: ClassTestTokenQuotaGuardService for the assessment path,
     * findPracticeSpendableFundsVnd for the practice path.
     *
     * <p>The formula must match {@code ClassTestTokenQuotaGuardService.spendableSchoolFundsVnd} TO THE DONG
     * -- a warning that deviates from the real blocking gate either scares the user about an amount that can still be spent, or
     * reassures them about an amount about to be rejected. Three places currently copy this same formula (add
     * {@code SchoolFundingResponse}); consolidating them into one is worth doing when all three are being edited anyway.
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
     * Splits the WHOLE distributable amount evenly among the eligible people, overwriting all their previous allocations.
     *
     * <p>No need to subtract the share of people who are NO LONGER eligible, which is a direct consequence of
     * sumOthers/distributedAmountVnd counting only eligible people: after this split, the total over the
     * counted set equals {@code distributableVnd} exactly, with no dong left over.
     *
     * <p>Before that filter existed, this very function pushed the school over its own cap -- it handed
     * out the whole distributable amount to the eligible people while the rows of people who had left were still added
     * to the total, so right after an "automatic even split" every manual edit was rejected.
     */
    private Map<UUID, BigDecimal> computeAutoSplit(List<UUID> eligibleUserIds, BigDecimal distributableVnd,
            Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing) {
        int count = eligibleUserIds.size();
        BigDecimal base = distributableVnd.divide(BigDecimal.valueOf(count), 6, RoundingMode.DOWN);
        // The remainder after an even split (not exhausted because of rounding DOWN), converted into SMALLEST_UNIT units to spread
        // over the first count people -- keeps the distributed total exact, no money "lost" or "created" by rounding.
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
                    "Cannot split evenly because some users have already used more than the new share; use manual mode");
            }
            result.put(userId, amount);
        }
        return result;
    }

    private Map<UUID, BigDecimal> computeManualAmounts(List<AllocateUserQuotaAmountCommand> allocations, List<UUID> eligibleUserIds,
            Map<UUID, SchoolSubscriptionQuotaUserAllocation> existing, BigDecimal distributableVnd,
            BigDecimal walletHeadroomVnd, boolean confirmWalletDraw) {
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("The allocation list must not be empty");
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
                throw new IllegalArgumentException("The user does not belong to the school or does not have the right role: " + item.userId());
            }
            if (result.containsKey(item.userId())) {
                throw new IllegalArgumentException("The allocation list contains a duplicate userId: " + item.userId());
            }
            if (item.amountVnd() == null || item.amountVnd().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("The allocation amount is invalid");
            }
            BigDecimal used = usedQuantityOrZero(existing.get(item.userId()));
            if (item.amountVnd().compareTo(used) < 0) {
                throw new IllegalArgumentException("Cannot set a quota lower than the amount already used");
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
                    "The total allocated quota exceeds the distributable share of the school and the self-funded balance");
            }
            if (!confirmWalletDraw) {
                throw new BusinessRuleViolationException(
                    walletDrawConfirmationMessage(overDistributableVnd, walletHeadroomVnd));
            }
        }
    }

    private static String walletDrawConfirmationMessage(BigDecimal walletDrawVnd, BigDecimal walletBalanceVnd) {
        return String.format(
            "This allocation exceeds the share from the plan and will draw an extra %s from the school's self-funded balance (%s remaining). "
                + "Confirm to continue.",
            formatVnd(walletDrawVnd), formatVnd(walletBalanceVnd));
    }

    private static String formatVnd(BigDecimal amountVnd) {
        return amountVnd.toPlainString() + " VND";
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
            throw new ForbiddenException("Access denied");
        }
    }

    private SchoolSubscription findActiveSubscription(UUID schoolId) {
        return schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("The school has no active subscription"));
    }

    private SchoolSubscriptionQuotaRecord findPool(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription quota not found"));
    }

    private Role findRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleCode));
    }
}
