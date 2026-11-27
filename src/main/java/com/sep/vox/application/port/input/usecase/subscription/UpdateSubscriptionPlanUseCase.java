package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanQuotaCommand;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanQuotaAmountValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class UpdateSubscriptionPlanUseCase implements IUseCase<UpdateSubscriptionPlanCommand, UUID> {

    // Column subscription_plans.price_vnd is numeric(15,0), the same width as orders.total_amount_vnd.
    private static final BigDecimal MAX_PRICE_VND = new BigDecimal("999999999999999");

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_TAGLINE_LENGTH = 2048;

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private final SubscriptionPlanQuotaAmountValidator quotaAmountValidator;

    @Override
    @Transactional
    public UUID execute(UpdateSubscriptionPlanCommand input) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        requireDraft(plan);
        PlanEdits edits = validatedEdits(input);
        applyEdits(plan, input, edits);
        SubscriptionPlan savedPlan = saveKeepingVersion(plan);
        replaceQuotasIfGiven(savedPlan.getId(), edits.includedAmountByQuotaType());

        // Quota is not part of SubscriptionPlanDto -- a GraphQL client that needs it selects the quotas field, and the resolver
        // loads it through the subscriptionPlanQuotasBySubscriptionPlanId DataLoader.
        return savedPlan.getId();
    }

    private record PlanEdits(
        @Nullable String name,
        @Nullable String tagline,
        @Nullable Map<QuotaType, BigDecimal> includedAmountByQuotaType
    ) {
    }

    // A published plan (ACTIVE/ARCHIVED) is locked against editing entirely, even fields unrelated to money/quota: renewal
    // (CreatePaymentLinkForRenewalUseCase / InvoiceSettlementService) reads the price and quota LIVE from this very plan, with
    // no snapshot -- editing in place would silently change the price/quota for a school that is using/renewing without any
    // confirmation step. It can only be edited while DRAFT (not published, so no school uses it yet); to change a published
    // plan, archive it and create a new plan (with replacedByPlanId).
    private void requireDraft(SubscriptionPlan plan) {
        if (plan.getStatus() != SubscriptionPlanStatus.DRAFT) {
            throw new IllegalArgumentException(
                "A plan can only be edited while it is in DRAFT status. A published plan must be archived and replaced by a new plan.");
        }
    }

    // Validate ALL input before touching the plan: if an error is thrown halfway, the transaction rolls back but the plan
    // object in memory has already been half-edited, and the quota set is deleted and rewritten below -- gathering the
    // checks at the top ensures there is never a half-edited state.
    private PlanEdits validatedEdits(UpdateSubscriptionPlanCommand input) {
        String name = normalizedName(input.name());
        String tagline = normalizedTagline(input.tagline());
        validatePriceVnd(input.priceVnd());
        validatePeriodCount(input.periodCount());
        validateMaxTimePerAttemptMin(input.maxTimePerAttemptMin());
        return new PlanEdits(name, tagline, validatedQuotas(input));
    }

    private void applyEdits(SubscriptionPlan plan, UpdateSubscriptionPlanCommand input, PlanEdits edits) {
        if (edits.name() != null) {
            plan.setName(edits.name());
        }
        if (edits.tagline() != null) {
            plan.setTagline(edits.tagline());
        }
        if (input.priceVnd() != null) {
            plan.setPriceVnd(input.priceVnd());
        }
        if (input.periodCount() != null) {
            plan.setPeriodCount(input.periodCount());
        }
        if (input.maxTimePerAttemptMin() != null) {
            plan.setMaxTimePerAttemptMin(input.maxTimePerAttemptMin());
        }
    }

    // Do NOT increment the version here: the version column is managed by Hibernate through @Version. The mapper builds a NEW
    // entity on every save so the entity is always detached, save() goes through merge, and merge COMPARES the entity's version
    // with the version in the DB -- incrementing it to N+1 while the DB still holds N makes every edit look like an overwrite of
    // an older copy and throws OptimisticLockException.
    private SubscriptionPlan saveKeepingVersion(SubscriptionPlan plan) {
        return subscriptionPlanRepository.save(plan);
    }

    private void replaceQuotasIfGiven(UUID planId, @Nullable Map<QuotaType, BigDecimal> includedAmountByQuotaType) {
        if (includedAmountByQuotaType != null) {
            // Replace the WHOLE quota set instead of editing row by row: a quota row has no business key other than (plan, quotaType),
            // and the plan is still DRAFT so nothing references their ids yet -- deleting everything and writing again is the only
            // way to express "drop this quota type entirely".
            subscriptionPlanQuotaRepository.deleteBySubscriptionPlanId(planId);
            List<SubscriptionPlanQuota> quotas = includedAmountByQuotaType.entrySet().stream()
                .map(entry -> SubscriptionPlanQuota.forPlan(planId, entry.getKey(), entry.getValue()))
                .toList();
            subscriptionPlanQuotaRepository.saveAll(quotas);
        }
    }

    /**
     * The plan name goes straight onto the sales page, so extra whitespace has to be collapsed: "Basic  Plan " and
     * "Basic Plan" are the same plan, and left as they are an admin looking at the list would think there are two
     * different plans.
     *
     * <p>An empty/all-whitespace string is rejected rather than silently ignored: the name column is NOT NULL and
     * the sender clearly INTENDS to edit the name -- treating it as "no edit" would make them think the name was cleared.
     */
    private static String normalizedName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = StringNormalization.trimAndCollapseSpaces(name);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("The plan name must not be empty");
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("The plan name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return normalized;
    }

    private static String normalizedTagline(String tagline) {
        if (tagline == null) {
            return null;
        }
        String normalized = StringNormalization.trimAndCollapseSpaces(tagline);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("The plan description must not be empty");
        }
        if (normalized.length() > MAX_TAGLINE_LENGTH) {
            throw new IllegalArgumentException("The plan description must not exceed " + MAX_TAGLINE_LENGTH + " characters");
        }
        return normalized;
    }

    /**
     * price_vnd is numeric(15,0) -- SCALE 0, not rounding for looks. Sending 100000.5 down makes Postgres ROUND
     * SILENTLY to 100001: the plan is listed at one price and charged at another, with no error shown. Rejected here
     * so the admin knows they just typed an extra decimal part.
     */
    private static void validatePriceVnd(BigDecimal priceVnd) {
        if (priceVnd == null) {
            return;
        }
        if (priceVnd.signum() < 0) {
            throw new IllegalArgumentException("The plan price must not be negative");
        }
        if (priceVnd.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("The plan price must be a whole VND amount with no decimal part");
        }
        if (priceVnd.compareTo(MAX_PRICE_VND) > 0) {
            throw new IllegalArgumentException("The plan price exceeds the allowed limit (maximum 999,999,999,999,999 VND)");
        }
    }

    // Matches the CHECK chk_subscription_plans_period_count_positive in the DB -- rejected here to give a readable
    // message instead of a ConstraintViolationException from the JDBC layer.
    private static void validatePeriodCount(Integer periodCount) {
        if (periodCount != null && periodCount < 1) {
            throw new IllegalArgumentException("The number of plan cycles must be greater than 0");
        }
    }

    // Matches the CHECK chk_subscription_plans_max_time_per_attempt_min_positive in the DB.
    private static void validateMaxTimePerAttemptMin(Integer maxTimePerAttemptMin) {
        if (maxTimePerAttemptMin != null && maxTimePerAttemptMin < 1) {
            throw new IllegalArgumentException("The number of minutes of a test must be greater than 0");
        }
    }

    /**
     * Returns null when the command does not touch the quota, so the caller can tell "keep as is" from "replace with a
     * new set". LinkedHashMap so quota rows are saved in the order the admin entered them, and the map itself is the
     * duplicate detector so no separate Set is needed.
     */
    private @Nullable Map<QuotaType, BigDecimal> validatedQuotas(UpdateSubscriptionPlanCommand input) {
        if (input.quotas() == null) {
            return null;
        }
        requireAtLeastOneQuota(input.quotas());
        return collectIncludedAmounts(input.quotas());
    }

    // An empty list is NOT "delete everything": a plan with no quota at all is useless to a school that buys it, and
    // CreateSubscriptionPlanUseCase already requires at least one quota. To leave the quota untouched, omit the quotas
    // field from the input entirely.
    private static void requireAtLeastOneQuota(List<UpdateSubscriptionPlanQuotaCommand> quotas) {
        if (quotas.isEmpty()) {
            throw new IllegalArgumentException("A plan must have at least one quota");
        }
    }

    private Map<QuotaType, BigDecimal> collectIncludedAmounts(List<UpdateSubscriptionPlanQuotaCommand> quotas) {
        Map<QuotaType, BigDecimal> includedAmountByQuotaType = new LinkedHashMap<QuotaType, BigDecimal>();
        for (UpdateSubscriptionPlanQuotaCommand quota : quotas) {
            QuotaType quotaType = quota.quotaType();
            // subscription_plan_quotas does NOT have a unique (subscription_plan_id, quota_type) yet, so the DB does not
            // block it: sending two GRADING rows would save both, and whichever row every later quota read picks up
            // depends on the order Postgres returns them in.
            if (includedAmountByQuotaType.containsKey(quotaType)) {
                throw new IllegalArgumentException(
                    "Quota type \"" + quotaType + "\" is declared more than once in the same plan");
            }
            quotaAmountValidator.requireValidIncludedAmount(quotaType, quota.includedAmountVnd());
            includedAmountByQuotaType.put(quotaType, quota.includedAmountVnd());
        }
        return includedAmountByQuotaType;
    }
}
