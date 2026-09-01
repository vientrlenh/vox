package com.sep.vox.domain.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * "Đã chờ bao nhiêu ngày" cho các thẻ hàng đợi.
 *
 * <p>Đếm theo NGÀY LỊCH giờ nghiệp vụ, không phải elapsed chia 24 giờ: đơn nộp 23:00 hôm qua mà bây
 * giờ là 01:00 thì người trực đọc là "đã sang ngày thứ hai", trong khi phép chia cho 24 giờ vẫn trả
 * 0 và làm thẻ trông như hàng đợi vẫn sạch.
 *
 * <p>Mọi thẻ đếm ngày chờ phải đi qua đây. Ba chỗ đang dùng — đơn đăng ký trường chờ duyệt, đơn khiếu
 * nại chờ xử lý, bài chưa có điểm — và ba bản sao của cùng một phép tính chỉ cần lệch một chi tiết
 * (múi giờ, hay ngày lịch với 24 giờ) là ba màn hình đếm ngày theo ba kiểu.
 */
public final class BusinessDays {

    private BusinessDays() {
    }

    /**
     * Số ngày lịch từ {@code from} tới {@code now}, theo {@link ZoneConstant#BUSINESS_ZONE}.
     *
     * @param from mốc bắt đầu; null nghĩa là KHÔNG CÓ GÌ ĐANG CHỜ và trả về null.
     * @return null khi {@code from} null — null KHÁC 0, và ở những thẻ này 0 lại là trạng thái tốt
     *         nhất ("vừa nộp hôm nay"), nên gộp hai cái sẽ vẽ hàng đợi rỗng y hệt hàng đợi vừa nhận
     *         việc.
     */
    public static Integer waitedDaysSince(Instant from, Instant now) {
        if (from == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(
            from.atZone(ZoneConstant.BUSINESS_ZONE).toLocalDate(),
            now.atZone(ZoneConstant.BUSINESS_ZONE).toLocalDate());
    }
}
