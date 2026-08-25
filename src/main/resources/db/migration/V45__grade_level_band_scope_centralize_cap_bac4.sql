-- Trần CENTRALIZE (kiểm tra tập trung, không neo vào 1 Lớp cụ thể) của Khối 10/11/12 nới lên
-- Bậc 4 (trước là Bậc 3) -- xem GradeLevelBandScopeInitializer đã đổi BAND_CODE_DEFAULT_TARGET.
-- Trần CLASS_TEST (hard_max_band_id) đã đúng Bậc 5 từ V44, không cần đụng. Initializer chỉ INSERT
-- dòng còn thiếu, không UPDATE dòng đã có, nên dữ liệu seed cũ không tự sửa lại theo code mới --
-- cần migration này để chỉnh lại dòng đã tồn tại.
UPDATE grade_level_band_scopes gs
SET default_target_band_id = band4.id,
    updated_at = now()
FROM grade_levels gl,
     framework_result_bands band4
WHERE gs.grade_level_id = gl.id
  AND gl.code IN ('GRADE_10', 'GRADE_11', 'GRADE_12')
  AND band4.framework_version_id = gs.framework_version_id
  AND band4.code = 'BAC_4';
