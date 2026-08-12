package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AiUsageRecord {
    private UUID id;
    private UUID examSessionId;
    private UUID turnId;
    private UUID usageEventId;
    private AiUsageType usageType;
    private String provider;
    private String modelName;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer cacheCreationInputTokens;
    private Integer cacheReadInputTokens;
    private Long durationMs;
    private String unitPriceJson;
    private BigDecimal costUsd;
    private Instant occurredAt;

    public AiUsageRecord() {}

    public AiUsageRecord(UUID id, UUID examSessionId, UUID turnId, UUID usageEventId, AiUsageType usageType,
            String provider, String modelName, Integer inputTokens, Integer outputTokens,
            Integer cacheCreationInputTokens, Integer cacheReadInputTokens, Long durationMs, String unitPriceJson,
            BigDecimal costUsd, Instant occurredAt) {
        this.id = id;
        this.examSessionId = examSessionId;
        this.turnId = turnId;
        this.usageEventId = usageEventId;
        this.usageType = usageType;
        this.provider = provider;
        this.modelName = modelName;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.cacheCreationInputTokens = cacheCreationInputTokens;
        this.cacheReadInputTokens = cacheReadInputTokens;
        this.durationMs = durationMs;
        this.unitPriceJson = unitPriceJson;
        this.costUsd = costUsd;
        this.occurredAt = occurredAt;
    }

    public AiUsageRecord(UUID examSessionId, UUID turnId, UUID usageEventId, AiUsageType usageType, String provider,
            String modelName, Integer inputTokens, Integer outputTokens, Integer cacheCreationInputTokens,
            Integer cacheReadInputTokens, Long durationMs, String unitPriceJson, BigDecimal costUsd,
            Instant occurredAt) {
        this.examSessionId = examSessionId;
        this.turnId = turnId;
        this.usageEventId = usageEventId;
        this.usageType = usageType;
        this.provider = provider;
        this.modelName = modelName;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.cacheCreationInputTokens = cacheCreationInputTokens;
        this.cacheReadInputTokens = cacheReadInputTokens;
        this.durationMs = durationMs;
        this.unitPriceJson = unitPriceJson;
        this.costUsd = costUsd;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public UUID getTurnId() {
        return turnId;
    }

    public void setTurnId(UUID turnId) {
        this.turnId = turnId;
    }

    public UUID getUsageEventId() {
        return usageEventId;
    }

    public void setUsageEventId(UUID usageEventId) {
        this.usageEventId = usageEventId;
    }

    public AiUsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(AiUsageType usageType) {
        this.usageType = usageType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Integer getCacheCreationInputTokens() {
        return cacheCreationInputTokens;
    }

    public void setCacheCreationInputTokens(Integer cacheCreationInputTokens) {
        this.cacheCreationInputTokens = cacheCreationInputTokens;
    }

    public Integer getCacheReadInputTokens() {
        return cacheReadInputTokens;
    }

    public void setCacheReadInputTokens(Integer cacheReadInputTokens) {
        this.cacheReadInputTokens = cacheReadInputTokens;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getUnitPriceJson() {
        return unitPriceJson;
    }

    public void setUnitPriceJson(String unitPriceJson) {
        this.unitPriceJson = unitPriceJson;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(BigDecimal costUsd) {
        this.costUsd = costUsd;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}