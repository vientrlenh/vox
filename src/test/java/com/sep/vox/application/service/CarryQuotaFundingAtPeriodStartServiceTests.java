package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.CarryQuotaFundingAtPeriodStartService;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

/**
 * Chốt phần tiền tự nạp mang sang tại ĐÚNG ranh giới hai kỳ -- nửa còn lại của lỗi gia hạn sớm.
 *
 * <p>{@code OrderSettlementQuotaCarryForwardTests} canh nửa kia: lúc chốt đơn thì KHÔNG được chốt con
 * số. File này canh chuyện gì xảy ra khi kỳ mới thật sự bắt đầu, và ba tính chất ở đây là ba thứ khiến
 * phép mang sang muộn an toàn hơn phép chụp ảnh sớm:
 *
 * <ul>
 *   <li><b>Đọc muộn</b> nên thấy đúng con số sau khi kỳ cũ đã tiêu/nạp xong.</li>
 *   <li><b>Cộng thêm chứ không gán</b> nên không xoá khoản trường tự nạp vào kỳ mới trong lúc chờ job.</li>
 *   <li><b>Đúng một lần</b> vì cái hẹn bị xoá ngay -- cộng thêm mà chạy hai lần là nhân đôi tiền.</li>
 * </ul>
 */
class CarryQuotaFundingAtPeriodStartServiceTests {

    private SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private CarryQuotaFundingAtPeriodStartService service;

    private final UUID previousSubscriptionId = UUID.randomUUID();
    private final UUID newSubscriptionId = UUID.randomUUID();
    private final UUID newPracticePoolId = UUID.randomUUID();
    private final Instant now = Instant.parse("2027-01-01T00:00:00Z");

    private final List<SchoolSubscriptionQuotaRecord> sourcePools = new ArrayList<>();

    @BeforeEach
    void setUp() {
        quotaRecordRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        service = new CarryQuotaFundingAtPeriodStartService(quotaRecordRepository);

        when(quotaRecordRepository.findBySchoolSubscriptionId(previousSubscriptionId))
            .thenReturn(sourcePools);
    }

    @Test
    void nothing_to_do_when_no_period_has_a_pending_carry() {
        when(quotaRecordRepository.findDueFundingCarries(now)).thenReturn(List.of());

        assertThat(service.carryDueFunding(now)).isZero();
        verify(quotaRecordRepository, never()).addFundingFromBalance(any(), any());
    }

    @Test
    void the_unspent_funding_of_the_closing_period_lands_in_the_new_one() {
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        assertThat(service.carryDueFunding(now)).isEqualTo(1);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(5_000_000)));
    }

    @Test
    void the_amount_is_read_at_the_boundary_so_spending_in_the_window_reduces_it() {
        // Cả điểm của việc hoãn: trường tiêu 12tr trên kỳ cũ SAU khi đã trả tiền gia hạn. Ảnh chụp lúc
        // chốt đơn sẽ nói 5tr; con số đúng ở ranh giới là 3tr.
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 12_000_000, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(3_000_000)));
    }

    @Test
    void funding_added_during_the_window_is_carried_too_instead_of_being_destroyed() {
        // Chiều ngược lại: trường NẠP THÊM 3tr sau khi trả tiền gia hạn. Khoản đó vào ví kỳ cũ, không
        // có trong ảnh chụp lúc chốt đơn, và trước V13 nó chết ở ranh giới.
        givenSourcePool(QuotaType.PRACTICE, 18_000_000, 0, 8_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(8_000_000)));
    }

    @Test
    void carrying_adds_to_the_new_pool_rather_than_overwriting_what_it_already_holds() {
        // Kỳ mới đã bắt đầu và trường tự nạp 2tr vào nó trước khi job kịp chạy. Một phép GÁN
        // "funded = phần chưa tiêu của kỳ cũ" sẽ xoá đúng khoản đó.
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE, 2_000_000);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(5_000_000)));
        verify(quotaRecordRepository, never()).save(any());
    }

    @Test
    void the_promise_is_cleared_so_a_second_run_carries_nothing_twice() {
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).clearFundingCarry(newPracticePoolId);
    }

    @Test
    void the_promise_is_cleared_even_when_there_is_nothing_left_to_carry() {
        // Kỳ cũ tiêu cạn. Không xoá hẹn thì job đọc lại dòng này mỗi giờ, mãi mãi.
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 15_000_000, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        assertThat(service.carryDueFunding(now)).isZero();

        verify(quotaRecordRepository, never()).addFundingFromBalance(any(), any());
        verify(quotaRecordRepository).clearFundingCarry(newPracticePoolId);
    }

    @Test
    void a_quota_type_the_new_plan_dropped_is_left_alone_rather_than_carried_somewhere_else() {
        // Kỳ cũ còn tiền ở ví EXAM, kỳ mới chỉ có PRACTICE. Không có hẹn nào cho EXAM, nên khoản đó
        // nằm lại -- service phải KHÔNG đẩy nhầm nó sang ví còn lại.
        givenSourcePool(QuotaType.EXAM, 10_000_000, 0, 4_000_000);
        givenSourcePool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);
        givenDueCarry(QuotaType.PRACTICE);

        service.carryDueFunding(now);

        verify(quotaRecordRepository).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(5_000_000)));
        verify(quotaRecordRepository, never()).addFundingFromBalance(
            eq(newPracticePoolId), eq(BigDecimal.valueOf(4_000_000)));
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private void givenSourcePool(QuotaType quotaType, long totalVnd, long usedVnd, long fundedVnd) {
        sourcePools.add(new SchoolSubscriptionQuotaRecord(
            UUID.randomUUID(), previousSubscriptionId, quotaType,
            BigDecimal.valueOf(totalVnd), BigDecimal.valueOf(usedVnd), BigDecimal.valueOf(fundedVnd)));
    }

    private void givenDueCarry(QuotaType quotaType) {
        givenDueCarry(quotaType, 0);
    }

    /** Ví của kỳ mới, đã tới ngày chạy và còn hẹn. {@code alreadyFundedVnd} = trường tự nạp trong lúc chờ. */
    private void givenDueCarry(QuotaType quotaType, long alreadyFundedVnd) {
        var target = new SchoolSubscriptionQuotaRecord(
            newPracticePoolId, newSubscriptionId, quotaType,
            BigDecimal.valueOf(10_000_000 + alreadyFundedVnd), BigDecimal.ZERO,
            BigDecimal.valueOf(alreadyFundedVnd));
        target.setCarryFundingFromSubscriptionId(previousSubscriptionId);
        when(quotaRecordRepository.findDueFundingCarries(now)).thenReturn(List.of(target));
    }
}
