package com.sep.vox.application.query.repository;

import java.math.BigDecimal;
import java.time.Instant;

import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;

/**
 * Đọc sức khỏe KINH DOANH của nền tảng cho dashboard system admin: bao nhiêu trường còn gói, bao
 * nhiêu sắp rụng, và giá vốn AI để đặt cạnh doanh thu.
 *
 * <p>Là một BÁO CÁO chứ không phải cổng ghi của aggregate nào — cùng khuôn với
 * {@link PlatformOperationalHealthQueryRepository}. Gộp ở đây thay vì thêm hàng loạt {@code countBy...}
 * vào {@code SchoolSubscriptionRepository} và {@code SchoolBalanceRepository}: mỗi màn thống kê mới
 * lại nhét một phép đếm nữa vào cổng nghiệp vụ thì tới lúc không còn phân biệt được cái nào là mô
 * hình nghiệp vụ, cái nào chỉ phục vụ một cái biểu đồ.
 */
public interface PlatformBusinessHealthQueryRepository {

    /**
     * Nhận {@link Instant} chứ không phải {@code LocalDate}: {@code school_subscriptions.start_date}
     * và {@code end_date} đã được V2 đổi sang {@code timestamptz} (mục "ALTER COLUMN start_date TYPE
     * timestamp with time zone"), nên so với một {@code date} sẽ để Postgres tự ép ngày đó thành mốc
     * nửa đêm THEO MÚI GIỜ CỦA SESSION — một phụ thuộc ẩn vào cấu hình JDBC, đúng thứ
     * {@code ZoneConstant} cảnh báo. So instant với instant thì không còn gì để đoán.
     *
     * @param now             thời điểm xét kỳ thuê bao nào đang phủ
     * @param expiringThrough ngưỡng cảnh báo sắp hết hạn, BAO GỒM
     */
    SchoolSubscriptionHealthDto countSchoolSubscriptionHealth(Instant now, Instant expiringThrough);

    /**
     * Trường có ví tự nạp âm — chính là trường đang bị chặn mở ca thi.
     *
     * <p>Không có cờ khóa nào để đọc: trạng thái khóa được SUY RA từ số dư mỗi lần hỏi, xem
     * {@code SchoolBalance.isInDebt()} và {@code SchoolSubscriptionDebtGuardService}. Điều kiện ở đây
     * ({@code balance_vnd < 0}) phải khớp với vị từ đó, nếu không dashboard sẽ đếm ra một tập trường
     * khác với tập thật sự đang bị chặn.
     *
     * <p>CẮT NGANG các nhóm thuê bao: đọc từ bảng khác nên một trường đang còn gói mà ví âm sẽ vào
     * cả hai số đếm. Đừng cộng con số này với ba nhóm kia.
     */
    long countSchoolsInDebt();

    /**
     * Giá vốn AI thực tế đã phát sinh trong kỳ, quy ra VND để đặt được cạnh doanh thu.
     *
     * <p>Dùng {@code cost_vnd} chứ không {@code cost_usd}: doanh thu ghi bằng VND, mà trừ hai đơn vị
     * khác nhau thì ra một con số không có nghĩa gì. Không lọc {@code charged_at} — biên lợi nhuận
     * hỏi chi phí đã PHÁT SINH, còn đã thu được tiền của trường hay chưa là chuyện khác.
     *
     * @param from bao gồm
     * @param to   KHÔNG bao gồm, cùng quy ước với {@code OrderRepository.sumTotalAmountByStatusInRange}
     * @return không bao giờ null; kỳ không có bản ghi nào trả {@link BigDecimal#ZERO}
     */
    BigDecimal sumAiCostVnd(Instant from, Instant to);
}
