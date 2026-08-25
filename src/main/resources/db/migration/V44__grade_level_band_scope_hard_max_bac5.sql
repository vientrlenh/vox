-- Trần chung của Khối 10/11/12 nới lên Bậc 5 (không còn phân biệt theo khối như V42) -- xem
-- GradeLevelBandScopeInitializer đã đổi BAND_CODE_HARD_MAX. Initializer chỉ INSERT dòng còn
-- thiếu, không UPDATE dòng đã có, nên dữ liệu seed cũ (trần Bậc 3 hoặc Bậc 4 tùy khối) không tự
-- sửa lại theo code initializer mới -- cần migration này để chỉnh lại dòng đã tồn tại.
UPDATE grade_level_band_scopes gs
SET hard_max_band_id = band5.id,
    updated_at = now()
FROM grade_levels gl,
     framework_result_bands band5
WHERE gs.grade_level_id = gl.id
  AND gl.code IN ('GRADE_10', 'GRADE_11', 'GRADE_12')
  AND band5.framework_version_id = gs.framework_version_id
  AND band5.code = 'BAC_5';
