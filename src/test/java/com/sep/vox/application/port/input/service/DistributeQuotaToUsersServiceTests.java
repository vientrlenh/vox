package com.sep.vox.application.port.input.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.WalletDrawConfirmationRequiredException;
import com.sep.vox.application.port.input.command.AllocateUserQuotaAmountCommand;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.DistributionMode;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;

/**
 * Cấp hạn mức cá nhân MANUAL vượt phần pool có thể ăn vào ví tự nạp của trường
 * (DistributeQuotaToUsersService.computeManualAmounts/applyWalletDraws) -- trước đây chỉ NỚI TRẦN
 * (kiểm tra rồi bỏ qua), giờ phải trừ THẬT {@code school_balances} và ghi đúng một bút toán
 * ALLOCATION_DRAW mỗi người, cùng đường ghi có khoá mà ConsumeQuotaService.chargeOverage đã dùng.
 */
class DistributeQuotaToUsersServiceTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final QuotaType QUOTA_TYPE = QuotaType.PRACTICE;
    private static final String STUDENT_EMAIL = "student@example.com";

    private UserContextPort userContextPort;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private DistributeQuotaToUsersService service;

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        subscriptionQuotaUserAllocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        var roleRepository = mock(RoleRepository.class);
        var schoolUserRepository = mock(SchoolUserRepository.class);
        schoolQuotaPolicyRepository = mock(SchoolQuotaPolicyRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);
        schoolBalanceEntryRepository = mock(SchoolBalanceEntryRepository.class);
        var userRepository = mock(UserRepository.class);

        service = new DistributeQuotaToUsersService(
            userContextPort,
            schoolSubscriptionRepository,
            subscriptionQuotaRepository,
            subscriptionQuotaUserAllocationRepository,
            roleRepository,
            schoolUserRepository,
            schoolQuotaPolicyRepository,
            schoolBalanceRepository,
            schoolBalanceEntryRepository,
            userRepository);

        var student = new User();
        student.setId(STUDENT_ID);
        student.setEmail(new Email(STUDENT_EMAIL));
        when(userRepository.findByIdIn(List.of(STUDENT_ID))).thenReturn(List.of(student));

        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(userContextPort.getCurrentSchoolId()).thenReturn(SCHOOL_ID);
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);

        var subscription = new SchoolSubscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setSchoolId(SCHOOL_ID);
        when(schoolSubscriptionRepository.findActiveBySchoolId(SCHOOL_ID)).thenReturn(Optional.of(subscription));

        // Pool = 1.000.000đ, chia được toàn bộ (ratio mặc định) -- distributableVnd = 1.000.000.
        var pool = new SchoolSubscriptionQuotaRecord(
            UUID.randomUUID(), SUBSCRIPTION_ID, QUOTA_TYPE, BigDecimal.valueOf(1_000_000), BigDecimal.ZERO);
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(SUBSCRIPTION_ID, QUOTA_TYPE))
            .thenReturn(Optional.of(pool));
        when(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(SCHOOL_ID, QUOTA_TYPE))
            .thenReturn(SchoolQuotaPolicy.fullyDistributable(SCHOOL_ID, QUOTA_TYPE));

        var role = mock(Role.class);
        when(role.getId()).thenReturn(ROLE_ID);
        when(roleRepository.findByCode(any())).thenReturn(Optional.of(role));

        var schoolUser = new SchoolUser(SCHOOL_ID, STUDENT_ID, Instant.now(), null);
        when(schoolUserRepository.findBySchoolId(eq(SCHOOL_ID), any(), eq(ROLE_ID), any(), any(), eq(false), any(Integer.class), any(Integer.class)))
            .thenReturn(new PageResult<>(List.of(schoolUser), 1, 10_000, 1, 1));

        when(subscriptionQuotaUserAllocationRepository.findBySchoolSubscriptionIdAndQuotaType(SUBSCRIPTION_ID, QUOTA_TYPE))
            .thenReturn(List.of());
    }

    @Test
    void should_not_touch_wallet_when_allocation_fits_the_pool() {
        givenWalletBalance(BigDecimal.valueOf(2_000_000));

        distribute(BigDecimal.valueOf(500_000), false);

        verify(schoolBalanceRepository, never()).findBySchoolIdForUpdateOrCreate(any(), any());
        verify(schoolBalanceEntryRepository, never()).save(any());
    }

    @Test
    void should_require_confirmation_when_allocation_draws_on_the_wallet() {
        givenWalletBalance(BigDecimal.valueOf(2_000_000));

        assertThatThrownBy(() -> distribute(BigDecimal.valueOf(1_500_000), false))
            .isInstanceOf(WalletDrawConfirmationRequiredException.class);

        verify(schoolBalanceRepository, never()).findBySchoolIdForUpdateOrCreate(any(), any());
        verify(schoolBalanceEntryRepository, never()).save(any());
    }

    @Test
    void should_draw_the_wallet_and_record_an_entry_once_confirmed() {
        givenWalletBalance(BigDecimal.valueOf(2_000_000));

        distribute(BigDecimal.valueOf(1_500_000), true);

        var entry = captureEntry();
        assertThat(entry.getEntryType()).isEqualTo(SchoolBalanceEntryType.ALLOCATION_DRAW);
        assertThat(entry.getAmountVnd()).isEqualByComparingTo(BigDecimal.valueOf(-500_000));
        assertThat(entry.getBalanceAfterVnd()).isEqualByComparingTo(BigDecimal.valueOf(1_500_000));
        assertThat(entry.getTargetUserId()).isEqualTo(STUDENT_ID);
        assertThat(entry.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(entry.getQuotaType()).isEqualTo(QUOTA_TYPE);
        // Sao kê phải hiện email, không phải UUID trần -- trường không tra được id.
        assertThat(entry.getReason()).contains(STUDENT_EMAIL).doesNotContain(STUDENT_ID.toString());
    }

    @Test
    void should_reject_when_exceeding_both_the_pool_and_the_wallet() {
        givenWalletBalance(BigDecimal.valueOf(100_000));

        assertThatThrownBy(() -> distribute(BigDecimal.valueOf(1_500_000), true))
            .isInstanceOf(IllegalArgumentException.class);

        verify(schoolBalanceRepository, never()).findBySchoolIdForUpdateOrCreate(any(), any());
        verify(schoolBalanceEntryRepository, never()).save(any());
    }

    @Test
    void should_refund_the_wallet_when_lowering_an_allocation_that_had_drawn_on_it() {
        // Học sinh đã có sẵn 1.500.000 (vượt pool 500.000 -- coi như đã trích ví từ trước).
        when(subscriptionQuotaUserAllocationRepository.findBySchoolSubscriptionIdAndQuotaType(SUBSCRIPTION_ID, QUOTA_TYPE))
            .thenReturn(List.of(new SchoolSubscriptionQuotaUserAllocation(
                SUBSCRIPTION_ID, QUOTA_TYPE, STUDENT_ID, BigDecimal.valueOf(1_500_000), BigDecimal.ZERO)));
        givenWalletBalance(BigDecimal.valueOf(1_500_000));

        // Hạ xuống còn 1.200.000 -- vẫn vượt pool 200.000, nên vẫn cần xác nhận, nhưng phần dôi
        // 300.000 phải được HOÀN lại ví.
        distribute(BigDecimal.valueOf(1_200_000), true);

        var entry = captureEntry();
        assertThat(entry.getEntryType()).isEqualTo(SchoolBalanceEntryType.ALLOCATION_DRAW);
        assertThat(entry.getAmountVnd()).isEqualByComparingTo(BigDecimal.valueOf(300_000));
        assertThat(entry.getBalanceAfterVnd()).isEqualByComparingTo(BigDecimal.valueOf(1_800_000));
        assertThat(entry.getTargetUserId()).isEqualTo(STUDENT_ID);
    }

    private void distribute(BigDecimal amountVnd, boolean confirmWalletDraw) {
        service.distribute(SCHOOL_ID, QUOTA_TYPE, "STUDENT", DistributionMode.MANUAL,
            List.of(new AllocateUserQuotaAmountCommand(STUDENT_ID, amountVnd)), confirmWalletDraw);
    }

    private void givenWalletBalance(BigDecimal balanceVnd) {
        when(schoolBalanceRepository.findBySchoolId(SCHOOL_ID))
            .thenReturn(Optional.of(new SchoolBalance(SCHOOL_ID, balanceVnd, Instant.now(), Instant.now())));
        when(schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(eq(SCHOOL_ID), any()))
            .thenReturn(new SchoolBalance(SCHOOL_ID, balanceVnd, Instant.now(), Instant.now()));
    }

    private SchoolBalanceEntry captureEntry() {
        var captor = ArgumentCaptor.forClass(SchoolBalanceEntry.class);
        verify(schoolBalanceEntryRepository).save(captor.capture());
        return captor.getValue();
    }
}
