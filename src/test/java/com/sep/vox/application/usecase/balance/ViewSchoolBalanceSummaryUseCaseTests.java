package com.sep.vox.application.usecase.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewSchoolBalanceSummaryQuery;
import com.sep.vox.application.port.input.service.SchoolScopedReadGuard;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceSummaryUseCase;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;

/**
 * Ô tổng của trang sao kê tồn tại để giải thích MỌI thay đổi của số dư trong dải. Điều đó chỉ đúng
 * khi nó cộng dồn đủ mọi loại bút toán -- và một lần đã không đúng: QUOTA_FUNDING thêm vào enum ở V12
 * mà không thêm vào đây, nên trường chuyển 5tr sang ví hạn mức thì sao kê hiện một ví tụt 5tr với ba
 * ô tổng đều bằng 0.
 *
 * <p>Vì thế phép kiểm chính ở đây duyệt {@code SchoolBalanceEntryType.values()} chứ không liệt kê
 * tay: liệt kê tay là lặp lại đúng cái danh sách vừa quên cập nhật.
 */
class ViewSchoolBalanceSummaryUseCaseTests {

    private SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private SchoolScopedReadGuard schoolScopedReadGuard;
    private ViewSchoolBalanceSummaryUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final Instant from = Instant.parse("2026-08-01T00:00:00Z");
    private final Instant to = Instant.parse("2026-09-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        schoolBalanceEntryRepository = mock(SchoolBalanceEntryRepository.class);
        schoolScopedReadGuard = mock(SchoolScopedReadGuard.class);
        useCase = new ViewSchoolBalanceSummaryUseCase(schoolBalanceEntryRepository, schoolScopedReadGuard);

        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(any(), any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void every_entry_type_is_summed_so_the_tiles_account_for_the_whole_balance_change() {
        useCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));

        for (var entryType : SchoolBalanceEntryType.values()) {
            verify(schoolBalanceEntryRepository)
                .sumAmountBySchoolIdAndEntryTypeInRange(eq(schoolId), eq(entryType), eq(from), eq(to));
        }
    }

    @Test
    void quota_funding_is_reported_on_its_own_rather_than_folded_into_the_overage_bucket() {
        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            any(), eq(SchoolBalanceEntryType.OVERAGE_CHARGE), any(), any()))
            .thenReturn(new BigDecimal("-120000"));
        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            any(), eq(SchoolBalanceEntryType.QUOTA_FUNDING), any(), any()))
            .thenReturn(new BigDecimal("-5000000"));

        var summary = useCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));

        // Gộp hai con số này là nói với hiệu trưởng rằng trường đã chi 5,12tr cho AI, trong khi thật
        // ra chỉ chi 120k -- 5tr kia vẫn là tiền của trường, chỉ nằm ở ví hạn mức.
        assertThat(summary.overageChargedVnd()).isEqualTo("-120000");
        assertThat(summary.quotaFundedVnd()).isEqualTo("-5000000");
    }

    @Test
    void top_up_and_refund_stay_folded_together_because_both_read_as_money_in() {
        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            any(), eq(SchoolBalanceEntryType.TOP_UP), any(), any()))
            .thenReturn(new BigDecimal("2000000"));
        when(schoolBalanceEntryRepository.sumAmountBySchoolIdAndEntryTypeInRange(
            any(), eq(SchoolBalanceEntryType.REFUND), any(), any()))
            .thenReturn(new BigDecimal("300000"));

        var summary = useCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));

        assertThat(summary.creditedVnd()).isEqualTo("2300000");
    }

    @Test
    void a_school_with_no_entries_reads_zero_everywhere_instead_of_failing() {
        var summary = useCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));

        assertThat(summary.creditedVnd()).isEqualTo("0");
        assertThat(summary.overageChargedVnd()).isEqualTo("0");
        assertThat(summary.quotaFundedVnd()).isEqualTo("0");
        assertThat(summary.adjustedVnd()).isEqualTo("0");
    }
}
