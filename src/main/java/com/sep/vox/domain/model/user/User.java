package com.sep.vox.domain.model.user;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.sep.vox.domain.valueobject.business.Email;
import com.sep.vox.domain.valueobject.business.Phone;
import com.sep.vox.domain.valueobject.id.UserId;

public class User {
    private UserId id;
    private String username;
    private Email email;
    private String passwordHash;
    private Phone phone;
    private String fullName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    
    public UserId getId() {
        return id;
    }
    public void setId(UserId id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
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

    
}
