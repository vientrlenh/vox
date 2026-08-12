-- Mở rộng chk_notification_preferences_category_valid cho nhóm EXAM_BLUEPRINT.
--
-- Lý do: ExamBlueprintVersionPublished vừa được chuyển từ email in-process sang outbox,
-- và nó không thuộc nhóm nào sẵn có -- người nhận là SCHOOL_ADMIN, còn bốn nhóm cũ đều
-- xoay quanh bài thi của học sinh hoặc việc chấm. Gộp tạm vào SYSTEM sẽ khiến người tắt
-- SYSTEM mất luôn các thông báo vận hành khác.
--
-- Danh sách trong CHECK phải khớp đúng enum NotificationCategory. Không có ràng buộc nào
-- ở tầng ứng dụng giữ hai bên đồng bộ, nên thêm hằng số vào enum mà quên migration này
-- thì lỗi chỉ lộ ra lúc người dùng lưu tuỳ chọn -- rất muộn.
--
-- notifications KHÔNG có cột category (nó lưu event_type), nên chỉ một bảng này bị ảnh
-- hưởng. Xem V9__notification.sql.

alter table notification_preferences
    drop constraint chk_notification_preferences_category_valid;

alter table notification_preferences
    add constraint chk_notification_preferences_category_valid
    check (category IN ('EXAM_RESULT', 'EXAM_APPEAL', 'GRADING', 'EXAM_SCHEDULE', 'EXAM_BLUEPRINT', 'SYSTEM'));
