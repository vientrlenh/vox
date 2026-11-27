package com.sep.vox.domain.model.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.sep.vox.domain.model.metering.QuotaType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchoolSubscriptionQuotaUserAllocationTests {

    @Test
    void unallocatedStartsWithZeroAllocatedAndZeroUsed() {
        UUID subscriptionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SchoolSubscriptionQuotaUserAllocation allocation =
                SchoolSubscriptionQuotaUserAllocation.unallocated(subscriptionId, QuotaType.ASSESSMENT, userId);

        assertThat(allocation.getId()).isNull();
        assertThat(allocation.getSchoolSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(allocation.getQuotaType()).isEqualTo(QuotaType.ASSESSMENT);
        assertThat(allocation.getUserId()).isEqualTo(userId);
        assertThat(allocation.getAllocatedAmountVnd()).isEqualByComparingTo("0");
        assertThat(allocation.getUsedAmountVnd()).isEqualByComparingTo("0");
    }
}
