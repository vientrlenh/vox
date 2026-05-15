package com.sep.vox.domain.model.user;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;

public class User {
    private UUID id;
    private Email email;
    private String passwordHash;
    private Phone phone;
    private String fullName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;
    private UserStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public User() {}

    public User(UUID id, Email email, String passwordHash, Phone phone,
            String fullName, Gender gender, LocalDate dateOfBirth, String address, UserStatus status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public User(Email email, String passwordHash, Phone phone,
            String fullName, Gender gender, LocalDate dateOfBirth, String address, UserStatus status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }


    // Getter and setter
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Email getEmail() {
        return email;
    }
    public void setEmail(Email email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Phone getPhone() {
        return phone;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
}
