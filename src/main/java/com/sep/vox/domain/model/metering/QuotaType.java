package com.sep.vox.domain.model.metering;

import com.sep.vox.domain.model.user.SchoolRoleCodes;

/**
 * Hai VÍ hạn mức cấp trường, đều tính bằng VND. Mỗi giá trị ở đây là một túi tiền RIÊNG: tiêu hết
 * túi này không đụng tới túi kia, và mỗi hoạt động chỉ trừ vào ĐÚNG MỘT túi.
 *
 * <p>Tách EXAM/PRACTICE là một quyết định NGHIỆP VỤ chứ không phải kế toán: nếu gộp chung một ví,
 * một tuần học sinh luyện nói nhiều bất thường sẽ ăn hết ngân sách dành cho kỳ thi cuối kỳ. Ranh
 * giới này trùng khít với {@link QuotaPricingSource} -- hai pipeline AI khác hẳn nhau (evalGraph
 * chấm thi nặng hơn realtimeCorrectionGraph của luyện nói) nên cũng calibrate hai rate riêng.
 *
 * <p>KHÔNG có CLASS_TEST. Trước đây nó đứng ngang hàng ở đây nhưng bản chất là TRẦN CHI nằm BÊN
 * TRONG ví EXAM, không phải ví thứ ba: một bài kiểm tra trên lớp bị trừ cùng một khoản
 * {@code totalCostUsd} hai lần -- một lần dưới GRADING, một lần dưới CLASS_TEST -- khiến
 * {@code SUM(used_amount_vnd)} luôn cao hơn tiền thật, và phải dựng riêng ràng buộc
 * {@code chk_school_balance_entries_no_class_test_charge} để chặn khoản trừ trùng đó chạm vào tiền.
 * Trần chi theo GIÁO VIÊN vẫn còn nguyên và vẫn là tính năng thật -- nó sống ở
 * {@link com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation} với
 * quotaType = EXAM, và chỉ được soi khi bài có {@code kind = CLASS_TEST} (xem
 * ClassTestTokenQuotaGuardService). Trần đó là một GIỚI HẠN, không bao giờ là một số dư: nó không
 * giữ tiền, không ghi nợ, không xuất hiện trong school_balance_entries.
 *
 * <p>KHÔNG có SPEAKING. Chi phí Azure voice live là một THÀNH PHẦN chi phí, không phải một hoạt
 * động -- nó đã được ghi sẵn ở {@code ai_usage_record.usage_type = 'DURATION'}, theo từng lượt và
 * kèm provider. Cả thi lẫn luyện nói đều phát sinh nó, nên dựng nó thành ví thứ ba sẽ lặp lại đúng
 * lỗi của CLASS_TEST theo chiều khác: một kỳ thi phải trừ vào hai ví, và tệ hơn CLASS_TEST ở chỗ
 * ví SPEAKING nằm vắt qua CẢ HAI hoạt động nên luyện nói lại rút cạn được phần thi -- mở lại đúng
 * cái cửa mà việc tách EXAM/PRACTICE sinh ra để đóng. Muốn biết "tháng này nói tốn bao nhiêu" thì
 * GROUP BY trên ai_usage_record.usage_type, không cần thêm ví.
 */
public enum QuotaType {
    /** Chấm thi -- mọi ExamKind (CENTRALIZED lẫn CLASS_TEST) đều trừ vào đây. */
    EXAM,
    /** Học sinh tự luyện nói. */
    PRACTICE;

    /**
     * Vai trò nhận trần chi cá nhân của ví này: EXAM chia cho GIÁO VIÊN, PRACTICE chia cho HỌC SINH.
     *
     * <p>Đặt ở đây vì đã có ba chỗ cần đúng một câu trả lời -- hai use case chia hạn mức
     * (AllocateExamQuotaToTeachers / AllocatePracticeQuotaToStudents) và phép lọc "còn đủ điều kiện"
     * lúc mang trần sang kỳ mới (OrderSettlementService.carryForwardUserAllocations). Ánh xạ này quyết
     * định AI giữ được trần qua một lần gia hạn, nên hai bên lệch nhau là mất trần của người thật.
     */
    public String allocationRoleCode() {
        return switch (this) {
            case EXAM -> SchoolRoleCodes.TEACHER;
            case PRACTICE -> SchoolRoleCodes.STUDENT;
        };
    }
}
