package com.sep.vox.domain.model.notification;

import java.util.Map;
import java.util.Set;

import com.sep.vox.domain.common.EventTypeConstant;

public enum NotificationCategory {
    EXAM_RESULT,
    EXAM_APPEAL,
    GRADING,
    EXAM_SCHEDULE,
    EXAM_BLUEPRINT,
    BILLING,
    SYSTEM;

    /**
     * Cầu nối giữa {@code eventType} của outbox và nhóm tuỳ chọn nhận thông báo.
     *
     * <p>Chỉ chứa những eventType thực sự sinh notification. Các loại vắng mặt đều có chủ
     * ý: {@code UserCreated} và {@code RegisterFormRejected} gửi tới người chưa từng đăng
     * nhập, còn hai event OTP thì gửi tới người đang đứng ngoài phiên đăng nhập. Cả bốn
     * đều chưa có thiết bị nào để đẩy -- xem consumer-groups.notification.topic trong
     * application.yaml, nơi các topic đó cũng không được đăng ký.
     */
    private static final Map<String, NotificationCategory> BY_EVENT_TYPE = Map.ofEntries(
        Map.entry(EventTypeConstant.EXAM_APPEAL_PUBLISHED, EXAM_APPEAL),
        Map.entry(EventTypeConstant.EXAM_APPEAL_REJECTED, EXAM_APPEAL),
        Map.entry(EventTypeConstant.EXAM_APPEAL_APPROVED, EXAM_APPEAL),

        Map.entry(EventTypeConstant.EXAM_RESULT_RELEASED, EXAM_RESULT),
        Map.entry(EventTypeConstant.EXAM_RESULT_REGRADED, EXAM_RESULT),
        Map.entry(EventTypeConstant.EXAM_RESULT_INVALIDATED, EXAM_RESULT),
        Map.entry(EventTypeConstant.EXAM_RESULT_INVALID_CLEARED, EXAM_RESULT),
        Map.entry(EventTypeConstant.EXAM_RESULT_OUTCOME_DECIDED, EXAM_RESULT),

        Map.entry(EventTypeConstant.GRADING_DEADLINE_REMINDER, GRADING),
        Map.entry(EventTypeConstant.GRADING_ASSIGNMENT_DECLINED, GRADING), 
        Map.entry(EventTypeConstant.EXAM_BLUEPRINT_VERSION_PUBLISHED, EXAM_BLUEPRINT),

        Map.entry(EventTypeConstant.INVOICE_PAID, BILLING),

        Map.entry(EventTypeConstant.SCHOOL_DEBT_CAP_EXCEEDED, SYSTEM),
        Map.entry(EventTypeConstant.SCHOOL_LOCKED_DUE_TO_DEBT, BILLING),
        Map.entry(EventTypeConstant.SCHOOL_DEBT_CLEARED, BILLING),
        Map.entry(EventTypeConstant.SCHOOL_QUOTA_USAGE_WARNING, BILLING),

        Map.entry(EventTypeConstant.SCHOOL_SUBSCRIPTION_SUSPENDED, BILLING),
        Map.entry(EventTypeConstant.SCHOOL_SUBSCRIPTION_UNSUSPENDED, BILLING)
    );

    /**
     * @throws IllegalArgumentException khi eventType chưa được khai báo. Ném thay vì trả
     *         về SYSTEM: một eventType lạ nghĩa là ai đó thêm event mới mà quên ánh xạ,
     *         và nuốt lỗi ở đây sẽ biến nó thành thông báo sai nhóm -- người dùng đã tắt
     *         nhóm đó vẫn bị làm phiền, còn lập trình viên thì không hề hay biết.
     */
    public static NotificationCategory of(String eventType) {
        var category = BY_EVENT_TYPE.get(eventType);
        if (category == null) {
            throw new IllegalArgumentException("Chưa ánh xạ NotificationCategory cho eventType=" + eventType);
        }
        return category;
    }

    public static boolean isMapped(String eventType) {
        return BY_EVENT_TYPE.containsKey(eventType);
    }

    /** Dùng cho validator lúc khởi động, xem NotificationCategoryMappingValidator. */
    public static Set<String> mappedEventTypes() {
        return BY_EVENT_TYPE.keySet();
    }
}
