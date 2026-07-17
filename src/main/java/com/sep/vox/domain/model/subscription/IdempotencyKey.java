package com.sep.vox.domain.model.subscription;

import java.time.OffsetDateTime;
import java.util.UUID;

public class IdempotencyKey {
    private UUID id;
    private String key;
    private String resultRef;
    private OffsetDateTime createdAt;

    public IdempotencyKey() {}

    public IdempotencyKey(UUID id, String key, String resultRef, OffsetDateTime createdAt) {
        this.id = id;
        this.key = key;
        this.resultRef = resultRef;
        this.createdAt = createdAt;
    }

    public IdempotencyKey(String key, String resultRef, OffsetDateTime createdAt) {
        this.key = key;
        this.resultRef = resultRef;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getResultRef() {
        return resultRef;
    }

    public void setResultRef(String resultRef) {
        this.resultRef = resultRef;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
