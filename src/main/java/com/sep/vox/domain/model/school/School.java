package com.sep.vox.domain.model.school;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.SchoolCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;

public class
School {
    private UUID id;
    private SchoolCode code;
    private String name;
    private String description;
    private Phone contactPhone;
    private Email contactEmail;
    private SchoolDomain domain;
    private String address;
    private StudentCount studentCount;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public School() {}

    public School(UUID id, SchoolCode code, String name, String description, Phone contactPhone, Email contactEmail, SchoolDomain domain, String address, StudentCount studentCount, boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.domain = domain;
        this.address = address;
        this.studentCount = studentCount;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public School(SchoolCode code, String name, String description, Phone contactPhone, Email contactEmail, SchoolDomain domain, 
            String address, StudentCount studentCount, boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            UUID createdBy, UUID updatedBy) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.domain = domain;
        this.address = address;
        this.studentCount = studentCount;
        this.isActive = isActive;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
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

    public SchoolDomain getDomain() {
        return domain;
    }

    public void setDomain(SchoolDomain domain) {
        this.domain = domain;
    }

    public static School create(String code, String name, String description, String contactPhone, String contactEmail, String domain, String address, int studentCount, UUID createdUserId, OffsetDateTime now) {
        return new School(
            new SchoolCode(code), 
            name, 
            description, 
            new Phone(contactPhone), 
            new Email(contactEmail), 
            new SchoolDomain(domain), 
            address, 
            new StudentCount(studentCount), 
            true, 
            now, 
            now, 
            createdUserId, 
            createdUserId
        );

    }
    
}
