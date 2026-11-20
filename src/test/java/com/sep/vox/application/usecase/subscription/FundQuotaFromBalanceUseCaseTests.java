package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.command.FundQuotaFromBalanceCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.subscription.FundQuotaFromBalanceUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Nạp tiền từ ví tự nạp sang ví hạn mức -- phép chuyển giữa hai túi của cùng nhà trường.
 *
 * <p>Bộ test này canh ba thứ, và cả ba đều là lý do phương án "trừ ví khi chia cho từng người" bị bỏ:
 *
 * <ul>
 *   <li><b>Tiền rời ví đúng MỘT lần.</b> Ví giảm, ví hạn mức tăng đúng bằng đó, và không có lần trừ
 *       thứ hai -- vì {@code tryConsume} sau đó sẽ thấy ví hạn mức còn chỗ nên
 *       {@code ConsumeQuotaService.chargeOverage} không bao giờ chạy.</li>
 *   <li><b>Khóa dòng trước khi trừ.</b> Đường ghi số dư DUY NHẤT đi qua
 *       {@code findBySchoolIdForUpdateOrCreate}; đọc bằng {@code findBySchoolId} rồi trừ sẽ để hai
 *       quản trị viên bấm cùng lúc cùng thấy một số dư và cùng tiêu nó.</li>
 *   <li><b>balanceAfter của bút toán lấy từ chính lần trừ đó</b>, không tính lại -- nếu không thì sao
 *       kê và số dư tổng hợp trôi khỏi nhau và bất biến SUM(entries) = balance_vnd vỡ.</li>
 * </ul>
 */
class FundQuotaFromBalanceUseCaseTests {

    private UserContextPort userContextPort;
    private SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private FundQuotaFromBalanceUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID quotaId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        var schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        quotaRecordRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        schoolBalanceEntryRepository = mock(SchoolBalanceEntryRepository.class);

        useCase = new FundQuotaFromBalanceUseCase(
            userContextPort,
            schoolSubscriptionRepository,
            quotaRecordRepository,
            schoolBalanceRepository,
            schoolBalanceEntryRepository,
            new SchoolSubscriptionDebtGuardService(schoolBalanceRepository));

        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(actorId);

        var subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        subscription.setSchoolId(schoolId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));

        givenPool(10_000_000, 10_000_000, 0);
        givenWallet(2_000_000);
    }

    @Test
    void funding_moves_money_from_the_wallet_into_the_quota_pool() {
        var response = useCase.execute(fund(500_000));

        verify(quotaRecordRepository).addFundingFromBalance(eq(quotaId), eq(BigDecimal.valueOf(500_000)));
        assertThat(response.fundedAmountVnd()).isEqualByComparingTo("500000");
        assertThat(response.balanceAfterVnd()).isEqualByComparingTo("1500000");
    }

    @Test
    void funding_debits_the_wallet_exactly_once_and_records_one_entry() {
        useCase.execute(fund(500_000));

        var entry = captureEntry();
        assertThat(entry.getEntryType()).isEqualTo(SchoolBalanceEntryType.QUOTA_FUNDING);
        // Âm: tiền RỜI ví. chk_school_balance_entries_quota_funding_traceable đòi amount_vnd < 0, và
        // factory tự đảo dấu để chỗ gọi không phải nhớ.
        assertThat(entry.getAmountVnd()).isEqualByComparingTo("-500000");
        verify(schoolBalanceRepository).save(any());
        verify(schoolBalanceEntryRepository).save(any());
    }

    @Test
    void funding_takes_balance_after_from_the_row_it_just_debited() {
        useCase.execute(fund(500_000));

        var entry = captureEntry();
        var balanceCaptor = ArgumentCaptor.forClass(SchoolBalance.class);
        verify(schoolBalanceRepository).save(balanceCaptor.capture());

        assertThat(entry.getBalanceAfterVnd())
            .isEqualByComparingTo(balanceCaptor.getValue().getBalanceVnd());
    }

    @Test
    void funding_locks_the_balance_row_before_debiting_it() {
        useCase.execute(fund(500_000));

        verify(schoolBalanceRepository).findBySchoolIdForUpdateOrCreate(eq(schoolId), any());
    }

    @Test
    void funding_carries_the_actor_and_quota_type_onto_the_entry() {
        // Cả hai đều BẮT BUỘC ở tầng DB: đây là quyết định của con người, một chiều, không hoàn lại
        // được, nên sổ phải nói được ai bấm và tiền vào túi nào.
        useCase.execute(fund(500_000));

        var entry = captureEntry();
        assertThat(entry.getActorId()).isEqualTo(actorId);
        assertThat(entry.getQuotaType()).isEqualTo(QuotaType.PRACTICE);
        assertThat(entry.getSubscriptionId()).isEqualTo(subscriptionId);
    }

    @Test
    void funding_entry_carries_no_ai_cost_columns() {
        // Đây là tiền đổi túi, KHÔNG phải một khoản chi cho AI -- không có hóa đơn nhà cung cấp nào để
        // đối soát và không phiên nào gây ra nó. Ràng buộc ở DB đòi đúng như vậy.
        useCase.execute(fund(500_000));

        var entry = captureEntry();
        assertThat(entry.getCostUsd()).isNull();
        assertThat(entry.getFxRateUsed()).isNull();
        assertThat(entry.getExamSessionId()).isNull();
        assertThat(entry.getPracticeSessionId()).isNull();
        assertThat(entry.getOrderId()).isNull();
    }

    @Test
    void funding_is_refused_when_the_wallet_holds_less_than_requested() {
        assertThatThrownBy(() -> useCase.execute(fund(2_500_000)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không đủ");

        verify(schoolBalanceEntryRepository, never()).save(any());
        verify(quotaRecordRepository, never()).addFundingFromBalance(any(), any());
    }

    @Test
    void funding_is_refused_with_the_debt_message_when_the_school_is_locked() {
        // Số dư âm cũng sẽ trượt phép so "đủ tiền không" bên dưới, nhưng với câu "không đủ số dư" --
        // đúng hiện tượng, sai nguyên nhân, nên chỉ sai luôn cách khắc phục.
        givenWallet(-1_000_000);

        assertThatThrownBy(() -> useCase.execute(fund(500_000)))
            .isInstanceOf(PlanLimitExceededException.class)
            .hasMessageContaining("bị khóa");

        verify(schoolBalanceRepository, never()).save(any());
        verify(quotaRecordRepository, never()).addFundingFromBalance(any(), any());
    }

    @Test
    void funding_is_refused_for_a_non_positive_amount() {
        assertThatThrownBy(() -> useCase.execute(fund(0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lớn hơn 0");

        assertThatThrownBy(() -> useCase.execute(fund(-500_000)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lớn hơn 0");

        verify(schoolBalanceRepository, never()).save(any());
    }

    @Test
    void funding_is_refused_for_a_school_the_admin_does_not_belong_to() {
        assertThatThrownBy(() -> useCase.execute(new FundQuotaFromBalanceCommand(
                UUID.randomUUID(), "PRACTICE", BigDecimal.valueOf(500_000), null)))
            .isInstanceOf(ForbiddenException.class);

        verify(schoolBalanceRepository, never()).save(any());
    }

    @Test
    void funding_is_refused_for_an_unknown_quota_type() {
        assertThatThrownBy(() -> useCase.execute(new FundQuotaFromBalanceCommand(
                schoolId, "SPEAKING", BigDecimal.valueOf(500_000), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Loại hạn mức không hợp lệ");
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private FundQuotaFromBalanceCommand fund(long amountVnd) {
        return new FundQuotaFromBalanceCommand(
            schoolId, "PRACTICE", BigDecimal.valueOf(amountVnd), null);
    }

    private SchoolBalanceEntry captureEntry() {
        var captor = ArgumentCaptor.forClass(SchoolBalanceEntry.class);
        verify(schoolBalanceEntryRepository).save(captor.capture());
        return captor.getValue();
    }

    private void givenPool(long totalVnd, long usedVnd, long fundedVnd) {
        var pool = new SchoolSubscriptionQuotaRecord(
            quotaId, subscriptionId, QuotaType.PRACTICE,
            BigDecimal.valueOf(totalVnd), BigDecimal.valueOf(usedVnd), BigDecimal.valueOf(fundedVnd));
        when(quotaRecordRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.PRACTICE))
            .thenReturn(Optional.of(pool));
        when(quotaRecordRepository.findById(quotaId)).thenReturn(Optional.of(pool));
    }

    /**
     * CÙNG MỘT instance cho cả đường đọc (guard nợ) lẫn đường ghi (khóa dòng): use case gọi
     * {@code apply()} lên bản ghi đang giữ khóa, và test phải thấy đúng con số đã bị đổi đó.
     */
    private void givenWallet(long balanceVnd) {
        var balance = new SchoolBalance(schoolId, BigDecimal.valueOf(balanceVnd), null, null);
        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.of(balance));
        when(schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(eq(schoolId), any())).thenReturn(balance);
    }
}
