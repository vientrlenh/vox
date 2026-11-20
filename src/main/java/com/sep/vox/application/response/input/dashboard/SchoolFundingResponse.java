package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;

import com.sep.vox.domain.common.DecimalText;
import com.sep.vox.domain.model.school.SchoolBalance;

/**
 * Trường còn mở được ca thi hay không, và còn chấm được bao nhiêu trước khi hết.
 *
 * <p>Hai túi tiền KHÁC NHAU, cố ý không gộp thành một con số:
 *
 * <ul>
 *   <li>Hạn mức kèm gói, tách theo {@code QuotaType} — chỉ ví {@code EXAM} trả tiền cho việc chấm
 *       thi. Ví {@code PRACTICE} còn rộng không giúp gì cho kỳ thi tuần sau.
 *   <li>Ví tự nạp — MỘT con số dùng chung, chỉ bị trừ khi hạn mức của loại tương ứng đã cạn.
 * </ul>
 *
 * <p>TIỀN Ở ĐÂY LÀ String, khác {@code tokenAllocated}/{@code tokenUsed} ngay cạnh nó. Lý do ghi ở
 * đầu school-balance.graphqls: số dư là {@code numeric(18,6)} nên double làm tròn sai đúng chỗ con
 * số phải khớp tuyệt đối. Trộn Float và String trong CÙNG một khối tiền còn khó hiểu hơn là lệch với
 * khối bên cạnh, nên mọi field tiền ở đây theo một kiểu.
 */
public record SchoolFundingResponse(
    /** Hạn mức CHẤM THI còn lại. Không bao giờ âm — phần vượt đã chuyển thành nợ trên ví. */
    String examQuotaRemainingVnd,
    String examQuotaTotalVnd,
    /** Số dư ví tự nạp. Âm CHÍNH LÀ khoản nợ. */
    String balanceVnd,
    boolean locked,
    /** Hạn mức thi còn lại cộng số dư ví nếu dương — tiền còn thật sự chấm được. */
    String spendableVnd,
    /** Phần đã hứa cho giáo viên mà họ chưa tiêu. Không bao giờ âm. */
    String committedToUsersVnd,
    /** {@code spendable - committedToUsers}. ÂM = trường đã hứa nhiều hơn số tiền còn lại. */
    String uncommittedVnd
) {

    /**
     * {@code locked} hỏi thẳng {@link SchoolBalance#isInDebt()} chứ không gõ lại phép so với 0: đó
     * đúng là hàm mà {@code SchoolSubscriptionDebtGuardService} gọi để chặn mở ca thi, và một luật
     * thì chỉ nên có một nơi định nghĩa.
     *
     * <p>Ví âm KHÔNG bị cộng vào {@code spendable}: nợ không phải là hạn mức âm mà là một khoản phải
     * trả, và trừ nó khỏi hạn mức còn lại sẽ vẽ ra "còn 3 triệu" cho một trường đang bị khoá cứng.
     *
     * <p><b>{@code spendable} KHÔNG bị {@code committedToUsers} trừ đi</b>, dù hai con số đứng cạnh
     * nhau. {@code spendable} là vị từ KHOÁ — nó phải khớp từng đồng với
     * {@code ClassTestTokenQuotaGuardService.spendableSchoolFundsVnd}, cửa thật sự từ chối lên lịch
     * kỳ thi, và cũng là mẫu số của băng cảnh báo "sắp hết". Trừ phần đã hứa vào đó là làm dashboard
     * báo hết tiền trong khi hệ thống vẫn cho chi — sai lệch tệ hơn hẳn con số hiện tại.
     * {@code uncommitted} trả lời câu hỏi KHÁC: trường còn tự do dùng bao nhiêu cho kỳ thi tập trung
     * mà không ăn vào phần đã hứa cho giáo viên.
     *
     * @param committedToUsersVnd tổng phần CHƯA TIÊU của các trần chi cá nhân, kẹp 0 theo từng dòng —
     *                            xem {@code SchoolSubscriptionQuotaUserAllocationRepository
     *                            .sumUnusedAllocation}. Trừ tổng {@code allocated} thay cho phần chưa
     *                            tiêu là trừ hai lần đúng những đồng giáo viên đã tiêu.
     * @param balance             null khi trường chưa từng chạm vào ví — ví rỗng và ví 0 đồng là cùng
     *                            một nghĩa.
     */
    public static SchoolFundingResponse of(BigDecimal examQuotaTotalVnd, BigDecimal examQuotaUsedVnd,
            BigDecimal committedToUsersVnd, SchoolBalance balance) {
        var remaining = examQuotaTotalVnd.subtract(examQuotaUsedVnd).max(BigDecimal.ZERO);
        var balanceVnd = balance == null ? BigDecimal.ZERO : balance.getBalanceVnd();
        var locked = balance != null && balance.isInDebt();
        var spendable = remaining.add(balanceVnd.max(BigDecimal.ZERO));
        var committed = committedToUsersVnd == null ? BigDecimal.ZERO : committedToUsersVnd;

        return new SchoolFundingResponse(
            DecimalText.of(remaining),
            DecimalText.of(examQuotaTotalVnd),
            DecimalText.of(balanceVnd),
            locked,
            DecimalText.of(spendable),
            DecimalText.of(committed),
            // KHÔNG kẹp về 0: số âm ở đây là thứ đáng báo động nhất mà màn này nói được — trường đã
            // hứa cho giáo viên nhiều hơn số tiền còn lại, nên người tiêu sau cùng sẽ bị từ chối bởi
            // ví trường dù trần cá nhân của họ vẫn còn chỗ. Kẹp về 0 là giấu đúng cảnh báo đó đi.
            DecimalText.of(spendable.subtract(committed))
        );
    }
}
