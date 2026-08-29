package com.sep.vox.application.response.input.balance;

import java.math.BigDecimal;

import com.sep.vox.domain.common.DecimalText;

/**
 * Cộng dồn sổ cái ví theo một khoảng thời gian, gom theo ba nhóm bút toán.
 *
 * <p>Là RESPONSE của use case chứ không phải DTO ở domain, khác với {@code SchoolBalanceDto} và
 * {@code SchoolBalanceEntryDto}: hai cái kia ánh xạ 1-1 từ một domain model có thật, còn cái này
 * không có model nào đứng sau -- nó là kết quả use case ghép từ bốn phép cộng dồn riêng biệt. Đặt ở
 * domain/dto thì phải bịa ra một "SchoolBalanceSummary" mà nghiệp vụ không hề có.
 *
 * <p>Phải tính ở DB chứ không dựng được ở client: client chỉ cầm một trang 20 dòng, còn dải cần cộng
 * có thể hàng nghìn. Đây cũng là nguồn cho con số "đã trừ trong 30 ngày qua" trên trang Số dư ví.
 *
 * <p>CỐ Ý không có số đếm bút toán: con số đó đã nằm ở {@code totalElements} của trang sao kê, và
 * hai nơi cùng đếm một thứ là hai nơi có thể lệch nhau.
 */
public record SchoolBalanceSummaryResponse(
    String creditedVnd,
    String overageChargedVnd,
    String adjustedVnd
) {

    /**
     * @param topUp      tổng TOP_UP  ({@code >= 0})
     * @param refund     tổng REFUND  ({@code >= 0}) -- gộp chung với nạp vì với người đọc sao kê thì
     *                   cả hai đều là "tiền vào ví"; tách ra là bắt họ tự cộng lại
     * @param overage    tổng OVERAGE_CHARGE ({@code <= 0})
     * @param adjustment tổng ADJUSTMENT (dấu nào cũng có thể)
     */
    public static SchoolBalanceSummaryResponse of(
            BigDecimal topUp, BigDecimal refund, BigDecimal overage, BigDecimal adjustment) {
        return new SchoolBalanceSummaryResponse(
            DecimalText.orZero(nullSafe(topUp).add(nullSafe(refund))),
            DecimalText.orZero(overage),
            DecimalText.orZero(adjustment)
        );
    }

    // Repository đã COALESCE về 0, nhưng cộng hai giá trị lại thì một null lọt qua sẽ thành
    // NullPointerException ở đúng ca "trường chưa có bút toán nào" -- ca thường gặp nhất.
    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
