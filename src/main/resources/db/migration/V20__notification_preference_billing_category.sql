-- Mở rộng chk_notification_preferences_category_valid cho nhóm BILLING.
--
-- Lý do: InvoicePaid vừa được chuyển từ email in-process sang outbox và nay sinh cả
-- notification in-app. Người nhận là SCHOOL_ADMIN, nội dung là chứng từ thanh toán --
-- không thuộc nhóm nào sẵn có. Gộp vào SYSTEM sẽ khiến người tắt SYSTEM mất luôn thông
-- báo hóa đơn, thứ mà gần như không ai muốn tắt.
--
-- Danh sách trong CHECK phải khớp đúng enum NotificationCategory. Xem thêm V19.

alter table notification_preferences
    drop constraint chk_notification_preferences_category_valid;

alter table notification_preferences
    add constraint chk_notification_preferences_category_valid
    check (category IN ('EXAM_RESULT', 'EXAM_APPEAL', 'GRADING', 'EXAM_SCHEDULE', 'EXAM_BLUEPRINT', 'BILLING', 'SYSTEM'));
