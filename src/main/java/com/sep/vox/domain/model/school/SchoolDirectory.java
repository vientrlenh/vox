package com.sep.vox.domain.model.school;

import com.sep.vox.domain.model.BaseModel;

public class SchoolDirectory extends BaseModel {
    private String code;
    private String name;
    private String provinceCode;
    private String provinceName;
    private String districtName;
    private String domain;
    private String address;
    private SchoolDirectoryOrigin origin;
    private boolean verified;

    public SchoolDirectory() {
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

    public void verify() {
        this.verified = true;
    }

    public static SchoolDirectory createByAdmin(String code, String name, String provinceCode, String provinceName,
            String districtName, String domain, String address) {
        return builder()
            .code(code)
            .name(name)
            .provinceCode(provinceCode)
            .provinceName(provinceName)
            .districtName(districtName)
            .domain(domain)
            .address(address)
            .origin(SchoolDirectoryOrigin.ADMIN_CREATED)
            .verified(true)
            .build();
    }

    public static SchoolDirectory createByUserSubmitted(String code, String name, String provinceCode, String provinceName,
            String districtName, String domain, String address) {
        return builder()
            .code(code)
            .name(name)
            .provinceCode(provinceCode)
            .provinceName(provinceName)
            .districtName(districtName)
            .domain(domain)
            .address(address)
            .origin(SchoolDirectoryOrigin.USER_SUBMITTED)
            .verified(false)
            .build();
    }

    public static SchoolDirectory createByImport(String code, String name, String provinceCode, String provinceName,
            String districtName, String domain, String address) {
        return builder()
            .code(code)
            .name(name)
            .provinceCode(provinceCode)
            .provinceName(provinceName)
            .districtName(districtName)
            .domain(domain)
            .address(address)
            .origin(SchoolDirectoryOrigin.OFFICIAL_IMPORT)
            .verified(false)
            .build();
    }

    public void applyImportUpdate(String name, String provinceCode, String provinceName, String districtName,
            String domain, String address) {
        this.name = name;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.domain = domain;
        this.address = address;
        this.origin = SchoolDirectoryOrigin.OFFICIAL_IMPORT;
    }

    public boolean isCurated() {
        return verified || origin == SchoolDirectoryOrigin.ADMIN_CREATED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<SchoolDirectory, Builder> {

        private Builder() {
            super(new SchoolDirectory());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder provinceCode(String provinceCode) {
            target.provinceCode = provinceCode;
            return this;
        }

        public Builder provinceName(String provinceName) {
            target.provinceName = provinceName;
            return this;
        }

        public Builder districtName(String districtName) {
            target.districtName = districtName;
            return this;
        }

        public Builder domain(String domain) {
            target.domain = domain;
            return this;
        }

        public Builder address(String address) {
            target.address = address;
            return this;
        }

        public Builder origin(SchoolDirectoryOrigin origin) {
            target.origin = origin;
            return this;
        }

        public Builder verified(boolean verified) {
            target.verified = verified;
            return this;
        }
    }
}
