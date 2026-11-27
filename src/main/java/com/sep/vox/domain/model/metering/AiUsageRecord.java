package com.sep.vox.domain.model.metering;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AiUsageRecord {
    private UUID id;
    private UUID assessmentSessionId;
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
    private BigDecimal costUsd; // report come from AI service
    private BigDecimal costVnd; // the price that the system really need
    private BigDecimal fxRateUsed;
    private Instant occurredAt;

    public AiUsageRecord() {}



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentSessionId() {
        return assessmentSessionId;
    }

    public void setAssessmentSessionId(UUID assessmentSessionId) {
        this.assessmentSessionId = assessmentSessionId;
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

    public BigDecimal getCostVnd() {
        return costVnd;
    }

    public void setCostVnd(BigDecimal costVnd) {
        this.costVnd = costVnd;
    }

    public BigDecimal getFxRateUsed() {
        return fxRateUsed;
    }

    public void setFxRateUsed(BigDecimal fxRateUsed) {
        this.fxRateUsed = fxRateUsed;
    }
    
    

    public static AiUsageRecord recorded(
            UUID assessmentSessionId, UUID turnId, UUID usageEventId, AiUsageType usageType, String provider,
            String modelName, Integer inputTokens, Integer outputTokens, Integer cacheCreationInputTokens,
            Integer cacheReadInputTokens, Long durationMs, String unitPriceJson, BigDecimal costUsd,
            BigDecimal costVnd, BigDecimal fxRateUsed, Instant occurredAt) {
        return builder()
                .assessmentSessionId(assessmentSessionId)
                .turnId(turnId)
                .usageEventId(usageEventId)
                .usageType(usageType)
                .provider(provider)
                .modelName(modelName)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .cacheCreationInputTokens(cacheCreationInputTokens)
                .cacheReadInputTokens(cacheReadInputTokens)
                .durationMs(durationMs)
                .unitPriceJson(unitPriceJson)
                .costUsd(costUsd)
                .costVnd(costVnd)
                .fxRateUsed(fxRateUsed)
                .occurredAt(occurredAt)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AiUsageRecord target = new AiUsageRecord();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder assessmentSessionId(UUID assessmentSessionId) {
            target.assessmentSessionId = assessmentSessionId;
            return this;
        }

        public Builder turnId(UUID turnId) {
            target.turnId = turnId;
            return this;
        }

        public Builder usageEventId(UUID usageEventId) {
            target.usageEventId = usageEventId;
            return this;
        }

        public Builder usageType(AiUsageType usageType) {
            target.usageType = usageType;
            return this;
        }

        public Builder provider(String provider) {
            target.provider = provider;
            return this;
        }

        public Builder modelName(String modelName) {
            target.modelName = modelName;
            return this;
        }

        public Builder inputTokens(Integer inputTokens) {
            target.inputTokens = inputTokens;
            return this;
        }

        public Builder outputTokens(Integer outputTokens) {
            target.outputTokens = outputTokens;
            return this;
        }

        public Builder cacheCreationInputTokens(Integer cacheCreationInputTokens) {
            target.cacheCreationInputTokens = cacheCreationInputTokens;
            return this;
        }

        public Builder cacheReadInputTokens(Integer cacheReadInputTokens) {
            target.cacheReadInputTokens = cacheReadInputTokens;
            return this;
        }

        public Builder durationMs(Long durationMs) {
            target.durationMs = durationMs;
            return this;
        }

        public Builder unitPriceJson(String unitPriceJson) {
            target.unitPriceJson = unitPriceJson;
            return this;
        }

        public Builder costUsd(BigDecimal costUsd) {
            target.costUsd = costUsd;
            return this;
        }

        public Builder costVnd(BigDecimal costVnd) {
            target.costVnd = costVnd;
            return this;
        }

        public Builder fxRateUsed(BigDecimal fxRateUsed) {
            target.fxRateUsed = fxRateUsed;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            target.occurredAt = occurredAt;
            return this;
        }

        public AiUsageRecord build() {
            return target;
        }

    }
}
