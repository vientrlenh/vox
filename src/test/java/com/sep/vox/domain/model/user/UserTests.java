package com.sep.vox.domain.model.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTests {

    @Test
    void should_allow_class_assignment_for_user_who_has_not_set_password_yet() {
        assertThat(userWith(UserStatus.INACTIVE).canBeAssignedToSchoolClass()).isTrue();
    }

    @Test
    void should_allow_class_assignment_for_active_user() {
        assertThat(userWith(UserStatus.ACTIVE).canBeAssignedToSchoolClass()).isTrue();
    }

    @Test
    void should_reject_class_assignment_for_locked_user() {
        assertThat(userWith(UserStatus.LOCKED).canBeAssignedToSchoolClass()).isFalse();
    }

    @Test
    void should_reject_class_assignment_for_disabled_user() {
        assertThat(userWith(UserStatus.DISABLED).canBeAssignedToSchoolClass()).isFalse();
    }

    private static User userWith(UserStatus status) {
        var user = new User();
        user.setStatus(status);
        return user;
    }
}
