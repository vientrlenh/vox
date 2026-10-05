package com.sep.vox.domain.common;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseModel {
    protected UUID id;
    protected Instant createdAt;

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
