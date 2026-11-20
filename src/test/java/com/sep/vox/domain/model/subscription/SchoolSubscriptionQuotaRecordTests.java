package com.sep.vox.domain.model.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Quy ước tiêu tiền của ví hạn mức: <b>tiền GÓI tiêu trước, tiền TỰ NẠP tiêu sau.</b>
 *
 * <p>Không cột nào ghi lại từng đồng đã tiêu thuộc nguồn nào, nên phải chọn một quy ước -- và quy ước
 * này có lợi cho nhà trường: định mức gói dù sao cũng hết hạn cuối kỳ, còn tiền trường tự bỏ ra thì
 * không nên bốc hơi trước nó. Nó cũng khớp thứ tự mà ConsumeQuotaService vốn đã tiêu ở cấp trên (hạn
 * mức trước, ví sau).
 *
 * <p>Con số này quyết định trường được mang bao nhiêu tiền sang kỳ mới, nên sai ở đây là mất tiền
 * thật của khách -- xem OrderSettlementService.seedQuotaRecords.
 */
class SchoolSubscriptionQuotaRecordTests {

    private final UUID subscriptionId = UUID.randomUUID();

    @Test
    void unspent_funding_is_untouched_while_plan_money_still_covers_the_usage() {
        // A 15m wallet (10m plan + 5m top-up), 8m spent -- still within the plan portion, so the 5m
        // top-up is untouched.
        SchoolSubscriptionQuotaRecord record = record(15_000_000, 8_000_000, 5_000_000);

        assertThat(record.unspentFundedVnd()).isEqualByComparingTo("5000000");
    }

    @Test
    void unspent_funding_shrinks_once_usage_eats_past_the_plan_allowance() {
        // Spending 12m from a 15m wallet: the 10m plan portion is used up, the next 2m eats into
        // the top-up money, leaving 3m.
        SchoolSubscriptionQuotaRecord record = record(15_000_000, 12_000_000, 5_000_000);

        assertThat(record.unspentFundedVnd()).isEqualByComparingTo("3000000");
    }

    @Test
    void unspent_funding_is_zero_when_the_pool_is_fully_spent() {
        SchoolSubscriptionQuotaRecord record = record(15_000_000, 15_000_000, 5_000_000);

        assertThat(record.unspentFundedVnd()).isEqualByComparingTo("0");
    }

    @Test
    void unspent_funding_is_zero_for_a_pool_that_was_never_funded() {
        SchoolSubscriptionQuotaRecord record = record(10_000_000, 2_000_000, 0);

        assertThat(record.unspentFundedVnd()).isEqualByComparingTo("0");
    }

    @Test
    void seeding_adds_carried_funding_on_top_of_the_new_plan_allowance() {
        // New period: the plan grants 10m and 3m of unspent top-up money is carried over -> a 13m
        // wallet, of which the 3m is still marked as the school's own money, so the NEXT renewal
        // can carry it over again.
        SchoolSubscriptionQuotaRecord seeded = SchoolSubscriptionQuotaRecord.seeded(
            subscriptionId, QuotaType.PRACTICE, BigDecimal.valueOf(10_000_000), BigDecimal.valueOf(3_000_000));

        assertThat(seeded.getTotalAllocatedAmountVnd()).isEqualByComparingTo("13000000");
        assertThat(seeded.getFundedFromBalanceVnd()).isEqualByComparingTo("3000000");
        assertThat(seeded.getUsedAmountVnd()).isEqualByComparingTo("0");
        assertThat(seeded.unspentFundedVnd()).isEqualByComparingTo("3000000");
    }

    @Test
    void seeding_without_carried_funding_leaves_the_pool_purely_plan_funded() {
        SchoolSubscriptionQuotaRecord seeded = SchoolSubscriptionQuotaRecord.seeded(
            subscriptionId, QuotaType.ASSESSMENT, BigDecimal.valueOf(10_000_000), BigDecimal.ZERO);

        assertThat(seeded.getTotalAllocatedAmountVnd()).isEqualByComparingTo("10000000");
        assertThat(seeded.getFundedFromBalanceVnd()).isEqualByComparingTo("0");
    }

    private SchoolSubscriptionQuotaRecord record(long totalVnd, long usedVnd, long fundedVnd) {
        return new SchoolSubscriptionQuotaRecord(
            UUID.randomUUID(), subscriptionId, QuotaType.PRACTICE,
            BigDecimal.valueOf(totalVnd), BigDecimal.valueOf(usedVnd), BigDecimal.valueOf(fundedVnd));
    }
}
