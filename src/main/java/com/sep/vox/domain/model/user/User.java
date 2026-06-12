package com.sep.vox.domain.model.user;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public class User {
    private UUID id;
    private Email email;
    private String passwordHash;
    private Phone phone;
    private FullName fullName;
    private Gender gender;
    private DateOfBirth dateOfBirth;
    private String address;
    private String avatarUrl;
    private UserStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public User() {}

    public User(UUID id, Email email, String passwordHash, Phone phone,
            FullName fullName, Gender gender, DateOfBirth dateOfBirth, String address, String avatarUrl, UserStatus status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public User(Email email, String passwordHash, Phone phone,
            FullName fullName, Gender gender, DateOfBirth dateOfBirth, String address, String avatarUrl, UserStatus status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
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

    public FullName getFullName() {
        return fullName;
    }

    public void setFullName(FullName fullName) {
        this.fullName = fullName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public DateOfBirth getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(DateOfBirth dateOfBirth) {
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



    public void updatePasswordAndActivate(String passwordHash, OffsetDateTime now) {
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
        this.updatedAt = now;
    }

    
    public static User createSchoolAdmin(String email, String password, String phone, String fullName, LocalDate dateOfBirth, String address, String avatarUrl, UUID createdUserId, OffsetDateTime now) {
        return new User(
            new Email(email), 
            password, 
            new Phone(phone), 
            new FullName(fullName), 
            null, 
            new DateOfBirth(dateOfBirth), 
            address, 
            avatarUrl,
            UserStatus.INACTIVE, 
            now, 
            now, 
            createdUserId, 
            createdUserId
        );
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public static User createStudent(String email, String phone, String fullName, LocalDate dateOfBirth, String address, String avatarUrl, UUID createdBy, OffsetDateTime now) {
        return createSchoolUser(email, phone, fullName, dateOfBirth, address, avatarUrl, createdBy, now);
    }

    public static User createTeacher(String email, String phone, String fullName, LocalDate dateOfBirth, String address, String avatarUrl, UUID createdBy, OffsetDateTime now) {
        return createSchoolUser(email, phone, fullName, dateOfBirth, address, avatarUrl, createdBy, now);
    }

    private static final String PASSWORD_NOT_SET = "__PASSWORD_NOT_SET__";

    private static User createSchoolUser(String email, String phone, String fullName, LocalDate dateOfBirth, String address, String avatarUrl, UUID createdBy, OffsetDateTime now) {
        return new User(
            new Email(email),
            PASSWORD_NOT_SET,
            new Phone(phone),
            new FullName(fullName),
            null,
            new DateOfBirth(dateOfBirth),
            address,
            avatarUrl,
            UserStatus.INACTIVE,
            now,
            now,
            createdBy,
            createdBy
        );
    }

    public void softDelete(UUID updatedBy, OffsetDateTime now) {
        this.status = UserStatus.DISABLED;
        this.updatedBy = updatedBy;
        this.updatedAt = now;
    }

}
