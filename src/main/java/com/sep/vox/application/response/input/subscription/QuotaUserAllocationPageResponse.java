package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;

/**
 * ONE PAGE of the personal quota allocation screen, with two totals for the whole set.
 *
 * <p>Replaces {@code QuotaUserAllocationSummaryResponse}, which returned ALL eligible people in
 * one go: a school with several thousand students means several thousand rows each time the page opens, and the UI
 * cannot build that many rows either.
 *
 * <p><b>Why {@code distributedAmountVnd} has to be here.</b> Previously the UI summed the allocation column of
 * every row itself to know how much the school had distributed and how much remained. When only one page
 * is in hand that sum gives a WRONG number -- so the total must be computed on the server over the whole set,
 * in the same mold as {@code SchoolBalanceSummaryResponse}.
 *
 * @param pool                   the school-level quota wallet -- ASSESSMENT when distributing to teachers, PRACTICE when
 *                               distributing to students
 * @param distributedAmountVnd   the total distributed to people who are STILL eligible, over the whole set and
 *                               not just this page. People who have left/graduated are NOT added
 *                               -- see {@code orphanedAmountVnd}
 * @param orphanedAmountVnd      the part still held under the names of people who are NO LONGER eligible (deactivated,
 *                               left the school, changed role). It does not count toward the ceiling and does not appear on
 *                               any page of the table, so it has to be said in words here: otherwise the total on screen and the sum of the rows
 *                               differ and nobody can explain why. Usually 0
 * @param distributableRatio     the school's distribution ceiling for this quota type, 0..1
 * @param distributableAmountVnd the part of the wallet allowed to be distributed = pool x distributableRatio. Returned ready-made instead
 *                               of letting the client multiply: this is the number the backend uses to reject, so
 *                               a second multiplication on the client is a chance for the two sides to diverge
 *                               by a few millionths of a dong and then report an error where the user cannot understand why
 * @param spendableFundsVnd      the money the school can REALLY still pay for this quota type = remaining plan
 *                               quota + self-topped-up wallet balance (clamped to 0). COMPLETELY DIFFERENT from
 *                               distributableAmountVnd, which is computed on total_allocated and does not
 *                               decrease when the wallet is spent: a school that has spent its wallet can still distribute
 *                               within the ceiling, and that ceiling says nothing about the ability to pay.
 *                               The UI uses it to WARN, not to block -- a spending ceiling is a
 *                               ceiling, and the real block already exists at the gates when opening an assessment / building a practice
 *                               paper (ClassTestTokenQuotaGuardService, findPracticeSpendableFundsVnd)
 * @param walletBalanceVnd       the part of the school's SELF-TOPPED-UP wallet (school_balances) that CAN be eaten into beyond
 *                               distributableAmountVnd when loosening the personal ceiling of ONE person -- not
 *                               a reserved pocket of money, and SHARED by both ASSESSMENT and PRACTICE
 *                               (see the BusinessRuleViolationException thrown when a wallet draw is not confirmed). Clamped to 0, negative is debt.
 * @param content                the eligible people on the page being viewed, including those who have been allocated nothing
 */
public record QuotaUserAllocationPageResponse(
    SchoolSubscriptionQuotaRecordDto pool,
    BigDecimal distributedAmountVnd,
    BigDecimal orphanedAmountVnd,
    BigDecimal distributableRatio,
    BigDecimal distributableAmountVnd,
    BigDecimal spendableFundsVnd,
    BigDecimal walletBalanceVnd,
    List<Row> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    /**
     * Một người trong danh sách chia hạn mức.
     *
     * <p>KHÔNG mang tên người dùng: tên được nối vào ở tầng GraphQL qua data loader {@code userById},
     * nên một trang 20 dòng chỉ tốn thêm ĐÚNG MỘT truy vấn users thay vì 20. Đây cũng là chỗ sửa lỗi
     * cột "Họ tên" luôn hiện dấu gạch: {@code SchoolSubscriptionQuotaUserAllocationDto} chưa bao giờ
     * có trường tên, còn client thì khai sẵn một trường như thế và không bao giờ nhận được giá trị.
     *
     * <p><b>{@code allocatedAmountVnd} null = CHƯA CHIA, khác hẳn 0.</b> Hai trạng thái này cho ra
     * hành vi NGƯỢC NHAU và trước đây bị gộp thành cùng một số 0 trên màn hình:
     *
     * <ul>
     *   <li>chưa có dòng phân bổ (null) = KHÔNG bị chặn theo cá nhân. Với giáo viên nghĩa là tiêu
     *       thoải mái trong ví trường ({@code ClassTestTokenQuotaGuardService.remainingUserAllocation}
     *       trả null thì cửa chặn bỏ qua luôn); với học sinh thì ngược lại, LEAST(...) trong
     *       {@code findPracticeSpendableFundsVnd} kéo về 0 nên em không luyện được lượt nào.</li>
     *   <li>có dòng và bằng 0 = bị chặn hẳn, ở cả hai loại.</li>
     * </ul>
     *
     * <p>Nên một trường có nửa số giáo viên "0 ₫" có thể đang là nửa số giáo viên KHÔNG có trần chi
     * nào cả -- và trên bảng cũ thì hai ca đó trông y hệt nhau. Client phải hiện thành hai nhãn khác
     * nhau ("Chưa phân bổ" vs "0 ₫"), nên kiểu dữ liệu ở đây phải phân biệt được chúng.
     *
     * <p>{@code usedAmountVnd} thì luôn có số: chưa có dòng nghĩa là chưa tiêu đồng nào qua trần cá
     * nhân, và 0 ở đó không mơ hồ.
     */
    public record Row(
        UUID userId,
        BigDecimal allocatedAmountVnd,
        BigDecimal usedAmountVnd
    ) {
    }
}
