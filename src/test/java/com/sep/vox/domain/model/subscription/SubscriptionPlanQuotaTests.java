package com.sep.vox.domain.model.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.sep.vox.domain.model.metering.QuotaType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionPlanQuotaTests {

    @Test
    void forPlanSetsEveryArgument() {
        UUID planId = UUID.randomUUID();

        SubscriptionPlanQuota quota = SubscriptionPlanQuota.forPlan(planId, QuotaType.PRACTICE, new BigDecimal("150000"));

        assertThat(quota.getId()).isNull();
        assertThat(quota.getSubscriptionPlanId()).isEqualTo(planId);
        assertThat(quota.getQuotaType()).isEqualTo(QuotaType.PRACTICE);
        assertThat(quota.getIncludedAmountVnd()).isEqualByComparingTo("150000");
    }
}
