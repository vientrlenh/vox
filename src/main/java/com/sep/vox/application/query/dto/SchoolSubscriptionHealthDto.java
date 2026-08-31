package com.sep.vox.application.query.dto;

/**
 * Phân loại TỪNG TRƯỜNG theo tình trạng gói dịch vụ tại một ngày, mỗi trường rơi vào đúng một nhóm.
 *
 * <p>Phải phân loại theo trường chứ không đếm theo dòng {@code school_subscriptions}: một trường có
 * nhiều kỳ thuê bao trong lịch sử, nên đếm dòng sẽ vừa cộng trùng vừa xếp một trường đang dùng tốt
 * vào nhóm "đã hết hạn" chỉ vì kỳ năm ngoái của nó đã hết.
 *
 * <p>"Còn hiệu lực" gồm cả {@code CANCELLED}: trạng thái đó chỉ tắt gia hạn tự động, trường vẫn dùng
 * được tới hết {@code endDate} — xem {@code SchoolSubscriptionStatus}. Chỉ {@code SUSPENDED} mới là
 * mất quyền dùng ngay.
 */
public record SchoolSubscriptionHealthDto(
    /** Trường có kỳ thuê bao phủ ngày đang xét. */
    long subscribedSchools,
    /** Tập con của {@code subscribedSchools}: kỳ phủ hiện tại sắp hết trong ngưỡng cảnh báo. */
    long expiringSoonSchools,
    /** Từng có gói nhưng hiện không kỳ nào phủ, và không bị đình chỉ. */
    long lapsedSchools,
    /** Bị system admin đình chỉ và hiện không có kỳ nào phủ. */
    long suspendedSchools
) {
}
