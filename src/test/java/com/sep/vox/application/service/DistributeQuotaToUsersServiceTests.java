package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.port.input.command.subscription.AllocateUserQuotaAmountCommand;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationPageResponse;
import com.sep.vox.domain.common.DistributionMode;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Phép chia hạn mức cá nhân -- nơi quản trị trường biến ví cấp trường thành trần chi cho từng người.
 *
 * <p>Bốn bất biến mà bộ test này giữ, cả bốn đều đã từng sai và cả bốn đều hỏng trong im lặng:
 *
 * <ul>
 *   <li><b>Chỉ người CÒN đủ điều kiện mới chiếm chỗ trong trần.</b> Dòng phân bổ không bị xoá khi
 *       giáo viên nghỉ việc, mà bảng thì chỉ hiện người đang tại chức -- cộng cả những dòng vô hình
 *       đó là để chúng ăn dần trần cho tới lúc mọi lần chia đều bị từ chối, với một câu báo lỗi không
 *       khớp với bất kỳ con số nào trên màn hình.</li>
 *   <li><b>Chia đều tự động phải khớp ĐÚNG phần được phép chia.</b> Không dư (vượt trần của chính
 *       trường ngay sau một cú bấm) và không thiếu (mất tiền vì làm tròn).</li>
 *   <li><b>"Chưa chia" khác "chia 0 đồng".</b> Hai trạng thái cho hành vi ngược nhau ở cửa chặn, nên
 *       tầng đọc phải phân biệt được chúng thay vì trả cùng một số 0.</li>
 *   <li><b>Trần phân phối KHÔNG phải khả năng chi trả.</b> Trần tính trên tổng hạn mức gói và không
 *       nhỏ đi khi ví bị tiêu; số tiền thật sự còn trả được là một con số RIÊNG, và màn hình phải nói
 *       ra cả hai.</li>
 * </ul>
 */
class DistributeQuotaToUsersServiceTests {

    private UserContextPort userContextPort;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository allocationRepository;
    private SchoolUserRepository schoolUserRepository;
    private SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;
    private SchoolBalanceRepository schoolBalanceRepository;
    private DistributeQuotaToUsersService service;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final UUID teacherA = UUID.randomUUID();
    private final UUID teacherB = UUID.randomUUID();
    /** Đã nghỉ việc: dòng phân bổ còn nguyên trong DB nhưng không còn ở bất kỳ trang nào của bảng. */
    private final UUID formerTeacher = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userContextPort = mock(UserContextPort.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        subscriptionQuotaRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        allocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        schoolQuotaPolicyRepository = mock(SchoolQuotaPolicyRepository.class);
        schoolBalanceRepository = mock(SchoolBalanceRepository.class);

        service = new DistributeQuotaToUsersService(
            userContextPort,
            schoolSubscriptionRepository,
            subscriptionQuotaRepository,
            allocationRepository,
            roleRepository,
            schoolUserRepository,
            schoolQuotaPolicyRepository,
            schoolBalanceRepository);

        when(userContextPort.isSystemAdmin()).thenReturn(false);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);

        SchoolSubscription subscription = new SchoolSubscription();
        subscription.setId(subscriptionId);
        subscription.setSchoolId(schoolId);
        when(schoolSubscriptionRepository.findActiveBySchoolId(schoolId)).thenReturn(Optional.of(subscription));

        when(roleRepository.findByCode(any()))
            .thenReturn(Optional.of(Role.builder()
            .id(roleId)
            .build()));

        when(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(eq(schoolId), any()))
            .thenReturn(SchoolQuotaPolicy.fullyDistributable(schoolId, QuotaType.ASSESSMENT));

        when(schoolBalanceRepository.findBySchoolId(schoolId)).thenReturn(Optional.empty());

        givenPool(10_000_000, 0);
        givenEligible(teacherA, teacherB);
        givenExistingAllocations();
        givenAllocationSums(0, 0);
    }

    // ---------------------------------------------------------------------
    // Người không còn đủ điều kiện
    // ---------------------------------------------------------------------

    @Test
    void manual_allocation_ignores_rows_of_users_who_are_no_longer_eligible() {
        // Người đã nghỉ đang giữ 6 triệu trên giấy tờ. Cộng vào thì 8 + 6 > 10 và lần chia này bị từ
        // chối -- trong khi bảng chỉ hiện hai giáo viên với tổng 0đ, nên lời từ chối đó không thể
        // đối chiếu với bất cứ thứ gì người dùng nhìn thấy.
        givenExistingAllocations(allocation(formerTeacher, 6_000_000, 0));

        assertThatCode(() -> allocateManually(teacherA, 8_000_000)).doesNotThrowAnyException();

        verify(allocationRepository).upsertAllocation(
            subscriptionId, QuotaType.ASSESSMENT, teacherA, BigDecimal.valueOf(8_000_000));
    }

    @Test
    void manual_allocation_still_counts_rows_of_users_who_are_still_eligible() {
        // Mặt kia của cùng một luật: bỏ qua người đã nghỉ KHÔNG có nghĩa là bỏ qua mọi người vắng mặt
        // trong yêu cầu. Đồng nghiệp còn đang tại chức thì phần của họ vẫn chiếm chỗ.
        givenExistingAllocations(allocation(teacherB, 6_000_000, 0));

        assertThatThrownBy(() -> allocateManually(teacherA, 8_000_000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vượt quá phần được phép chia");
    }

    @Test
    void view_page_reports_orphaned_allocation_separately_from_distributed() {
        // 6.5 million is on the ledger, but only 4 million belongs to users who are still eligible.
        // The difference must be named: if it is silently dropped, "already distributed" on screen
        // does not match the DB, and if it is added in, it eats the ceiling of the people who
        // actually need it.
        givenAllocationSums(4_000_000, 6_500_000);

        QuotaUserAllocationPageResponse page = service.viewPage(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, null, 1, 20);

        assertThat(page.distributedAmountVnd()).isEqualByComparingTo("4000000");
        assertThat(page.orphanedAmountVnd()).isEqualByComparingTo("2500000");
    }

    // ---------------------------------------------------------------------
    // Chia đều tự động
    // ---------------------------------------------------------------------

    @Test
    void auto_split_distributes_exactly_the_distributable_amount() {
        UUID teacherC = UUID.randomUUID();
        givenEligible(teacherA, teacherB, teacherC);
        // 10,000,000 / 3 does not divide evenly -- the remainder must still be spread out and must
        // not evaporate.
        List<BigDecimal> amounts = captureAutoSplitAmounts(3);

        assertThat(amounts.stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b)))
            .isEqualByComparingTo("10000000");
    }

    @Test
    void auto_split_does_not_overshoot_when_former_staff_still_hold_allocations() {
        // Đây chính là cái bẫy cũ: chia đều phát hết phần được phép chia cho người đủ điều kiện, còn
        // dòng của người đã nghỉ thì không ai đụng tới -- nên ngay sau một cú "chia đều tự động",
        // tổng vượt trần và mọi lần sửa tay tiếp theo đều bị từ chối. Giờ tổng chỉ đếm người đủ điều
        // kiện, nên chia đều xong là vừa khít trần, và sửa tay ngay sau đó vẫn chạy.
        givenExistingAllocations(allocation(formerTeacher, 6_000_000, 0));
        captureAutoSplitAmounts(2);

        givenExistingAllocations(
            allocation(formerTeacher, 6_000_000, 0),
            allocation(teacherA, 5_000_000, 0),
            allocation(teacherB, 5_000_000, 0));

        assertThatCode(() -> allocateManually(teacherA, 5_000_000)).doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------------
    // "Chưa chia" khác "chia 0 đồng"
    // ---------------------------------------------------------------------

    @Test
    void view_page_distinguishes_never_allocated_from_explicit_zero() {
        // If both show "0", the school admin cannot tell that half of their teachers have NO
        // spending ceiling at all -- see the javadoc of QuotaUserAllocationPageResponse.Row.
        givenExistingAllocations(allocation(teacherB, 0, 0));

        QuotaUserAllocationPageResponse page = service.viewPage(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, null, 1, 20);

        QuotaUserAllocationPageResponse.Row rowA = page.content().stream().filter(row -> row.userId().equals(teacherA)).findFirst().orElseThrow();
        QuotaUserAllocationPageResponse.Row rowB = page.content().stream().filter(row -> row.userId().equals(teacherB)).findFirst().orElseThrow();

        assertThat(rowA.allocatedAmountVnd()).isNull();
        assertThat(rowA.usedAmountVnd()).isEqualByComparingTo("0");
        assertThat(rowB.allocatedAmountVnd()).isEqualByComparingTo("0");
    }

    // ---------------------------------------------------------------------
    // Trần phân phối vs khả năng chi trả
    // ---------------------------------------------------------------------

    @Test
    void distributable_cap_ignores_pool_consumption_while_spendable_funds_do_not() {
        // The wallet has spent 7 of 10 million. The ceiling is still 10 million -- ON PURPOSE: a
        // personal spending ceiling must stay stable for the whole period; if it shrank every time
        // someone spent, the school admin would have to redistribute constantly. But the amount
        // that can really still be paid is only 3 million, and that is the number the screen must
        // warn about.
        givenPool(10_000_000, 7_000_000);

        QuotaUserAllocationPageResponse page = service.viewPage(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, null, 1, 20);

        assertThat(page.distributableAmountVnd()).isEqualByComparingTo("10000000");
        assertThat(page.spendableFundsVnd()).isEqualByComparingTo("3000000");
    }

    @Test
    void spendable_funds_add_wallet_balance_but_never_subtract_debt() {
        // A negative balance is DEBT, and debt is already blocked at a separate gate with its own
        // message. Subtracting it here would turn a locked school into a school that has "run out
        // of quota" -- reporting the wrong reason, which only points to the wrong fix.
        givenPool(10_000_000, 7_000_000);
        givenWallet(-5_000_000);

        QuotaUserAllocationPageResponse page = service.viewPage(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, null, 1, 20);

        assertThat(page.spendableFundsVnd()).isEqualByComparingTo("3000000");
        assertThat(page.walletBalanceVnd()).isEqualByComparingTo("0");
    }

    // ---------------------------------------------------------------------
    // Cổng xác nhận rút ví
    // ---------------------------------------------------------------------

    @Test
    void allocation_beyond_cap_needs_explicit_wallet_confirmation() {
        givenWallet(4_000_000);

        assertThatThrownBy(() -> allocateManually(teacherA, 12_000_000))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Phân bổ này vượt phần chia từ gói, sẽ trích thêm 2000000.000000đ từ ví tự nạp của trường "
                + "(đang còn 4000000đ). Xác nhận để tiếp tục.");

        assertThatCode(() -> service.distribute(
                schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, DistributionMode.MANUAL,
                List.of(new AllocateUserQuotaAmountCommand(teacherA, BigDecimal.valueOf(12_000_000))), true))
            .doesNotThrowAnyException();
    }

    @Test
    void allocation_beyond_cap_and_wallet_is_refused_even_with_confirmation() {
        // Xác nhận là để người dùng biết mình đang ăn vào ví chung, không phải để đi vòng qua giới hạn.
        givenWallet(1_000_000);

        assertThatThrownBy(() -> service.distribute(
                schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, DistributionMode.MANUAL,
                List.of(new AllocateUserQuotaAmountCommand(teacherA, BigDecimal.valueOf(12_000_000))), true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("số dư ví tự nạp");
    }

    @Test
    void allocation_below_already_spent_amount_is_refused() {
        givenExistingAllocations(allocation(teacherA, 5_000_000, 3_000_000));

        assertThatThrownBy(() -> allocateManually(teacherA, 1_000_000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nhỏ hơn số lượng đã sử dụng");
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private void allocateManually(UUID userId, long amountVnd) {
        service.distribute(
            schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, DistributionMode.MANUAL,
            List.of(new AllocateUserQuotaAmountCommand(userId, BigDecimal.valueOf(amountVnd))), false);
    }

    /**
     * Runs the even distribution and returns the amounts that were written -- used to add them back
     * up and check against the ceiling.
     */
    private List<BigDecimal> captureAutoSplitAmounts(int expectedUsers) {
        service.distribute(schoolId, QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, DistributionMode.AUTO, null, false);

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(allocationRepository, times(expectedUsers)).upsertAllocation(
            eq(subscriptionId), eq(QuotaType.ASSESSMENT), any(), amountCaptor.capture());
        return amountCaptor.getAllValues();
    }

    private void givenPool(long totalVnd, long usedVnd) {
        SchoolSubscriptionQuotaRecord pool = SchoolSubscriptionQuotaRecord.builder()
            .schoolSubscriptionId(subscriptionId)
            .quotaType(QuotaType.ASSESSMENT)
            .totalAllocatedAmountVnd(BigDecimal.valueOf(totalVnd))
            .usedAmountVnd(BigDecimal.valueOf(usedVnd))
            .build();
        pool.setId(UUID.randomUUID());
        when(subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
            .thenReturn(Optional.of(pool));
    }

    private void givenEligible(UUID... userIds) {
        List<SchoolUser> content = java.util.Arrays.stream(userIds)
            .map(userId -> SchoolUser.builder()
                .schoolId(schoolId)
                .userId(userId)
                .startDate(null)
                .endDate(null)
                .build())
            .toList();
        when(schoolUserRepository.findBySchoolId(
                any(), any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(content, 1, 20, content.size(), 1));
    }

    private void givenExistingAllocations(SchoolSubscriptionQuotaUserAllocation... allocations) {
        when(allocationRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, QuotaType.ASSESSMENT))
            .thenReturn(List.of(allocations));
    }

    private void givenAllocationSums(long eligibleVnd, long allVnd) {
        when(allocationRepository.sumAllocatedForEligibleUsers(any(), any(), any(), any(), any()))
            .thenReturn(BigDecimal.valueOf(eligibleVnd));
        when(allocationRepository.sumAllocated(any(), any())).thenReturn(BigDecimal.valueOf(allVnd));
    }

    private void givenWallet(long balanceVnd) {
        when(schoolBalanceRepository.findBySchoolId(schoolId))
            .thenReturn(Optional.of(SchoolBalance.builder()
                .schoolId(schoolId)
                .balanceVnd(BigDecimal.valueOf(balanceVnd))
                .createdAt(null)
                .updatedAt(null)
                .build()));
    }

    private SchoolSubscriptionQuotaUserAllocation allocation(UUID userId, long allocatedVnd, long usedVnd) {
        return SchoolSubscriptionQuotaUserAllocation.builder()
            .schoolSubscriptionId(subscriptionId)
            .quotaType(QuotaType.ASSESSMENT)
            .userId(userId)
            .allocatedAmountVnd(BigDecimal.valueOf(allocatedVnd))
            .usedAmountVnd(BigDecimal.valueOf(usedVnd))
            .build();
    }
}
