package com.sep.vox.domain.common;

import java.math.BigDecimal;

/**
 * Đưa một khoản tiền ra dây dưới dạng chuỗi thập phân nguyên vẹn.
 *
 * <p>Dùng cho mọi cột {@code numeric(18,6)} của ví trường (school_balances, school_balance_entries,
 * school_debt_events). Những cột đó cố ý giữ 6 số lẻ vì một lượt ôn luyện có thể chỉ tốn vài phần
 * trăm đồng, nên chúng KHÔNG thoả tiền đề của quy ước Float trên schema GraphQL ("VND luôn nguyên").
 * {@code 128440.95} không biểu diễn chính xác được bằng double, mà đây đúng là chỗ con số phải khớp
 * tuyệt đối: bất biến của sổ cái là {@code SUM(amount_vnd) = balance_vnd}.
 *
 * <p>{@code toPlainString()} chứ không {@code toString()}: {@code toString()} chuyển sang ký hiệu
 * khoa học khi số mũ đủ nhỏ ("1E-7"), và client parse chuỗi đó thành số tiền thì ra một thứ khác
 * hẳn. Chuỗi ở đây giữ nguyên scale đã lưu, nên "-128440.950000" chứ không phải "-128440.95" -- cố ý
 * để hai lần đọc cùng một bút toán luôn cho cùng một chuỗi, dù giá trị có tròn đồng hay không.
 *
 * <p>Chuyển ở tầng Java chứ không bằng {@code str()} trong JPQL: chuỗi này giờ là một phần HỢP ĐỒNG
 * với client, nên nó phải do mã của mình dựng ra một cách xác định, không phải do cách một phiên bản
 * Postgres nào đó render {@code cast(numeric as varchar)}.
 */
public final class DecimalText {

    private DecimalText() {}

    /** null vào thì null ra -- cột tiền nullable (triggerAmountVnd, costUsd) giữ nguyên nghĩa "không có". */
    public static String of(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /**
     * Cho cột {@code NOT NULL} mà câu tổng hợp có thể trả về null (SUM trên 0 dòng). Trường chưa có
     * bút toán nào là ca THƯỜNG GẶP nhất chứ không phải ca biên, nên nó phải ra "0" thay vì làm
     * hỏng một field non-null trên schema.
     */
    public static String orZero(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }
}
