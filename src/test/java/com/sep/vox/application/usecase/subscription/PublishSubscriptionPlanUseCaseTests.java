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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.PublishSubscriptionPlanCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanReplacementValidator;
import com.sep.vox.application.port.input.usecase.subscription.PublishSubscriptionPlanUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Điểm mới đáng test: publish một gói DRAFT đang là replacedByPlanId của gói khác (luồng "Tạo gói
 * thay thế") phải bị chặn nếu điều khoản (giá/chu kỳ/hạn mức/thời lượng) tệ hơn gói nó thay -- đây là
 * lúc DUY NHẤT luật đó được kiểm cho luồng này, vì lúc tạo gói còn DRAFT nên chưa kiểm được.
 */
class PublishSubscriptionPlanUseCaseTests {

    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID REPLACED_PLAN_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    private SubscriptionPlanRepository subscriptionPlanRepository;
    private SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private PublishSubscriptionPlanUseCase useCase;

    @BeforeEach
    void setUp() {
        subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);
        subscriptionPlanQuotaRepository = mock(SubscriptionPlanQuotaRepository.class);
        var userContextPort = mock(UserContextPort.class);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        when(subscriptionPlanQuotaRepository.findBySubscriptionPlanId(PLAN_ID))
            .thenReturn(List.of(quota(QuotaType.EXAM, BigDecimal.valueOf(1_000_000))));
        when(subscriptionPlanRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        useCase = new PublishSubscriptionPlanUseCase(
            subscriptionPlanRepository,
            subscriptionPlanQuotaRepository,
            new SubscriptionPlanReplacementValidator(subscriptionPlanQuotaRepository),
            userContextPort);
    }

    @Test
    void should_publish_a_plain_draft_with_no_replacement_target() {
        givenDraftPlan(BigDecimal.valueOf(100_000));
        when(subscriptionPlanRepository.findByReplacedByPlanId(PLAN_ID)).thenReturn(List.of());

        var result = useCase.execute(new PublishSubscriptionPlanCommand(PLAN_ID));

        assertThat(result).isEqualTo(PLAN_ID);
    }

    @Test
    void should_block_publish_when_price_is_lower_than_the_plan_it_replaces() {
        givenDraftPlan(BigDecimal.valueOf(50_000));
        var replacedPlan = activePlan(BigDecimal.valueOf(100_000));
        when(subscriptionPlanRepository.findByReplacedByPlanId(PLAN_ID)).thenReturn(List.of(replacedPlan));

        assertThatThrownBy(() -> useCase.execute(new PublishSubscriptionPlanCommand(PLAN_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("giá");
    }

    @Test
    void should_block_publish_when_quota_is_lower_than_the_plan_it_replaces() {
        givenDraftPlan(BigDecimal.valueOf(100_000));
        when(subscriptionPlanQuotaRepository.findBySubscriptionPlanId(PLAN_ID))
            .thenReturn(List.of(quota(QuotaType.EXAM, BigDecimal.valueOf(500_000))));
        var replacedPlan = activePlan(BigDecimal.valueOf(100_000));
        when(subscriptionPlanQuotaRepository.findBySubscriptionPlanId(REPLACED_PLAN_ID))
            .thenReturn(List.of(quota(QuotaType.EXAM, BigDecimal.valueOf(1_000_000))));
        when(subscriptionPlanRepository.findByReplacedByPlanId(PLAN_ID)).thenReturn(List.of(replacedPlan));

        assertThatThrownBy(() -> useCase.execute(new PublishSubscriptionPlanCommand(PLAN_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hạn mức");
    }

    @Test
    void should_publish_when_terms_are_at_least_as_good_as_the_plan_it_replaces() {
        givenDraftPlan(BigDecimal.valueOf(100_000));
        var replacedPlan = activePlan(BigDecimal.valueOf(100_000));
        when(subscriptionPlanQuotaRepository.findBySubscriptionPlanId(REPLACED_PLAN_ID))
            .thenReturn(List.of(quota(QuotaType.EXAM, BigDecimal.valueOf(1_000_000))));
        when(subscriptionPlanRepository.findByReplacedByPlanId(PLAN_ID)).thenReturn(List.of(replacedPlan));

        var result = useCase.execute(new PublishSubscriptionPlanCommand(PLAN_ID));

        assertThat(result).isEqualTo(PLAN_ID);
    }

    @Test
    void should_reject_publishing_a_plan_that_is_not_a_draft() {
        var plan = activePlan(BigDecimal.valueOf(100_000));
        plan.setId(PLAN_ID);
        when(subscriptionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> useCase.execute(new PublishSubscriptionPlanCommand(PLAN_ID)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_reject_when_the_plan_does_not_exist() {
        when(subscriptionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new PublishSubscriptionPlanCommand(PLAN_ID)))
            .isInstanceOf(NotFoundException.class);
    }

    private void givenDraftPlan(BigDecimal priceVnd) {
        var plan = plan(PLAN_ID, priceVnd, SubscriptionPlanStatus.DRAFT);
        when(subscriptionPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
    }

    private SubscriptionPlan activePlan(BigDecimal priceVnd) {
        return plan(REPLACED_PLAN_ID, priceVnd, SubscriptionPlanStatus.ACTIVE);
    }

    private static SubscriptionPlan plan(UUID id, BigDecimal priceVnd, SubscriptionPlanStatus status) {
        var plan = new SubscriptionPlan();
        plan.setId(id);
        plan.setName("Gói test");
        plan.setTagline("Tagline");
        plan.setPriceVnd(priceVnd);
        plan.setPeriodType(SubscriptionPlanPeriod.YEAR);
        plan.setPeriodCount(1);
        plan.setMaxTimePerAttemptMin(15);
        plan.setStatus(status);
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedAt(Instant.now());
        return plan;
    }

    private static SubscriptionPlanQuota quota(QuotaType quotaType, BigDecimal includedAmountVnd) {
        return new SubscriptionPlanQuota(UUID.randomUUID(), quotaType, includedAmountVnd);
    }
}
