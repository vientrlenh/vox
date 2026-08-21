package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.ArchivePlanCommand;
import com.sep.vox.application.port.input.usecase.subscription.ArchivePlanUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

class ArchivePlanUseCaseTests {

    private SubscriptionPlanRepository subscriptionPlanRepository;
    private PlanQuotaRepository planQuotaRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private UserContextPort userContextPort;
    private ArchivePlanUseCase useCase;

    private final UUID planId = UUID.randomUUID();
    private final UUID replacementId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        planQuotaRepository = mock(PlanQuotaRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new ArchivePlanUseCase(
            subscriptionPlanRepository, planQuotaRepository, schoolSubscriptionRepository, userContextPort);

        when(userContextPort.isSystemAdmin()).thenReturn(true);
        when(planQuotaRepository.findAllByPlanId(planId)).thenReturn(List.of());
        when(subscriptionPlanRepository.save(any(SubscriptionPlan.class))).thenAnswer(call -> call.getArgument(0));
    }

    private SubscriptionPlan plan(UUID id, BigDecimal price, PlanStatus status) {
        return new SubscriptionPlan(
            id, "Gói", null, price, 365, 60, status, 1, Instant.now(), null, null, new BigDecimal("0.20"));
    }

    @Test
    void archivesWithReplacementPlanOfDifferentPrice() {
        var original = plan(planId, new BigDecimal("1000000"), PlanStatus.ACTIVE);
        var replacement = plan(replacementId, new BigDecimal("2000000"), PlanStatus.ACTIVE);
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(original));
        when(subscriptionPlanRepository.findById(replacementId)).thenReturn(Optional.of(replacement));
        when(schoolSubscriptionRepository.existsActiveByPlanId(planId)).thenReturn(true);

        var result = useCase.execute(new ArchivePlanCommand(planId, replacementId));

        assertThat(result).isNotNull();
        assertThat(original.getStatus()).isEqualTo(PlanStatus.ARCHIVED);
        assertThat(original.getReplacedByPlanId()).isEqualTo(replacementId);
    }

    @Test
    void rejectsReplacementPlanEqualToItself() {
        var original = plan(planId, new BigDecimal("1000000"), PlanStatus.ACTIVE);
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(original));
        when(schoolSubscriptionRepository.existsActiveByPlanId(planId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new ArchivePlanCommand(planId, planId)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
