package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.service.OrderSettlementService;
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.domain.model.order.OrderStatus;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.PaymentRecordRepository;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Cái gì SỐNG SÓT qua ranh giới hai kỳ đăng ký -- tiền tự nạp chưa tiêu, và trần chi cá nhân.
 *
 * <p>Đây là chỗ V12 dễ vỡ nhất, và cũng là file DUY NHẤT của lần thay đổi đó chưa có test nào.
 * {@code seedQuotaRecords} dựng LẠI bản ghi hạn mức mỗi kỳ, nên mọi thứ không được chép sang một cách
 * có chủ đích đều biến mất -- lần trước là trần chi (Q-2, "chia lại được"), lần này là TIỀN THẬT của
 * nhà trường (Q-7, "không chia lại được").
 *
 * <p><b>Một phần các test dưới đây ĐANG ĐỎ và cố ý như vậy.</b> Chúng mô tả hành vi đúng của bốn lỗi
 * còn mở, không phải hành vi hiện tại; đọc javadoc của từng test để biết lỗi nào. Các test XANH xen
 * giữa chúng ghim phần đã chạy đúng, để lần sửa sắp tới không đạp lên.
 *
 * <p>Vì có test đỏ, nhánh mang file này KHÔNG được là {@code default}: CI Build là cổng của cd.yml,
 * đỏ ở đó là chặn mọi lần deploy khác.
 */
class OrderSettlementQuotaCarryForwardTests {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private InvoiceRepository invoiceRepository;
    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private SchoolSubscriptionQuotaUserAllocationRepository quotaUserAllocationRepository;
    private SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private SchoolUserRepository schoolUserRepository;
    private RoleRepository roleRepository;
    private OrderSettlementService service;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID previousSubscriptionId = UUID.randomUUID();
    private final UUID newSubscriptionId = UUID.randomUUID();

    private final Instant now = Instant.parse("2026-11-01T03:00:00Z");

    /** Giữ lại để test đọc ngược -- kỳ cũ KHÔNG bị settlement sửa, và đó chính là vấn đề ở ca gia hạn sớm. */
    private final Map<QuotaType, SchoolSubscriptionQuotaRecord> previousPools = new EnumMap<>(QuotaType.class);

    private SchoolSubscription previousSubscription;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        quotaRecordRepository = mock(SchoolSubscriptionQuotaRecordRepository.class);
        quotaUserAllocationRepository = mock(SchoolSubscriptionQuotaUserAllocationRepository.class);
        subscriptionPlanQuotaRepository = mock(SubscriptionPlanQuotaRepository.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        roleRepository = mock(RoleRepository.class);
        var subscriptionPlanRepository = mock(SubscriptionPlanRepository.class);

        service = new OrderSettlementService(
            orderRepository,
            orderItemRepository,
            mock(PaymentRecordRepository.class),
            invoiceRepository,
            mock(SchoolBalanceRepository.class),
            mock(SchoolBalanceEntryRepository.class),
            schoolSubscriptionRepository,
            quotaRecordRepository,
            quotaUserAllocationRepository,
            subscriptionPlanRepository,
            subscriptionPlanQuotaRepository,
            mock(SchoolDebtNotificationService.class),
            schoolUserRepository,
            roleRepository,
            mock(OutboxRepository.class),
            mock(JsonSerializationPort.class));

        var order = new Order();
        order.setId(orderId);
        order.setSchoolId(schoolId);
        order.setType(OrderType.SUBSCRIPTION_REQUEST);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotalAmountVnd(BigDecimal.valueOf(20_000_000));
        order.setTotalAmountVnd(BigDecimal.valueOf(20_000_000));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        var item = new OrderItem();
        item.setOrderId(orderId);
        item.setType(OrderItemType.SUBSCRIPTION);
        item.setItemId(planId);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));

        var plan = new SubscriptionPlan();
        plan.setId(planId);
        plan.setName("Gói năm");
        plan.setPeriodType(SubscriptionPlanPeriod.YEAR);
        plan.setPeriodCount(1);
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        // Hóa đơn coi như đã phát: nhánh đó (và outbox theo sau) không liên quan gì tới việc mang hạn
        // mức sang kỳ mới, cắt đi để test nói đúng một chuyện.
        when(invoiceRepository.existsByOrderId(orderId)).thenReturn(true);

        // Chỉ gán id cho dòng MỚI. cutOverToUpgrade cũng save() các kỳ cũ, và gán đè id ở đó sẽ làm
        // hai kỳ mang cùng một id.
        when(schoolSubscriptionRepository.save(any())).thenAnswer(invocation -> {
            SchoolSubscription saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(newSubscriptionId);
            }
            return saved;
        });

        previousSubscription = new SchoolSubscription();
        previousSubscription.setId(previousSubscriptionId);
        previousSubscription.setSchoolId(schoolId);
        previousSubscription.setStatus(SchoolSubscriptionStatus.ACTIVE);
        previousSubscription.setEndDate(now.minus(1, ChronoUnit.DAYS));
        when(schoolSubscriptionRepository.findMostRecentBySchoolId(schoolId))
            .thenReturn(Optional.of(previousSubscription));

        // Mặc định: kỳ cũ đã hết hạn, nên kỳ mới chạy NGAY -- mốc chốt sổ trùng mốc bắt đầu.
        when(schoolSubscriptionRepository.findUnfinishedBySchoolId(eq(schoolId), any()))
            .thenReturn(List.of());

        givenNewPlanQuotas(Map.of(QuotaType.EXAM, 10_000_000L, QuotaType.PRACTICE, 10_000_000L));
    }

    // =====================================================================
    // XANH -- hành vi đã đúng, ghim lại để lần sửa tới không đạp lên
    // =====================================================================

    @Test
    void renewal_after_expiry_carries_unspent_funding_into_the_new_period() {
        // Q-7. Tiền này KHÔNG còn ở school_balances (đã rời ví kèm bút toán QUOTA_FUNDING), nó nằm
        // ngay trong bản ghi bị dựng lại -- không mang sang là xoá tiền thật vào đúng ngày gia hạn.
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        var seeded = seededPool(QuotaType.PRACTICE);
        assertThat(seeded.getFundedFromBalanceVnd()).isEqualByComparingTo("5000000");
        assertThat(seeded.getTotalAllocatedAmountVnd()).isEqualByComparingTo("15000000");
        assertThat(seeded.getUsedAmountVnd()).isEqualByComparingTo("0");
    }

    @Test
    void only_the_unspent_part_of_the_funding_is_carried() {
        // Quy ước "tiền gói tiêu trước, tiền tự nạp tiêu sau": tiêu 12tr trên ví 15tr có 5tr tự nạp
        // thì chỉ 3tr còn lại được mang sang, 2tr kia đã tiêu thật.
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 12_000_000, 5_000_000);

        settle();

        var seeded = seededPool(QuotaType.PRACTICE);
        assertThat(seeded.getFundedFromBalanceVnd()).isEqualByComparingTo("3000000");
        assertThat(seeded.getTotalAllocatedAmountVnd()).isEqualByComparingTo("13000000");
    }

    @Test
    void renewal_carries_personal_ceilings_so_the_school_does_not_stall() {
        // Q-2. Không chép thì PRACTICE về 0 cho CẢ TRƯỜNG (LEAST + COALESCE), còn EXAM thì mọi giáo
        // viên thành không còn trần -- hai chiều hỏng ngược nhau, cùng im lặng như nhau.
        var teacher = UUID.randomUUID();
        givenPreviousAllocations(QuotaType.EXAM, Map.of(teacher, 2_000_000L));

        settle();

        verify(quotaUserAllocationRepository).upsertAllocation(
            eq(newSubscriptionId), eq(QuotaType.EXAM), eq(teacher), eq(BigDecimal.valueOf(2_000_000)));
    }

    @Test
    void a_first_ever_subscription_carries_nothing() {
        when(schoolSubscriptionRepository.findMostRecentBySchoolId(schoolId)).thenReturn(Optional.empty());

        settle();

        assertThat(seededPool(QuotaType.PRACTICE).getFundedFromBalanceVnd()).isEqualByComparingTo("0");
        verify(quotaUserAllocationRepository, never()).upsertAllocation(any(), any(), any(), any());
    }

    // =====================================================================
    // ĐỎ -- bốn lỗi còn mở. Xem javadoc từng test.
    // =====================================================================

    /**
     * ĐỎ -- gia hạn sớm, chiều "tiền nhân đôi".
     *
     * <p>{@code seedQuotaRecords} chụp ảnh phần chưa tiêu tại lúc TRẢ TIỀN, nhưng
     * {@code nextPeriodStart} cho kỳ mới bắt đầu ở endDate của kỳ cũ. Trong quãng giữa, kỳ cũ vẫn là
     * kỳ {@code findActiveBySchoolId} trả về và ví hạn mức của nó vẫn tiêu được -- nên 5tr đó vừa nằm
     * trong kỳ cũ (tiêu được tới 31/12) vừa đã được chép sang kỳ mới (tiêu lại từ 1/1).
     *
     * <p>Bất biến bị vỡ, và nó không phụ thuộc vào cách sửa: tổng tiền tự nạp đang MỞ trên các kỳ của
     * một trường không được lớn hơn số họ đã thật sự nạp.
     */
    @Test
    void early_renewal_must_not_leave_the_same_funded_money_open_in_two_periods() {
        givenPreviousPeriodStillInForce();
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        var stillOpenOnOldPeriod = previousPools.get(QuotaType.PRACTICE).getFundedFromBalanceVnd();
        var carriedToNewPeriod = seededPool(QuotaType.PRACTICE).getFundedFromBalanceVnd();

        assertThat(stillOpenOnOldPeriod.add(carriedToNewPeriod))
            .as("5tr tự nạp đang mở ở cả hai kỳ cùng lúc")
            .isEqualByComparingTo("5000000");
    }

    /**
     * ĐỎ -- gia hạn sớm, chiều "tiền bốc hơi".
     *
     * <p>Mặt kia của cùng một mốc sai. Trường trả tiền gia hạn tháng 11 rồi nạp thêm 3tr vào tháng 12:
     * khoản đó rơi vào ví hạn mức của KỲ CŨ (kỳ duy nhất đang hiệu lực), không có trong ảnh chụp đã
     * lấy từ tháng 11, và chết theo kỳ cũ ở ranh giới. Đúng lỗi Q-7 sinh ra để chặn, chỉ thu hẹp vào
     * đúng quãng gia hạn sớm.
     *
     * <p>Không thể vá bằng cách chụp ảnh kỹ hơn: mọi con số tính TRƯỚC lúc kỳ cũ ngừng tiêu được đều
     * có thể bị chính kỳ cũ làm sai đi. Phép mang sang phải chạy ở RANH GIỚI. Test này chỉ đòi đúng
     * một điều -- đừng chốt con số đó ngay lúc thanh toán -- và để ngỏ việc chọn job quét hay
     * materialize lúc đọc.
     *
     * <p>Gia hạn khi kỳ cũ đã hết hạn thì hai mốc trùng nhau, và
     * {@link #renewal_after_expiry_carries_unspent_funding_into_the_new_period} giữ nguyên yêu cầu
     * mang sang NGAY ở ca đó.
     */
    @Test
    void funding_is_carried_at_the_period_boundary_not_at_settlement() {
        givenPreviousPeriodStillInForce();
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        verify(quotaRecordRepository, never()).findBySchoolSubscriptionId(previousSubscriptionId);
    }

    /**
     * Hoãn mà không để lại dấu vết thì cũng là mất tiền, chỉ khác đường đi. Kỳ tương lai phải mang
     * theo một cái HẸN trỏ về kỳ nguồn -- đó là toàn bộ thứ nối hai nửa của phép mang sang, và
     * {@code CarryQuotaFundingAtPeriodStartService} không có cách nào khác để tìm ra kỳ nguồn.
     */
    @Test
    void an_early_renewal_leaves_a_promise_pointing_back_at_the_period_it_must_draw_from() {
        givenPreviousPeriodStillInForce();
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        assertThat(seededPool(QuotaType.PRACTICE).getCarryFundingFromSubscriptionId())
            .isEqualTo(previousSubscriptionId);
    }

    @Test
    void a_renewal_that_starts_immediately_leaves_no_promise_behind() {
        // Mốc chốt sổ trùng mốc bắt đầu: đã mang sang ngay tại chỗ, còn hẹn treo là job sẽ cộng lần hai.
        givenPreviousPool(QuotaType.PRACTICE, 15_000_000, 0, 5_000_000);

        settle();

        assertThat(seededPool(QuotaType.PRACTICE).getCarryFundingFromSubscriptionId()).isNull();
    }

    /**
     * ĐỎ -- dòng phân bổ của người đã rời trường được chép sang, mãi mãi.
     *
     * <p>Q-5 đã quyết định những dòng này KHÔNG tính vào trần ({@code sumAllocatedForEligibleUsers}),
     * nhưng {@code carryForwardUserAllocations} vẫn đọc TẤT CẢ dòng của kỳ cũ. Không có đường xoá, nên
     * mỗi lần gia hạn lại dựng lại đúng tập đó cộng thêm người mới nghỉ -- bảng phình theo toàn bộ
     * lịch sử nhân sự, và {@code orphanedAmountVnd} mà màn hình đang hiện cho nhà trường chỉ có tăng.
     *
     * <p>Phép lọc phải là ĐÚNG phép lọc mà {@code sumAllocatedForEligibleUsers} dùng -- ACTIVE + còn
     * thuộc trường + đúng vai trò. {@code findBySchoolIdWithRole} trông có vẻ hợp nhưng lọc
     * {@code status <> DISABLED}, tức vẫn nhận INACTIVE và LOCKED: lỏng hơn trần, và hai bên đếm lệch
     * tập chính là lỗi Q-5.
     */
    @Test
    void allocations_of_users_who_have_left_the_school_are_not_carried_forward() {
        var stillHere = UUID.randomUUID();
        var departed = UUID.randomUUID();
        givenPreviousAllocations(QuotaType.EXAM, Map.of(stillHere, 2_000_000L, departed, 3_000_000L));
        givenEligible(SchoolRoleCodes.TEACHER, stillHere);

        settle();

        verify(quotaUserAllocationRepository, never()).upsertAllocation(
            eq(newSubscriptionId), eq(QuotaType.EXAM), eq(departed), any());
    }

    /**
     * ĐỎ -- trần cá nhân mang sang không bị kẹp vào ví của kỳ MỚI.
     *
     * <p>Gói mới nhỏ hơn (hạ gói, hoặc hết khuyến mãi) thì tổng trần chép sang có thể lớn hơn cả ví.
     * Trường mở kỳ mới trong trạng thái đã vượt trần của chính mình: banner hổ phách bật sẵn, và mọi
     * lần sửa tay sau đó bị từ chối bằng "vượt quá phần được phép chia" -- đúng triệu chứng Q-6, vào
     * lại bằng cửa gia hạn thay vì cửa chia đều.
     *
     * <p>Test kẹp ở mức chắc chắn đúng -- không bao giờ chép sang nhiều hơn ví đang có. Trần thật còn
     * chặt hơn ({@code distributableAmountOf}, theo tỉ lệ của trường), nhưng chính sách đó nằm ở
     * repository mà service này chưa cầm, nên để người sửa chọn.
     */
    @Test
    void carried_allocations_are_clamped_to_the_new_periods_pool() {
        givenNewPlanQuotas(Map.of(QuotaType.EXAM, 10_000_000L));
        givenPreviousAllocations(QuotaType.EXAM,
            Map.of(UUID.randomUUID(), 9_000_000L, UUID.randomUUID(), 9_000_000L));

        settle();

        assertThat(carriedAmounts(QuotaType.EXAM).stream().reduce(BigDecimal.ZERO, (a, b) -> a.add(b)))
            .as("chép sang 18tr trần cá nhân vào một ví chỉ có 10tr")
            .isLessThanOrEqualTo(BigDecimal.valueOf(10_000_000));
    }

    /**
     * ĐỎ -- chép trần cho loại ví mà gói mới KHÔNG còn.
     *
     * <p>{@code carryForwardUserAllocations} duyệt {@code QuotaType.values()} rồi upsert vô điều kiện,
     * không hỏi kỳ mới có ví loại đó không. Trường chuyển sang gói không kèm PRACTICE sẽ có một loạt
     * dòng phân bổ trỏ vào một ví không tồn tại.
     *
     * <p>{@code seedQuotaRecords} đã xử đúng ca đối xứng cho TIỀN -- nó ghi WARN kèm số tiền không
     * mang sang được. Phía trần thì im lặng. Danh sách loại ví đã dựng có sẵn ở đó (seededTypes) và
     * {@code seedQuotaRecords} vốn đã chạy trước, nên chỉ là chuyện chuyền tay.
     */
    @Test
    void allocations_are_not_carried_for_quota_types_the_new_plan_dropped() {
        givenNewPlanQuotas(Map.of(QuotaType.EXAM, 10_000_000L));
        givenPreviousAllocations(QuotaType.PRACTICE, Map.of(UUID.randomUUID(), 1_000_000L));

        settle();

        verify(quotaUserAllocationRepository, never()).upsertAllocation(
            any(), eq(QuotaType.PRACTICE), any(), any());
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private void settle() {
        var payment = new PaymentRecord();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setAmountVnd(BigDecimal.valueOf(20_000_000));
        service.settlePaid(payment, now);
    }

    /** Gia hạn SỚM: kỳ cũ còn hạn, nên kỳ mới xếp hàng phía sau nó thay vì chạy ngay. */
    private void givenPreviousPeriodStillInForce() {
        previousSubscription.setEndDate(now.plus(60, ChronoUnit.DAYS));
        when(schoolSubscriptionRepository.findUnfinishedBySchoolId(eq(schoolId), any()))
            .thenReturn(List.of(previousSubscription));
    }

    private void givenNewPlanQuotas(Map<QuotaType, Long> includedByType) {
        var quotas = includedByType.entrySet().stream()
            .map(entry -> new SubscriptionPlanQuota(
                planId, entry.getKey(), BigDecimal.valueOf(entry.getValue())))
            .toList();
        when(subscriptionPlanQuotaRepository.findBySubscriptionPlanId(planId)).thenReturn(quotas);
    }

    private void givenPreviousPool(QuotaType quotaType, long totalVnd, long usedVnd, long fundedVnd) {
        previousPools.put(quotaType, new SchoolSubscriptionQuotaRecord(
            UUID.randomUUID(), previousSubscriptionId, quotaType,
            BigDecimal.valueOf(totalVnd), BigDecimal.valueOf(usedVnd), BigDecimal.valueOf(fundedVnd)));
        when(quotaRecordRepository.findBySchoolSubscriptionId(previousSubscriptionId))
            .thenReturn(new ArrayList<>(previousPools.values()));
    }

    private void givenPreviousAllocations(QuotaType quotaType, Map<UUID, Long> allocatedByUser) {
        var rows = allocatedByUser.entrySet().stream()
            .map(entry -> new SchoolSubscriptionQuotaUserAllocation(
                previousSubscriptionId, quotaType, entry.getKey(),
                BigDecimal.valueOf(entry.getValue()), BigDecimal.ZERO))
            .toList();
        when(quotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaType(previousSubscriptionId, quotaType))
            .thenReturn(rows);
    }

    /**
     * Những người CÒN đủ điều kiện nhận trần của vai trò này. Không gọi helper thì roleRepository trả
     * Optional rỗng và service chép TOÀN BỘ -- đúng hành vi fail-open đã chọn, nên các test khác ở đây
     * không cần biết gì về vai trò.
     */
    private void givenEligible(String roleCode, UUID... userIds) {
        var roleId = UUID.randomUUID();
        var role = new Role();
        role.setId(roleId);
        when(roleRepository.findByCode(roleCode)).thenReturn(Optional.of(role));

        var members = Arrays.stream(userIds).map(this::schoolUser).toList();
        when(schoolUserRepository.findBySchoolId(
                eq(schoolId), any(), eq(roleId), eq(UserStatus.ACTIVE.name()), any(), eq(false), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(members, 1, members.size(), members.size(), 1));
    }

    private SchoolUser schoolUser(UUID userId) {
        var schoolUser = new SchoolUser();
        schoolUser.setUserId(userId);
        return schoolUser;
    }

    private SchoolSubscriptionQuotaRecord seededPool(QuotaType quotaType) {
        var captor = ArgumentCaptor.forClass(SchoolSubscriptionQuotaRecord.class);
        verify(quotaRecordRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
            .filter(record -> record.getQuotaType() == quotaType)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Kỳ mới không có ví " + quotaType + " nào được dựng"));
    }

    private List<BigDecimal> carriedAmounts(QuotaType quotaType) {
        var captor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(quotaUserAllocationRepository, atLeastOnce()).upsertAllocation(
            eq(newSubscriptionId), eq(quotaType), any(), captor.capture());
        return captor.getAllValues();
    }
}
