package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.Code;

public class SchoolDirectory extends BaseModel {
    private Code code;
    private String name;
    private String provinceCode;
    private String provinceName;
    private String districtName;
    private String domain;
    private String address; 
    private SchoolDirectoryOrigin origin;
    private boolean verified;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public SchoolDirectory() {}

    public SchoolDirectory(Code code, String name, String provinceCode, String provinceName,
            String districtName, String domain, String address, SchoolDirectoryOrigin origin, boolean verified,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.code = code;
        this.name = name;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.domain = domain;
        this.address = address;
        this.origin = origin;
        this.verified = verified;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public SchoolDirectory(Builder builder) {
        super(builder.id, builder.createdAt);
        this.code = builder.code;
        this.name = builder.name;
        this.provinceCode = builder.provinceCode;
        this.provinceName = builder.provinceName;
        this.districtName = builder.districtName;
        this.domain = builder.domain;
        this.address = builder.address;
        this.origin = builder.origin;
        this.verified = builder.verified;
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

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SchoolDirectoryOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(SchoolDirectoryOrigin origin) {
        this.origin = origin;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
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

    public static Builder builder() {
        return new SchoolDirectory.Builder();
    }

    public void verify(UUID updatedBy, Instant now) {
        this.verified = true;
        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    public static SchoolDirectory createByAdmin(String code, String name, String provinceCode, String provinceName, String districtName, String domain, String address, Instant now, UUID createdBy) {
        return new SchoolDirectory(
            code, 
            name, 
            provinceCode, 
            provinceName, 
            districtName, 
            domain, 
            address, 
            SchoolDirectoryOrigin.ADMIN_CREATED, true, 
            now, 
            now, 
            createdBy, 
            createdBy
        );
    }

    public static SchoolDirectory createByUserSubmitted(String code, String name, String provinceCode, String provinceName, String districtName, String domain, String address, Instant now, UUID createdBy) {
        return new SchoolDirectory(
            code, 
            name, 
            provinceCode, 
            provinceName, 
            districtName, 
            domain, 
            address, 
            SchoolDirectoryOrigin.USER_SUBMITTED,
            false,
            now,
            now,
            createdBy,
            createdBy
        );
    }

    public static SchoolDirectory createByImport(String code, String name, String provinceCode, String provinceName, String districtName, String domain, String address, Instant now, UUID createdBy) {
        return new SchoolDirectory(
            code,
            name,
            provinceCode,
            provinceName,
            districtName,
            domain,
            address,
            SchoolDirectoryOrigin.OFFICIAL_IMPORT,
            false,
            now,
            now,
            createdBy,
            createdBy
        );
    }


    public void applyImportUpdate(String name, String provinceCode, String provinceName, String districtName, String domain, String address, UUID updatedBy, Instant now) {
        this.name = name;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.domain = domain;
        this.address = address;
        this.origin = SchoolDirectoryOrigin.OFFICIAL_IMPORT;
        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }


    public boolean isCurated() {
        return verified || origin == SchoolDirectoryOrigin.ADMIN_CREATED;
    }

    public static class Builder {
        private UUID id;
        private Code code;
        private String name;
        private String provinceCode;
        private String provinceName;
        private String districtName;
        private String domain;
        private String address;
        private SchoolDirectoryOrigin origin;
        private boolean verified;
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

        public Builder provinceCode(String provinceCode) {
            this.provinceCode = provinceCode;
            return this;
        }

        public Builder provinceName(String provinceName) {
            this.provinceName = provinceName;
            return this;
        }

        public Builder districtName(String districtName) {
            this.districtName = districtName;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder origin(SchoolDirectoryOrigin origin) {
            this.origin = origin;
            return this;
        }

        public Builder verified(boolean verified) {
            this.verified = verified;
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

        public SchoolDirectory build() {
            return new SchoolDirectory(this);
        }
    }
}
