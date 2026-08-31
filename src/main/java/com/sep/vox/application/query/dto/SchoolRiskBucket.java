package com.sep.vox.application.query.dto;

/**
 * Bốn nhóm "trường cần chú ý" trên trang tổng quan hệ thống.
 *
 * <p>KHÔNG phải một phép chia nhóm — đừng cộng bốn số lại:
 *
 * <ul>
 *   <li>{@link #EXPIRING_SOON} là TẬP CON của nhóm trường còn gói.</li>
 *   <li>{@link #LAPSED} và {@link #SUSPENDED} loại trừ nhau và loại trừ nhóm còn gói.</li>
 *   <li>{@link #IN_DEBT} CẮT NGANG cả ba: nó đọc từ số dư ví chứ không từ trạng thái thuê bao, nên
 *       một trường đang còn gói mà ví âm nằm ở cả hai chỗ.</li>
 * </ul>
 *
 * <p>Trường CHƯA TỪNG mua gói nào không thuộc ba nhóm đầu: chúng không có dòng nào trong
 * {@code school_subscriptions} để gộp, nên vị từ của cả ba trả về NULL và loại chúng ra. Đó là hành
 * vi GIỐNG HỆT phép đếm của trang tổng quan, và giữ nguyên là có chủ đích — hai màn hình phải nói
 * cùng một con số trước đã; có nên thêm nhóm thứ năm cho chúng hay không là câu hỏi riêng.
 */
public enum SchoolRiskBucket {
    /** Còn gói, nhưng kỳ đang phủ sẽ hết trong ngưỡng cảnh báo. */
    EXPIRING_SOON,
    /** Từng có gói, hiện không kỳ nào phủ, và không bị đình chỉ. */
    LAPSED,
    /** Bị đình chỉ và hiện không có kỳ nào phủ. */
    SUSPENDED,
    /** Ví tự nạp âm — chính là trường đang bị chặn mở ca thi. */
    IN_DEBT
}
