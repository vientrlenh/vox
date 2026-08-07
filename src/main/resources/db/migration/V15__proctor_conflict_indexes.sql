-- Chặn giám thị gác hai ca thi trùng giờ.
--
-- Không thêm ràng buộc cấp CSDL: Postgres EXCLUDE đòi cột thời gian nằm ngay trên hàng bị ràng
-- buộc, mà start_date/end_date sống ở bảng cha exam_schedules. Nhân đôi giờ xuống
-- exam_schedule_proctors rồi tự đồng bộ thì hại nhiều hơn lợi, nên luật được giữ ở tầng ứng dụng
-- (ExamScheduleProctorConflictValidator), trong cùng transaction với khoá bi quan trên ca thi.
--
-- Hai chỉ mục dưới đây phục vụ câu truy vấn kiểm tra chồng lấn mới
-- (ExamScheduleProctorRepository.existsOverlappingAssignment): tra theo giáo viên rồi lọc theo
-- khung giờ của các ca còn hiệu lực.

create index if not exists idx_exam_schedule_proctors_teacher_id
    on exam_schedule_proctors (teacher_id);

create index if not exists idx_exam_schedules_active_window
    on exam_schedules (start_date, end_date)
    where status in ('DRAFT', 'PUBLISHED');
