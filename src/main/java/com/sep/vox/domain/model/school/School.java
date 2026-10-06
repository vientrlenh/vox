package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.domain.common.BaseModel;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.PositiveInteger;

public class School extends BaseModel {
    private Code code;
    private Name name;
    private String description;
    private SchoolDomain domain;
    private UUID wardId;
    private UUID cityId;
    private UUID provinceId;
    private String streetAddress;
    private PositiveInteger studentCount;
    private SchoolStatus status;
    private Instant updatedAt;
    private UUID registeredBy;
    private UUID createdBy;
    private UUID updatedBy;

    public School() {}

    public School(Code code, Name name, String description, SchoolDomain domain, UUID wardId, UUID cityId, UUID provinceId, String streetAddress, PositiveInteger studentCount, SchoolStatus status, Instant updatedAt, UUID registeredBy, UUID createdBy, UUID updatedBy) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.domain = domain;
        this.wardId = wardId;
        this.cityId = cityId;
        this.provinceId = provinceId;
        this.streetAddress = streetAddress;
        this.studentCount = studentCount;
        this.status = status;
        this.updatedAt = updatedAt;
        this.registeredBy = registeredBy;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public School(Builder builder) {
        super(builder.id, builder.createdAt);
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.domain = builder.domain;
        this.wardId = builder.wardId;
        this.cityId = builder.cityId;
        this.provinceId = builder.provinceId;
        this.streetAddress = builder.streetAddress;
        this.studentCount = builder.studentCount;
        this.status = builder.status;
        this.updatedAt = builder.updatedAt;
        this.registeredBy = builder.registeredBy;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SchoolDomain getDomain() {
        return domain;
    }

    public void setDomain(SchoolDomain domain) {
        this.domain = domain;
    }

    public UUID getWardId() {
        return wardId;
    }

    public void setWardId(UUID wardId) {
        this.wardId = wardId;
    }

    public UUID getCityId() {
        return cityId;
    }

    public void setCityId(UUID cityId) {
        this.cityId = cityId;
    }

    public UUID getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(UUID provinceId) {
        this.provinceId = provinceId;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public PositiveInteger getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(PositiveInteger studentCount) {
        this.studentCount = studentCount;
    }

    public SchoolStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(UUID registeredBy) {
        this.registeredBy = registeredBy;
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

    public static Builder builder() {
        return new School.Builder();
    }

    public static School create(
        String code, String name, String description, String domain, UUID wardId, UUID cityId, UUID provinceId, String streetAddress, int studentCount, UUID registeredBy, UUID createdBy, Instant now
    ) {
        School school = School.builder()
            .code(Code.from(code))
            .name(Name.from(name))
            .description(description)
            .domain(SchoolDomain.from(domain))
            .wardId(wardId)
            .cityId(cityId)
            .provinceId(provinceId)
            .streetAddress(streetAddress)
            .studentCount(PositiveInteger.from(studentCount))
            .status(SchoolStatus.INACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .registeredBy(registeredBy)
            .createdBy(createdBy)
            .updatedBy(createdBy)
            .build();
        return school;
    }

    public static class Builder {
        private UUID id;
        private Code code;
        private Name name;
        private String description;
        private SchoolDomain domain;
        private UUID wardId;
        private UUID cityId;
        private UUID provinceId;
        private String streetAddress;
        private PositiveInteger studentCount;
        private SchoolStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID registeredBy;
        private UUID createdBy;
        private UUID updatedBy;

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

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder domain(SchoolDomain domain) {
            this.domain = domain;
            return this;
        }

        public Builder wardId(UUID wardId) {
            this.wardId = wardId;
            return this;
        }

        public Builder cityId(UUID cityId) {
            this.cityId = cityId;
            return this;
        }

        public Builder provinceId(UUID provinceId) {
            this.provinceId = provinceId;
            return this;
        }

        public Builder streetAddress(String streetAddress) {
            this.streetAddress = streetAddress;
            return this;
        }

        public Builder studentCount(PositiveInteger count) {
            this.studentCount = count;
            return this;
        }

        public Builder status(SchoolStatus status) {
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

        public Builder registeredBy(UUID registeredBy) {
            this.registeredBy = registeredBy;
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

        public School build() {
            return new School(this);
        }
    }
    
}
