package com.sep.vox.domain.model.subscription;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit ledger of the times the System Admin intervened in the lifecycle of a subscription.
 *
 * <p>A separate table is needed because the three columns {@code suspendedAt/suspendedReason/suspendedBy} on
 * SchoolSubscription are STATE, not HISTORY: lifting a suspension clears all three to null, so afterwards nothing
 * shows that the school was ever suspended, by whom, or why. For a forced action aimed at a paying customer, losing
 * the trail is unacceptable.
 *
 * <p>financial_events is NOT reused: that table was made for money, and its amount_signed/currency/payment_method
 * are all NOT NULL, so every suspension would have to put a 0 VND payment by "MANUAL" into it -- three meaningless
 * values just to satisfy constraints. The MONEY part of that table now belongs to orders/payment_records/invoices/
 * school_balance_entries; the lifecycle part belongs here.
 *
 * <p>It follows the path school_debt_event opened: one event table per concern, with every column meaningful for
 * every row.
 */
public class SchoolSubscriptionEvent {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId;
    private SchoolSubscriptionEventType eventType;
    /** The person who acted -- always a System Admin, never null. */
    private UUID actorId;
    /** Required for SUSPENDED; for UNSUSPENDED it is an optional note. */
    private String reason;
    private Instant occurredAt;

    public SchoolSubscriptionEvent() {}



    public static SchoolSubscriptionEvent suspended(UUID schoolId, UUID subscriptionId, UUID actorId,
            String reason, Instant now) {
        return builder()
            .schoolId(schoolId)
            .subscriptionId(subscriptionId)
            .eventType(SchoolSubscriptionEventType.SUSPENDED)
            .actorId(actorId)
            .reason(reason)
            .occurredAt(now)
            .build();
    }

    public static SchoolSubscriptionEvent unsuspended(UUID schoolId, UUID subscriptionId, UUID actorId,
            String note, Instant now) {
        return builder()
            .schoolId(schoolId)
            .subscriptionId(subscriptionId)
            .eventType(SchoolSubscriptionEventType.UNSUSPENDED)
            .actorId(actorId)
            .reason(note)
            .occurredAt(now)
            .build();
    }

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

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public SchoolSubscriptionEventType getEventType() {
        return eventType;
    }

    public void setEventType(SchoolSubscriptionEventType eventType) {
        this.eventType = eventType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolSubscriptionEvent target = new SchoolSubscriptionEvent();

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

        public Builder subscriptionId(UUID subscriptionId) {
            target.subscriptionId = subscriptionId;
            return this;
        }

        public Builder eventType(SchoolSubscriptionEventType eventType) {
            target.eventType = eventType;
            return this;
        }

        public Builder actorId(UUID actorId) {
            target.actorId = actorId;
            return this;
        }

        public Builder reason(String reason) {
            target.reason = reason;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            target.occurredAt = occurredAt;
            return this;
        }

        public SchoolSubscriptionEvent build() {
            return target;
        }

    }
}
