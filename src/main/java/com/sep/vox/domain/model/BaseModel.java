package com.sep.vox.domain.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * State shared by every domain model that is stored with an id and audit columns.
 *
 * <p>{@code createdBy} and {@code updatedBy} hold the acting user's id as text, or
 * {@link com.sep.vox.domain.common.ActorConstant#SYSTEM} when no user performed the change. The persistence layer fills
 * all four audit values when the record is saved; use cases and factories do not set them. Rows written before this
 * convention can still hold {@code null}.
 */
public abstract class BaseModel {

    private static final int UUID_TEXT_LENGTH = 36;

    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /** The creating user's id, or empty when the record was created by the system or the value is unknown. */
    public Optional<UUID> createdByUserId() {
        return toUserId(createdBy);
    }

    /** The last updating user's id, or empty when the last update was made by the system or the value is unknown. */
    public Optional<UUID> updatedByUserId() {
        return toUserId(updatedBy);
    }

    public boolean isCreatedBy(UUID userId) {
        return userId != null && userId.toString().equals(createdBy);
    }

    private static Optional<UUID> toUserId(String actor) {
        if (actor == null || actor.length() != UUID_TEXT_LENGTH) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(actor));
        } catch (IllegalArgumentException notAUserId) {
            return Optional.empty();
        }
    }

    /**
     * Fluent builder of the shared fields. A subclass builder adds its own fields and passes the model it fills to
     * the constructor. A builder is single use: {@link #build()} returns the instance it has been filling, not a copy.
     *
     * @param <T> the model being built
     * @param <B> the concrete builder, so shared setters can return it and chaining keeps its type
     */
    public abstract static class Builder<T extends BaseModel, B extends Builder<T, B>> {

        protected final T target;

        protected Builder(T target) {
            this.target = target;
        }

        protected abstract B self();

        public B id(UUID id) {
            target.setId(id);
            return self();
        }

        public B createdAt(Instant createdAt) {
            target.setCreatedAt(createdAt);
            return self();
        }

        public B updatedAt(Instant updatedAt) {
            target.setUpdatedAt(updatedAt);
            return self();
        }

        public B createdBy(String createdBy) {
            target.setCreatedBy(createdBy);
            return self();
        }

        public B updatedBy(String updatedBy) {
            target.setUpdatedBy(updatedBy);
            return self();
        }

        public T build() {
            return target;
        }
    }
}
