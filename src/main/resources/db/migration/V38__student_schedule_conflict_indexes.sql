-- Chặn học sinh bị xếp hai ca thi trùng giờ (kể cả ở hai kỳ thi khác nhau).
--
-- Không thêm ràng buộc cấp CSDL, cùng lý do đã ghi ở V17: Postgres EXCLUDE đòi cột thời gian nằm
-- ngay trên hàng bị ràng buộc, mà start_date/end_date sống ở bảng cha exam_schedules. Nhân đôi giờ
-- xuống exam_candidates rồi trigger-sync mỗi lần ca đổi giờ thì hại nhiều hơn lợi, nên luật được
-- giữ ở tầng ứng dụng (ExamScheduleCandidateConflictValidator), trong cùng transaction với khoá bi
-- quan trên ca thi.
--
-- Rủi ro còn lại được chấp nhận: hai transaction song song xếp cùng một học sinh vào hai ca chồng
-- giờ ở hai kỳ thi KHÁC NHAU khoá hai hàng exam_schedules khác nhau nên không serialize, cả hai
-- cùng đọc thấy học sinh đang rảnh và cùng commit. Đây đúng là hạng rủi ro mà luật giám thị đang
-- chấp nhận từ V17 -- cho thí sinh một đảm bảo mạnh hơn giám thị là bất đối xứng vô lý.
--
-- uq_exam_candidates_exam_student là (exam_id, student_id) nên tra theo student_id KHÔNG dùng được
-- chỉ mục đó: student_id không phải cột dẫn đầu. Hai chỉ mục dưới đây phục vụ câu truy vấn kiểm tra
-- chồng lấn mới (ExamCandidateRepository.findConflictsForStudents) ở cả hai chiều join. Lọc
-- schedule_id is not null vì thí sinh chưa xếp ca không bao giờ nằm trong kết quả.
--
-- Vế lọc theo khung giờ đã có idx_exam_schedules_active_window (V17), không cần chỉ mục mới trên
-- exam_schedules.

create index if not exists idx_exam_candidates_student_id
    on exam_candidates (student_id)
    where schedule_id is not null;

create index if not exists idx_exam_candidates_schedule_id
    on exam_candidates (schedule_id)
    where schedule_id is not null;
