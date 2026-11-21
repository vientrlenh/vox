package com.sep.vox.domain.model.user;

import java.time.LocalDate;

import com.sep.vox.domain.model.BaseModel;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public class User extends BaseModel {
    private static final String PASSWORD_NOT_SET = "__PASSWORD_NOT_SET__";

    private Email email;
    private String passwordHash;
    private Phone phone;
    private FullName fullName;
    private Gender gender;
    private DateOfBirth dateOfBirth;
    private String address;
    private String avatarUrl;
    private UserStatus status;

    public User() {}

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

    /**
     * Users created by a school (imported or added by hand) stay INACTIVE until they set their own password through
     * the invitation mail, but the school still has to place them in classes from the start of the year. So only an
     * account that was locked or disabled cannot be added to a class.
     */
    public boolean canBeAssignedToSchoolClass() {
        return status != UserStatus.DISABLED && status != UserStatus.LOCKED;
    }

    public void updatePasswordAndActivate(String passwordHash) {
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
    }

    public static User createSchoolAdmin(String email, String phone, String fullName, LocalDate dateOfBirth,
            String address, String avatarUrl) {
        return create(email, phone, fullName, dateOfBirth, address, avatarUrl);
    }

    public static User create(String email, String phone, String fullName, LocalDate dateOfBirth, String address,
            String avatarUrl) {
        return builder()
            .email(new Email(email))
            .passwordHash(PASSWORD_NOT_SET)
            .phone(new Phone(phone))
            .fullName(new FullName(fullName))
            .dateOfBirth(new DateOfBirth(dateOfBirth))
            .address(address)
            .avatarUrl(avatarUrl)
            .status(UserStatus.INACTIVE)
            .build();
    }

    public void softDelete() {
        this.status = UserStatus.DISABLED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<User, Builder> {

        private Builder() {
            super(new User());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder email(Email email) {
            target.email = email;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            target.passwordHash = passwordHash;
            return this;
        }

        public Builder phone(Phone phone) {
            target.phone = phone;
            return this;
        }

        public Builder fullName(FullName fullName) {
            target.fullName = fullName;
            return this;
        }

        public Builder gender(Gender gender) {
            target.gender = gender;
            return this;
        }

        public Builder dateOfBirth(DateOfBirth dateOfBirth) {
            target.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder address(String address) {
            target.address = address;
            return this;
        }

        public Builder avatarUrl(String avatarUrl) {
            target.avatarUrl = avatarUrl;
            return this;
        }

        public Builder status(UserStatus status) {
            target.status = status;
            return this;
        }
    }
}
