-- Siết 1 RubricVersion chỉ được gắn với đúng 1 AssessmentPolicy (mọi trạng thái, kể cả ARCHIVED).
-- Trước đó rubric_version_id chỉ có index thường (idx_assessment_policies_rubric_version), cho phép
-- nhiều policy khác scope cùng trỏ vào 1 rubric version. Nếu DB đã tồn tại dữ liệu vi phạm (nhiều
-- policy cùng rubric_version_id), migration này SẼ FAIL khi tạo unique index -- đó là dữ liệu
-- nghiệp vụ thật, không tự động dồn được như V11 (thứ tự có thể renumber vô hại), cần người quyết
-- định archive/xoá bớt trước khi chạy lại.
drop index if exists idx_assessment_policies_rubric_version;

create unique index idx_assessment_policies_rubric_version on assessment_policies (rubric_version_id);
