package com.sep.vox.domain.shared;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseModel {
    private UUID id;
    private Instant createdAt;

    protected BaseModel() {}

    protected BaseModel(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
