package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;

/**
 * MỘT TRANG của màn chia hạn mức cá nhân, kèm hai con số tổng cho cả tập.
 *
 * <p>Thay cho {@code QuotaUserAllocationSummaryResponse} vốn trả về TOÀN BỘ người đủ điều kiện trong
 * một lượt: một trường có vài nghìn học sinh thì đó là vài nghìn dòng cho mỗi lần mở trang, và giao
 * diện cũng không dựng nổi ngần ấy hàng.
 *
 * <p><b>Vì sao phải có {@code distributedAmountVnd} ở đây.</b> Trước đây giao diện tự cộng cột phân
 * bổ của mọi dòng để biết trường đã chia hết bao nhiêu và còn lại bao nhiêu. Khi chỉ còn một trang
 * trong tay thì phép cộng đó cho ra số SAI -- nên tổng phải do phía máy chủ tính trên toàn bộ tập,
 * cùng khuôn với {@code SchoolBalanceSummaryResponse}.
 *
 * @param pool                   ví hạn mức cấp trường -- EXAM khi chia cho giáo viên, PRACTICE khi
 *                               chia cho học sinh
 * @param distributedAmountVnd   tổng đã chia cho TẤT CẢ người dùng, không phải chỉ trang này
 * @param distributableRatio     trần phân phối của trường cho loại hạn mức này, 0..1
 * @param distributableAmountVnd phần ví được phép chia ra = pool x distributableRatio. Trả sẵn thay
 *                               vì để client tự nhân: đây là con số mà backend dùng để từ chối, nên
 *                               một phép nhân thứ hai ở client là một cơ hội để hai bên lệch nhau
 *                               vài phần triệu đồng rồi báo lỗi ở chỗ người dùng không hiểu nổi
 * @param content                người đủ điều kiện ở trang đang xem, kể cả người chưa được chia gì
 */
public record QuotaUserAllocationPageResponse(
    SchoolSubscriptionQuotaRecordDto pool,
    BigDecimal distributedAmountVnd,
    BigDecimal distributableRatio,
    BigDecimal distributableAmountVnd,
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
     * <p>{@code allocatedAmountVnd} = 0 với người chưa được chia gì. Đó là dòng ẢO, không có trong
     * DB -- màn chia hạn mức phải hiện được cả người đang có 0 thì quản trị trường mới biết còn ai
     * chưa chia.
     */
    public record Row(
        UUID userId,
        BigDecimal allocatedAmountVnd,
        BigDecimal usedAmountVnd
    ) {
    }
}
