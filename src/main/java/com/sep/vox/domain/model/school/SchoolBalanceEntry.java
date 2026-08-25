package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SchoolBalanceEntry {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId; // khoảng chi cho gói đăng ký hiện tại
    private UUID orderId;
    private UUID examSessionId;
    private UUID actorId;
    private BigDecimal amountVnd;
    private BigDecimal balanceAfterVnd;
    private BigDecimal originalCostUsd;
    private BigDecimal exchangeRateUsed;
    private Instant occuredAt;
    private String quotaType;
    private String entryType;
    private String reason;

    public SchoolBalanceEntry() {}

    public SchoolBalanceEntry(UUID id, UUID schoolId, UUID subscriptionId, UUID orderId, UUID examSessionId,
            UUID actorId, BigDecimal amountVnd, BigDecimal balanceAfterVnd, BigDecimal originalCostUsd,
            BigDecimal exchangeRateUsed, Instant occuredAt, String quotaType, String entryType, String reason) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.orderId = orderId;
        this.examSessionId = examSessionId;
        this.actorId = actorId;
        this.amountVnd = amountVnd;
        this.balanceAfterVnd = balanceAfterVnd;
        this.originalCostUsd = originalCostUsd;
        this.exchangeRateUsed = exchangeRateUsed;
        this.occuredAt = occuredAt;
        this.quotaType = quotaType;
        this.entryType = entryType;
        this.reason = reason;
    }

    public SchoolBalanceEntry(UUID schoolId, UUID subscriptionId, UUID orderId, UUID examSessionId, UUID actorId,
            BigDecimal amountVnd, BigDecimal balanceAfterVnd, BigDecimal originalCostUsd, BigDecimal exchangeRateUsed,
            Instant occuredAt, String quotaType, String entryType, String reason) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.orderId = orderId;
        this.examSessionId = examSessionId;
        this.actorId = actorId;
        this.amountVnd = amountVnd;
        this.balanceAfterVnd = balanceAfterVnd;
        this.originalCostUsd = originalCostUsd;
        this.exchangeRateUsed = exchangeRateUsed;
        this.occuredAt = occuredAt;
        this.quotaType = quotaType;
        this.entryType = entryType;
        this.reason = reason;
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

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public void setAmountVnd(BigDecimal amountVnd) {
        this.amountVnd = amountVnd;
    }

    public BigDecimal getBalanceAfterVnd() {
        return balanceAfterVnd;
    }

    public void setBalanceAfterVnd(BigDecimal balanceAfterVnd) {
        this.balanceAfterVnd = balanceAfterVnd;
    }

    public BigDecimal getOriginalCostUsd() {
        return originalCostUsd;
    }

    public void setOriginalCostUsd(BigDecimal originalCostUsd) {
        this.originalCostUsd = originalCostUsd;
    }

    public BigDecimal getExchangeRateUsed() {
        return exchangeRateUsed;
    }

    public void setExchangeRateUsed(BigDecimal exchangeRateUsed) {
        this.exchangeRateUsed = exchangeRateUsed;
    }

    public Instant getOccuredAt() {
        return occuredAt;
    }

    public void setOccuredAt(Instant occuredAt) {
        this.occuredAt = occuredAt;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(String quotaType) {
        this.quotaType = quotaType;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    
}
