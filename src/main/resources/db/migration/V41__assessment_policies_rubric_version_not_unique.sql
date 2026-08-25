-- Nới lỏng lại: 1 RubricVersion giờ được phép gắn với nhiều AssessmentPolicy (khác scope).
-- Hạ unique index thêm ở V38 xuống lại thành index thường như trước đó, chỉ để tăng tốc
-- truy vấn theo rubric_version_id, không còn ràng buộc duy nhất.
drop index if exists idx_assessment_policies_rubric_version;

create index idx_assessment_policies_rubric_version on assessment_policies (rubric_version_id);
