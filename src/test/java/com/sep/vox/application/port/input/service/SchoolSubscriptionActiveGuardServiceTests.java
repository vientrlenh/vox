package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

class SchoolSubscriptionActiveGuardServiceTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();

    @Test
    void requireActiveForSchool_throws_whenNoActiveSubscription() {
        SchoolSubscriptionRepository repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveBySchoolId(SCHOOL_ID)).thenReturn(Optional.empty());
        SchoolSubscriptionActiveGuardService guard = new SchoolSubscriptionActiveGuardService(repository);

        assertThatThrownBy(() -> guard.requireActiveForSchool(SCHOOL_ID, "create a centralized assessment"))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("has no active subscription")
            .hasMessageContaining("create a centralized assessment");
    }

    @Test
    void requireActiveForSchool_passes_whenSubscriptionActive() {
        SchoolSubscriptionRepository repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveBySchoolId(SCHOOL_ID)).thenReturn(Optional.of(mock(SchoolSubscription.class)));
        SchoolSubscriptionActiveGuardService guard = new SchoolSubscriptionActiveGuardService(repository);

        assertThatCode(() -> guard.requireActiveForSchool(SCHOOL_ID, "create a class test")).doesNotThrowAnyException();
    }

    @Test
    void requireActiveForStudent_throws_whenNoActiveSubscription() {
        SchoolSubscriptionRepository repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveSubscriptionIdForUser(STUDENT_ID)).thenReturn(Optional.empty());
        SchoolSubscriptionActiveGuardService guard = new SchoolSubscriptionActiveGuardService(repository);

        assertThatThrownBy(() -> guard.requireActiveForStudent(STUDENT_ID, "start a personalized AI practice session"))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("has no active subscription");
    }

    @Test
    void requireActiveForStudent_passes_whenSubscriptionActive() {
        SchoolSubscriptionRepository repository = mock(SchoolSubscriptionRepository.class);
        when(repository.findActiveSubscriptionIdForUser(STUDENT_ID)).thenReturn(Optional.of(SUBSCRIPTION_ID));
        SchoolSubscriptionActiveGuardService guard = new SchoolSubscriptionActiveGuardService(repository);

        assertThatCode(() -> guard.requireActiveForStudent(STUDENT_ID, "start a personalized AI practice session"))
            .doesNotThrowAnyException();
    }
}
