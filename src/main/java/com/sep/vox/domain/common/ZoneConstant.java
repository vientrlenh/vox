package com.sep.vox.domain.common;

import java.time.ZoneId;

/**
 * Múi giờ NGHIỆP VỤ của hệ thống. Mọi khái niệm tính theo lịch địa phương -- "hết hạn sau 1 tháng",
 * "ngày thi", "kỳ sao kê" -- đều phải quy về múi này trước khi cộng trừ, vì cộng trên UTC sẽ lệch
 * một ngày với mọi mốc rơi vào 17:00-23:59 giờ Việt Nam.
 *
 * <p>Đặt ở domain chứ không ở {@code application.common.DateMapper}: đây là một quy ước NGHIỆP VỤ mà
 * chính domain model cần tới (xem {@code SubscriptionPlan.endDateFrom}), mà domain thì không được
 * phụ thuộc ngược lên application -- xem LayeredArchitectureTests. DateMapper trỏ về đây để cả hệ
 * thống chỉ có một nguồn sự thật.
 *
 * <p>Dùng {@link ZoneId} chứ không phải {@code ZoneOffset.ofHours(7)}: offset cứng sai ở những vùng
 * có giờ mùa hè, và {@code atStartOfDay} vốn cần ZoneId.
 */
public final class ZoneConstant {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private ZoneConstant() {
    }
}
