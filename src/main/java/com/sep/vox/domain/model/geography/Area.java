package com.sep.vox.domain.model.geography;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.Name;

public class Area extends BaseModel {
    private Code code; 
    private Name name;
    private AreaLevel level;
    private UUID parentId;
    private Instant effectiveFrom;
    private Instant effectiveTo;

    public Area() {}

    public Area(Code code, Name name, AreaLevel level, UUID parentId, Instant effectiveFrom, Instant effectiveTo) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.parentId = parentId;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public Area(Builder builder) {
        super(builder.id, builder.createdAt);
        this.code = builder.code;
        this.name = builder.name;
        this.level = builder.level;
        this.parentId = builder.parentId;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveTo = builder.effectiveTo;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public AreaLevel getLevel() {
        return level;
    }

    public void setLevel(AreaLevel level) {
        this.level = level;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public static Builder builder() {
        
        return new Area.Builder();
    }

    public static class Builder {
        private UUID id;
        private Code code; 
        private Name name;
        private AreaLevel level;
        private UUID parentId;
        private Instant effectiveFrom;
        private Instant effectiveTo;
        private Instant createdAt;

        public Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder code(Code code) {
            this.code = code;
            return this;
        }

        public Builder name(Name name) {
            this.name = name;
            return this;
        }

        public Builder level(AreaLevel level) {
            this.level = level;
            return this;
        }

        public Builder parentId(UUID parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(Instant effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Area build() {
            return new Area(this);
        }
    }
}
