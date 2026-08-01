-- Bỏ optimistic locking trên exam_grading_assignments, thay bằng khoá bi quan.
--
-- Cột version do V3 thêm vào để chặn hai luồng cùng ghi đè một phân công. Nó được thay
-- bằng SELECT ... FOR UPDATE ở ExamGradingAssignmentRepository#findByIdForUpdate, dùng
-- trong các luồng đọc-sửa-ghi (chấm bài, đổi giáo viên, đặt hạn) — cùng khuôn với các
-- repository khác trong dự án.
--
-- BẮT BUỘC đi cùng lần deploy gỡ @Version khỏi entity: cột đang là NOT NULL không có
-- DEFAULT, nên khi Hibernate thôi đưa version vào câu INSERT mà cột vẫn còn thì MỌI lần
-- tạo phân công mới sẽ fail. Ngược lại, drop cột trước khi code lên cũng hỏng vì
-- ddl-auto: validate không thấy cột mà entity đang khai báo.

alter table if exists exam_grading_assignments drop column if exists version;
