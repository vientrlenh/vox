package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

class SchoolSubscriptionActiveGuardServiceTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();

    @Test
    void requireActiveForSchool_throws_whenNoActiveSubscription() {
        var repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveBySchoolId(SCHOOL_ID)).thenReturn(Optional.empty());
        var guard = new SchoolSubscriptionActiveGuardService(repository);

        assertThatThrownBy(() -> guard.requireActiveForSchool(SCHOOL_ID, "tạo Bài kiểm tra tập trung"))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("chưa có gói subscription đang hoạt động")
            .hasMessageContaining("tạo Bài kiểm tra tập trung");
    }

    @Test
    void requireActiveForSchool_passes_whenSubscriptionActive() {
        var repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveBySchoolId(SCHOOL_ID)).thenReturn(Optional.of(mock(SchoolSubscription.class)));
        var guard = new SchoolSubscriptionActiveGuardService(repository);

        assertThatCode(() -> guard.requireActiveForSchool(SCHOOL_ID, "tạo Class Test")).doesNotThrowAnyException();
    }

    @Test
    void requireActiveForStudent_throws_whenNoActiveSubscription() {
        var repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveSubscriptionIdForUser(STUDENT_ID)).thenReturn(Optional.empty());
        var guard = new SchoolSubscriptionActiveGuardService(repository);

        assertThatThrownBy(() -> guard.requireActiveForStudent(STUDENT_ID, "bắt đầu buổi luyện tập cá nhân hóa AI"))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("chưa có gói subscription đang hoạt động");
    }

    @Test
    void requireActiveForStudent_passes_whenSubscriptionActive() {
        var repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveSubscriptionIdForUser(STUDENT_ID)).thenReturn(Optional.of(SUBSCRIPTION_ID));
        var guard = new SchoolSubscriptionActiveGuardService(repository);

        assertThatCode(() -> guard.requireActiveForStudent(STUDENT_ID, "bắt đầu buổi luyện tập cá nhân hóa AI"))
            .doesNotThrowAnyException();
    }
}
