package com.sep.vox.domain.model.subscription;

import java.time.Instant;
import java.util.UUID;

public class TokenUsageEvent {
    private UUID id;
    private UUID subscriptionId;
    private UUID examSessionId;
    private QuotaType quotaType;
    private Integer tokensConsumed;
    private Instant occurredAt;

    public TokenUsageEvent() {}

    public TokenUsageEvent(UUID id, UUID subscriptionId, UUID examSessionId, QuotaType quotaType,
            Integer tokensConsumed, Instant occurredAt) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.examSessionId = examSessionId;
        this.quotaType = quotaType;
        this.tokensConsumed = tokensConsumed;
        this.occurredAt = occurredAt;
    }

    public TokenUsageEvent(UUID subscriptionId, UUID examSessionId, QuotaType quotaType,
            Integer tokensConsumed, Instant occurredAt) {
        this.subscriptionId = subscriptionId;
        this.examSessionId = examSessionId;
        this.quotaType = quotaType;
        this.tokensConsumed = tokensConsumed;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public Integer getTokensConsumed() {
        return tokensConsumed;
    }

    public void setTokensConsumed(Integer tokensConsumed) {
        this.tokensConsumed = tokensConsumed;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
