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
 * khối bên cạnh, nên cả bốn field ở đây theo một kiểu.
 */
public record SchoolFundingResponse(
    /** Hạn mức CHẤM THI còn lại. Không bao giờ âm — phần vượt đã chuyển thành nợ trên ví. */
    String examQuotaRemainingVnd,
    String examQuotaTotalVnd,
    /** Số dư ví tự nạp. Âm CHÍNH LÀ khoản nợ. */
    String balanceVnd,
    boolean locked,
    /** Hạn mức thi còn lại cộng số dư ví nếu dương — tiền còn thật sự chấm được. */
    String spendableVnd
) {

    /**
     * {@code locked} hỏi thẳng {@link SchoolBalance#isInDebt()} chứ không gõ lại phép so với 0: đó
     * đúng là hàm mà {@code SchoolSubscriptionDebtGuardService} gọi để chặn mở ca thi, và một luật
     * thì chỉ nên có một nơi định nghĩa.
     *
     * <p>Ví âm KHÔNG bị cộng vào {@code spendable}: nợ không phải là hạn mức âm mà là một khoản phải
     * trả, và trừ nó khỏi hạn mức còn lại sẽ vẽ ra "còn 3 triệu" cho một trường đang bị khoá cứng.
     *
     * @param balance null khi trường chưa từng chạm vào ví — ví rỗng và ví 0 đồng là cùng một nghĩa.
     */
    public static SchoolFundingResponse of(BigDecimal examQuotaTotalVnd, BigDecimal examQuotaUsedVnd,
            SchoolBalance balance) {
        var remaining = examQuotaTotalVnd.subtract(examQuotaUsedVnd).max(BigDecimal.ZERO);
        var balanceVnd = balance == null ? BigDecimal.ZERO : balance.getBalanceVnd();
        var locked = balance != null && balance.isInDebt();
        var spendable = remaining.add(balanceVnd.max(BigDecimal.ZERO));

        return new SchoolFundingResponse(
            DecimalText.of(remaining),
            DecimalText.of(examQuotaTotalVnd),
            DecimalText.of(balanceVnd),
            locked,
            DecimalText.of(spendable)
        );
    }
}
