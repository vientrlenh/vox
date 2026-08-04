-- Bật lại toàn bộ năm học (school_grades) đang bị kẹt ở trạng thái INACTIVE.
--
-- Import năm học tạo bản ghi với status = 'INACTIVE' (SchoolGradeImportCommitHandler),
-- trong khi thêm tay tạo 'ACTIVE' (CreateSchoolGradeUseCase). Không có luồng nào bật lại
-- được: UpdateSchoolGrade chỉ ghi name/description/start_date/end_date, và import lần sau
-- cũng không đụng tới status. Hệ quả là năm học import xong không tạo/import được lớp
-- (CreateSchoolClassUseCase và SchoolClassImportCommitHandler chỉ nhận năm học ACTIVE).
--
-- Đi cùng lần deploy sửa handler import sang ACTIVE, nếu không dữ liệu import mới lại
-- tiếp tục sinh ra INACTIVE.
--
-- Chỉ đụng INACTIVE; ARCHIVED là bản đã xoá mềm (DeleteSchoolGrade/DeleteSchoolGradeLevel)
-- nên phải giữ nguyên.

update school_grades
set status = 'ACTIVE',
    updated_at = now()
where status = 'INACTIVE';
