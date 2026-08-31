package com.sep.vox.domain.model.notification;

import java.util.Map;
import java.util.Set;

import com.sep.vox.domain.common.EventTypeConstant;

/**
 * Màn hình mà một thông báo dẫn tới khi người dùng bấm vào.
 *
 * <p>Tên ở đây cố tình KHÔNG phải URL: web, Flutter và desktop có ba bảng route khác hẳn
 * nhau, mà cột {@code notifications.payload} thì sống mãi -- nhúng đường dẫn thật vào đó
 * nghĩa là mọi dòng cũ nói dối ngay hôm đổi tên route. Server chốt "mở cái gì", client tự
 * dịch sang "mở đường dẫn nào".
 *
 * <p>Nhiều eventType dồn về một target là chuyện bình thường, và chính là lý do lớp này
 * tồn tại: năm event nhóm điểm đều dẫn về đúng một màn hình kết quả. Nhờ vậy thêm một loại
 * event mới cho màn hình đã có chỉ cần sửa bảng dưới đây, client không phải phát hành lại
 * -- với app Flutter đó là khác biệt giữa một lần deploy và một vòng duyệt store.
 *
 * <p>Tiền tố nói rõ vai trò người nhận, vì bản thân id không phân biệt được:
 * {@code assignmentId} đi cùng cả {@code GradingDeadlineReminder} (gửi giáo viên) lẫn
 * {@code GradingAssignmentDeclined} (gửi admin đã giao việc), còn {@code schoolId} đi cùng
 * cả event gửi school admin lẫn event gửi system admin. Client đoán theo tên khoá sẽ dẫn
 * nhầm người vào màn hình họ không có quyền xem.
 */
public enum NotificationTarget {

    /** Học sinh: kết quả của một bài thi. */
    EXAM_RESULT_DETAIL,

    /** Học sinh: chi tiết một đơn phúc khảo. */
    EXAM_APPEAL_DETAIL,

    /** Giáo viên: một phân công chấm cụ thể. */
    TEACHER_GRADING_TASK,

    /** School admin: phân công vừa bị giáo viên trả lại, cần giao cho người khác. */
    ADMIN_GRADING_ASSIGNMENT,

    /** School admin: blueprint đề thi vừa được publish. */
    SCHOOL_BLUEPRINT_DETAIL,

    /** School admin: một hoá đơn của trường. */
    SCHOOL_INVOICE_DETAIL,

    /** School admin: trang nợ/hạn mức AI của trường. */
    SCHOOL_BILLING_OVERVIEW,

    /** School admin: gói subscription của trường. */
    SCHOOL_SUBSCRIPTION_DETAIL,

    /** System admin: một trường đang cần chú ý. */
    SYSTEM_SCHOOL_ATTENTION;

    /**
     * Cầu nối giữa {@code eventType} của outbox và màn hình đích.
     *
     * <p>Phải phủ đúng tập eventType mà {@link NotificationCategory} đã khai báo: category
     * quyết định event nào sinh notification, còn bảng này quyết định notification đó mở
     * đi đâu. Hai bảng lệch nhau được chặn lúc khởi động, xem
     * {@code NotificationCategoryMappingValidator}.
     */
    private static final Map<String, NotificationTarget> BY_EVENT_TYPE = Map.ofEntries(
        Map.entry(EventTypeConstant.EXAM_RESULT_RELEASED, EXAM_RESULT_DETAIL),
        Map.entry(EventTypeConstant.EXAM_RESULT_REGRADED, EXAM_RESULT_DETAIL),
        Map.entry(EventTypeConstant.EXAM_RESULT_INVALIDATED, EXAM_RESULT_DETAIL),
        Map.entry(EventTypeConstant.EXAM_RESULT_INVALID_CLEARED, EXAM_RESULT_DETAIL),
        Map.entry(EventTypeConstant.EXAM_RESULT_OUTCOME_DECIDED, EXAM_RESULT_DETAIL),

        Map.entry(EventTypeConstant.EXAM_APPEAL_PUBLISHED, EXAM_APPEAL_DETAIL),
        Map.entry(EventTypeConstant.EXAM_APPEAL_REJECTED, EXAM_APPEAL_DETAIL),
        Map.entry(EventTypeConstant.EXAM_APPEAL_APPROVED, EXAM_APPEAL_DETAIL),

        Map.entry(EventTypeConstant.GRADING_DEADLINE_REMINDER, TEACHER_GRADING_TASK),
        // Cùng mang assignmentId với event trên, nhưng người nhận là admin đã giao việc chứ
        // không phải giáo viên -- nên là target khác, màn hình khác.
        Map.entry(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, ADMIN_GRADING_ASSIGNMENT),

        Map.entry(EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED, SCHOOL_BLUEPRINT_DETAIL),

        Map.entry(EventTypeConstant.INVOICE_PAID, SCHOOL_INVOICE_DETAIL),

        // Ba event nợ/hạn mức gửi school admin đều dẫn về cùng trang tổng quan: chúng không
        // trỏ tới một thực thể cụ thể nào, chỉ nói tình trạng của trường vừa đổi.
        Map.entry(EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, SCHOOL_BILLING_OVERVIEW),
        Map.entry(EventTypeConstant.SCHOOL_DEBT_CLEARED, SCHOOL_BILLING_OVERVIEW),
        Map.entry(EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING, SCHOOL_BILLING_OVERVIEW),

        // Cùng mang schoolId với ba event trên, nhưng người nhận là system admin đang nhìn
        // một trường của người khác, không phải school admin nhìn trường mình.
        Map.entry(EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, SYSTEM_SCHOOL_ATTENTION),

        Map.entry(EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED, SCHOOL_SUBSCRIPTION_DETAIL),
        Map.entry(EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED, SCHOOL_SUBSCRIPTION_DETAIL)
    );

    /**
     * @throws IllegalArgumentException khi eventType chưa được khai báo. Ném thay vì trả về
     *         một target mặc định: đoán bừa sẽ đẩy người dùng vào màn hình không liên quan,
     *         mà lỗi kiểu đó không ai báo lại. Trên thực tế nhánh này không chạy được --
     *         consumer chỉ gọi tới đây sau khi {@link NotificationCategory#isMapped} trả về
     *         true, và validator lúc khởi động bắt buộc hai bảng phủ cùng một tập eventType.
     */
    public static NotificationTarget of(String eventType) {
        var target = BY_EVENT_TYPE.get(eventType);
        if (target == null) {
            throw new IllegalArgumentException("Chưa ánh xạ NotificationTarget cho eventType=" + eventType);
        }
        return target;
    }

    public static boolean isMapped(String eventType) {
        return BY_EVENT_TYPE.containsKey(eventType);
    }

    /** Dùng cho validator lúc khởi động, xem NotificationCategoryMappingValidator. */
    public static Set<String> mappedEventTypes() {
        return BY_EVENT_TYPE.keySet();
    }
}
