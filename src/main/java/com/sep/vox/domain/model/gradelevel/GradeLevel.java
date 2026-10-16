package com.sep.vox.domain.model.gradelevel;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;

public class GradeLevel extends BaseModel {
    private Code code;
    private String name;
    private String description;
    private PositiveInteger order;
    private GradeLevelStatus status;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected GradeLevel() {}

    public GradeLevel(Code code, String name, String description, PositiveInteger order, GradeLevelStatus status, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.order = order;
        this.status = status;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public GradeLevel(Builder builder) {
        super(builder.id, builder.createdAt);
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.order = builder.order;
        this.status = builder.status;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PositiveInteger getOrder() {
        return order;
    }

    public void setOrder(PositiveInteger order) {
        this.order = order;
    }

    public GradeLevelStatus getStatus() {
        return status;
    }

    public void setStatus(GradeLevelStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isNotActive() {
        return this.status != GradeLevelStatus.ACTIVE;
    }

    public void delete(UUID updatedBy) {
        this.status = GradeLevelStatus.INACTIVE;
        this.updatedBy = updatedBy;
    }

    public static GradeLevel create(String name, String description, Integer order, UUID createdBy) {
        return GradeLevel.builder()
            .name(name)
            .description(description)
            .order(PositiveInteger.from(order))
            .status(GradeLevelStatus.ACTIVE)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
    }

    public static Builder builder() {
        return new GradeLevel.Builder();
    }

    public static class Builder {
        private UUID id;
        private Code code;
        private String name;
        private String description;
        private PositiveInteger order;
        private GradeLevelStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder code(Code code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder order(PositiveInteger order) {
            this.order = order;
            return this;
        }

        public Builder status(GradeLevelStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(UUID updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public GradeLevel build() {
            return new GradeLevel(this);
        }
    }
}
