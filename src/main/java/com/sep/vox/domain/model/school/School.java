package com.sep.vox.domain.model.school;

import com.sep.vox.domain.model.BaseModel;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.SchoolCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;

public class School extends BaseModel {
    private SchoolCode code;
    private String name;
    private String description;
    private Phone contactPhone;
    private Email contactEmail;
    private SchoolDomain domain;
    private String address;
    private StudentCount studentCount;
    private boolean isActive;

    public School() {
    }

    public SchoolCode getCode() {
        return code;
    }

    public void setCode(SchoolCode code) {
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

    public Phone getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(Phone contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Email getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(Email contactEmail) {
        this.contactEmail = contactEmail;
    }

    public SchoolDomain getDomain() {
        return domain;
    }

    public void setDomain(SchoolDomain domain) {
        this.domain = domain;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public StudentCount getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(StudentCount studentCount) {
        this.studentCount = studentCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public static School create(String code, String name, String description, String contactPhone, String contactEmail,
            String domain, String address, int studentCount) {
        return builder()
            .code(new SchoolCode(code))
            .name(name)
            .description(description)
            .contactPhone(new Phone(contactPhone))
            .contactEmail(new Email(contactEmail))
            .domain(new SchoolDomain(domain))
            .address(address)
            .studentCount(new StudentCount(studentCount))
            .active(true)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<School, Builder> {

        private Builder() {
            super(new School());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder code(SchoolCode code) {
            target.code = code;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder contactPhone(Phone contactPhone) {
            target.contactPhone = contactPhone;
            return this;
        }

        public Builder contactEmail(Email contactEmail) {
            target.contactEmail = contactEmail;
            return this;
        }

        public Builder domain(SchoolDomain domain) {
            target.domain = domain;
            return this;
        }

        public Builder address(String address) {
            target.address = address;
            return this;
        }

        public Builder studentCount(StudentCount studentCount) {
            target.studentCount = studentCount;
            return this;
        }

        public Builder active(boolean isActive) {
            target.isActive = isActive;
            return this;
        }
    }
}
