package com.sep.vox.domain.model.school;

import java.time.Instant;
import java.util.UUID;

public class SchoolDirectory {
    private UUID id;
    private String code;
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

    public SchoolDirectory() {}

    public SchoolDirectory(UUID id, String code, String name, String provinceCode, String provinceName,
            String districtName, String domain, String address, SchoolDirectoryOrigin origin, boolean verified, Instant createdAt,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.domain = domain;
        this.address = address;
        this.origin = origin;
        this.verified = verified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public SchoolDirectory(String code, String name, String provinceCode, String provinceName, String districtName,
            String domain, String address, SchoolDirectoryOrigin origin, boolean verified, Instant createdAt, Instant updatedAt,
            UUID createdBy, UUID updatedBy) {
        this.code = code;
        this.name = name;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.domain = domain;
        this.address = address;
        this.origin = origin;
        this.verified = verified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
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
}
