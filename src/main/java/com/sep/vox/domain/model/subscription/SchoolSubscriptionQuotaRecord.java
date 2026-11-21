package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

public class SchoolSubscriptionQuotaRecord {
    private UUID id;
    private UUID schoolSubscriptionId;
    private QuotaType quotaType;
    private BigDecimal totalAllocatedAmountVnd;
    private BigDecimal usedAmountVnd;
    /**
     * The part of {@code totalAllocatedAmountVnd} that comes from the school's self-funded wallet rather than from the
     * plan -- see V12. It always grows TOGETHER with total in one statement (addFundingFromBalance), so the two columns
     * cannot drift apart; the database has a CHECK that keeps {@code 0 <= funded <= total}.
     */
    private BigDecimal fundedFromBalanceVnd = BigDecimal.ZERO;
    /**
     * The source period from which this wallet still MUST receive unspent self-funded money -- a promise, not an
     * amount. Non-null only on a period created by an EARLY renewal, and only until the next job run. See V13.
     */
    private UUID carryFundingFromSubscriptionId;

    public SchoolSubscriptionQuotaRecord() {}




    /**
     * The quota record of a NEW period: the plan's quota plus the unspent self-funded money carried over from the old
     * period.
     *
     * <p>A named factory rather than one more overloaded constructor: the signature
     * {@code (UUID, QuotaType, BigDecimal, BigDecimal, BigDecimal)} would sit next to
     * {@code (UUID, UUID, QuotaType, BigDecimal, BigDecimal)} and the reader would have to count parameters to know which
     * one is being called.
     *
     * <p>This is also the ONLY place that can build a row that already has funded > 0, and it forces
     * {@code total = plan quota + carried} -- so the invariant {@code funded <= total} is kept by the addition itself,
     * not by the caller's promise.
     */
    public static SchoolSubscriptionQuotaRecord seeded(UUID schoolSubscriptionId, QuotaType quotaType,
            BigDecimal planIncludedAmountVnd, BigDecimal carriedFundingVnd) {
        BigDecimal included = planIncludedAmountVnd == null ? BigDecimal.ZERO : planIncludedAmountVnd;
        BigDecimal carried = carriedFundingVnd == null ? BigDecimal.ZERO : carriedFundingVnd.max(BigDecimal.ZERO);
        return builder()
            .schoolSubscriptionId(schoolSubscriptionId)
            .quotaType(quotaType)
            .totalAllocatedAmountVnd(included.add(carried))
            .usedAmountVnd(BigDecimal.ZERO)
            .fundedFromBalanceVnd(carried)
            .build();
    }

    /**
     * The quota record of a FUTURE period (early renewal): exactly the plan's quota, nothing carried over yet, plus a
     * PROMISE pointing back at the source period.
     *
     * <p>The self-funded money is DELIBERATELY left empty here. The source period is still running, can still be spent
     * and topped up until the boundary, so every number fixed at this moment may become wrong before the new period gets
     * to start -- fixing it early either doubles the money or makes it evaporate, depending on what the school does in
     * between. See V13 and {@code CarryQuotaFundingAtPeriodStartService}.
     */
    public static SchoolSubscriptionQuotaRecord seededPendingCarry(UUID schoolSubscriptionId,
            QuotaType quotaType, BigDecimal planIncludedAmountVnd, UUID carryFundingFromSubscriptionId) {
        SchoolSubscriptionQuotaRecord record = seeded(schoolSubscriptionId, quotaType, planIncludedAmountVnd, BigDecimal.ZERO);
        record.setCarryFundingFromSubscriptionId(carryFundingFromSubscriptionId);
        return record;
    }

    /**
     * The self-funded money of this period that is still UNSPENT -- the number carried over to the next period.
     *
     * <p>Convention: <b>the PLAN's money is spent first, the self-funded money last.</b> No column records which source
     * each spent dong belonged to, so a convention has to be chosen, and this one favours the school: the plan quota
     * expires at the end of the period anyway, whereas the money the school put in itself should not evaporate before it.
     * It also matches the order in which ConsumeQuotaService already spends one level up (quota first, wallet after).
     *
     * <p>Consequence: {@code min(funded, total - used)}. Spending 12m on a wallet of 15m that has 5m self-funded leaves
     * 3m -- 2m of the self-funded money has been spent, and only 3m is carried over.
     */
    public BigDecimal unspentFundedVnd() {
        BigDecimal funded = fundedFromBalanceVnd == null ? BigDecimal.ZERO : fundedFromBalanceVnd;
        BigDecimal remaining = totalAllocatedAmountVnd.subtract(usedAmountVnd);
        return funded.min(remaining).max(BigDecimal.ZERO);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolSubscriptionId() {
        return schoolSubscriptionId;
    }

    public void setSchoolSubscriptionId(UUID schoolSubscriptionId) {
        this.schoolSubscriptionId = schoolSubscriptionId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getTotalAllocatedAmountVnd() {
        return totalAllocatedAmountVnd;
    }

    public void setTotalAllocatedAmountVnd(BigDecimal totalAllocatedAmountVnd) {
        this.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
    }

    public BigDecimal getUsedAmountVnd() {
        return usedAmountVnd;
    }

    public void setUsedAmountVnd(BigDecimal usedAmountVnd) {
        this.usedAmountVnd = usedAmountVnd;
    }

    public BigDecimal getFundedFromBalanceVnd() {
        return fundedFromBalanceVnd;
    }

    public void setFundedFromBalanceVnd(BigDecimal fundedFromBalanceVnd) {
        this.fundedFromBalanceVnd = fundedFromBalanceVnd;
    }

    public UUID getCarryFundingFromSubscriptionId() {
        return carryFundingFromSubscriptionId;
    }

    public void setCarryFundingFromSubscriptionId(UUID carryFundingFromSubscriptionId) {
        this.carryFundingFromSubscriptionId = carryFundingFromSubscriptionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolSubscriptionQuotaRecord target = new SchoolSubscriptionQuotaRecord();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder schoolSubscriptionId(UUID schoolSubscriptionId) {
            target.schoolSubscriptionId = schoolSubscriptionId;
            return this;
        }

        public Builder quotaType(QuotaType quotaType) {
            target.quotaType = quotaType;
            return this;
        }

        public Builder totalAllocatedAmountVnd(BigDecimal totalAllocatedAmountVnd) {
            target.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
            return this;
        }

        public Builder usedAmountVnd(BigDecimal usedAmountVnd) {
            target.usedAmountVnd = usedAmountVnd;
            return this;
        }

        public Builder fundedFromBalanceVnd(BigDecimal fundedFromBalanceVnd) {
            target.fundedFromBalanceVnd = fundedFromBalanceVnd == null ? BigDecimal.ZERO : fundedFromBalanceVnd;
            return this;
        }

        public Builder carryFundingFromSubscriptionId(UUID carryFundingFromSubscriptionId) {
            target.carryFundingFromSubscriptionId = carryFundingFromSubscriptionId;
            return this;
        }

        public SchoolSubscriptionQuotaRecord build() {
            return target;
        }

    }
}
