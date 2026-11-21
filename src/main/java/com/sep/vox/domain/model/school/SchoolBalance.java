package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The school's SELF-TOPPED-UP money wallet. It ONLY holds money the school paid in, not the plan quota --
 * the plan quota lives in school_subscription_quota_records, split by QuotaType and with its own
 * used counter.
 *
 * <p>The two are deliberately NOT merged into one number: a plan grants quota by KIND (ASSESSMENT /
 * PRACTICE), so a single school-level balance cannot express "300,000 left but
 * not usable for grading attempts". Merging them silently erases the limit by kind.
 *
 * <p>This wallet may only be touched WHEN the quota of the corresponding kind is exhausted -- see ConsumeQuotaService.
 */
public class SchoolBalance {
    private UUID id;
    private UUID schoolId;
    // No CHECK >= 0 at the database level: the negative part here IS the debt, which arises when a REAL charge (the exam
    // path, see ConsumeQuotaService) exceeds both the plan's quota and the balance that was topped up.
    private BigDecimal balanceVnd;
    private Instant createdAt;
    private Instant updatedAt;
    // Must be carried on the domain model, not only on the JpaEntity: the mapper builds a NEW entity on every save, so the
    // entity is always detached -- without the version, Hibernate treats it as transient and INSERTs over the existing id.
    private Long version;

    public SchoolBalance() {}



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public BigDecimal getBalanceVnd() {
        return balanceVnd;
    }

    public void setBalanceVnd(BigDecimal balanceVnd) {
        this.balanceVnd = balanceVnd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /** Debt = negative balance. The school can still use the service (if the QuotaType allows going into debt) but must top up. */
    public boolean isInDebt() {
        return balanceVnd.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * An empty wallet for a school that has never topped up. Created the first time the wallet is touched rather than
     * together with the School: a wallet with no rows and a wallet with a balance of 0 mean the same thing, so creating it
     * up front only adds a table that has to be cleaned up when a school is deleted.
     *
     * <p>Do NOT use this to build a row and then save it: the balance row must be created by
     * {@code SchoolBalanceRepository.findBySchoolIdForUpdateOrCreate}, because only there is the creation part of the same
     * INSERT ... ON CONFLICT statement as the row lock. Building it in Java and saving would reopen exactly the race window
     * that method exists to close.
     */
    public static SchoolBalance emptyFor(UUID schoolId, Instant now) {
        return builder()
            .schoolId(schoolId)
            .balanceVnd(BigDecimal.ZERO)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    /**
     * Adds to / subtracts from the balance and returns the NEW balance. It is returned so the caller can write it straight
     * into {@code SchoolBalanceEntry.balanceAfterVnd} -- those two numbers must be one, because if the caller recomputes
     * it, the statement and the aggregated balance would drift apart.
     *
     * @param amountVnd positive = top-up/refund, negative = deduction
     */
    public BigDecimal apply(BigDecimal amountVnd, Instant now) {
        this.balanceVnd = this.balanceVnd.add(amountVnd);
        this.updatedAt = now;
        return this.balanceVnd;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolBalance target = new SchoolBalance();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder balanceVnd(BigDecimal balanceVnd) {
            target.balanceVnd = balanceVnd;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            target.updatedAt = updatedAt;
            return this;
        }

        public Builder version(Long version) {
            target.version = version;
            return this;
        }

        public SchoolBalance build() {
            return target;
        }

    }
}
