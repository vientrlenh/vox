package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;

/**
 * Kết quả của MỘT lần trừ chi phí (ConsumeQuotaService) -- không phải ảnh chụp của một dòng nào.
 *
 * <p>Không nhét mấy con số này vào {@link SchoolSubscriptionQuotaRecordDto} vì cùng lý do đã nêu ở
 * {@link SchoolSubscriptionQuotaRecordResponse}: chúng không phải cột của ví hạn mức. Nặng hơn thế,
 * một lần trừ giờ động vào HAI aggregate khác nhau (ví hạn mức + số dư trường), nên một DTO chiếu
 * đúng một dòng không thể diễn tả đủ dù có thêm cột.
 *
 * <p>Các số dẫn xuất được tính NGAY trong transaction đã giữ khóa dòng số dư rồi trả ra đây, thay vì
 * để chỗ gọi tự truy vấn lại sau khi commit: hai lần đọc ngoài khóa có thể không khớp với chính cái
 * vừa xảy ra bên trong khóa.
 *
 * @param quota               ví hạn mức SAU khi trừ
 * @param chargedToQuotaVnd   phần nằm trong hạn mức kèm gói
 * @param chargedToBalanceVnd phần VƯỢT, đã ghi thành bút toán OVERAGE_CHARGE (0 nếu không vượt).
 *                            Luôn có {@code chargedToQuotaVnd + chargedToBalanceVnd = số tiền đã trừ}
 * @param balanceAfterVnd     số dư ví tự nạp sau bút toán -- ÂM nghĩa là trường đang nợ
 */
public record ConsumeQuotaResponse(
    SchoolSubscriptionQuotaRecordDto quota,
    BigDecimal chargedToQuotaVnd,
    BigDecimal chargedToBalanceVnd,
    BigDecimal balanceAfterVnd,

    /**
     * CHÍNH lần trừ này đẩy trường từ không nợ sang nợ -- một CHUYỂN TIẾP, chỉ đúng đúng một lần.
     * Dùng để phát thông báo. Đang nợ sẵn từ trước thì cờ này false.
     */
    boolean crossedIntoDebt,

    /**
     * Hết sạch cả hạn mức lẫn số dư -- một TRẠNG THÁI, đúng ở mọi lần trừ tiếp theo chứ không chỉ
     * lần đầu. Dùng để đóng phiên luyện nói một cách tử tế (SubmitTurnResultDto.quotaExhausted).
     *
     * <p>Cố ý tách khỏi {@link #crossedIntoDebt}: một cái báo "vừa mới xảy ra", một cái báo "đang
     * như vậy". Gộp làm một thì hoặc thông báo lặp lại mỗi lượt, hoặc phiên không bao giờ đóng.
     */
    boolean fundsExhausted
) {
}
