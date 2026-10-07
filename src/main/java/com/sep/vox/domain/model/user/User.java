package com.sep.vox.domain.model.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.BirthDate;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Name;
import com.sep.vox.domain.valueobject.Phone;

public class User extends BaseModel {
    private Email email;
    private String passwordHash;
    private UserRole role;
    private Phone phone;
    private Name fullName;
    private Gender gender;
    private BirthDate birthDate;
    private String address;
    private String avatarUrl;
    private UserStatus status;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected User() {}

    public User(Email email, String passwordHash, UserRole role, Phone phone,
            Name fullName, Gender gender, BirthDate birthDate, String address, String avatarUrl, UserStatus status, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.phone = phone;
        this.fullName = fullName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public User(Builder builder) {
        super(builder.id, builder.createdAt);
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.role = builder.role;
        this.phone = builder.phone;
        this.fullName = builder.fullName;
        this.gender = builder.gender;
        this.birthDate = builder.birthDate;
        this.address = builder.address;
        this.avatarUrl = builder.avatarUrl;
        this.status = builder.status;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Phone getPhone() {
        return phone;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
    }

    public Name getFullName() {
        return fullName;
    }

    public void setFullName(Name fullName) {
        this.fullName = fullName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public BirthDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(BirthDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
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

    public boolean isSystemAdmin() {
        return this.role == UserRole.SYSTEM_ADMIN;
    }

    public List<String> roleStrs() {
        return List.of(this.role.name());
    }

    public void updatePasswordAndActivate(String hashedPassword, Instant now) {
        this.passwordHash = hashedPassword;
        this.updatedAt = now;
        this.status = UserStatus.ACTIVE;
    }

    public static Builder builder() {
        return new User.Builder();
    }

    public static User createStudent() {
        return User.builder()
            .build();
    }

    public static class Builder { 
        private UUID id;
        private Email email;
        private String passwordHash;
        private UserRole role;
        private Phone phone;
        private Name fullName;
        private Gender gender;
        private BirthDate birthDate;
        private String address;
        private String avatarUrl;
        private UserStatus status;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;

        public Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder email(Email email) {
            this.email = email;
            return this;
        } 

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder role(UserRole role) {
            this.role = role;
            return this;
        }

        public Builder phone(Phone phone) {
            this.phone = phone;
            return this;
        } 

        public Builder fullName(Name fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder gender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder birthDate(BirthDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public Builder status(UserStatus status) {
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

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(UUID updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

}
