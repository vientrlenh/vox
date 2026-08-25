-- Chỉ Khối 12 được nới trần lên Bậc 4 cho lớp chuyên; Khối 10/11 ép trần về đúng mặc định (Bậc 3).
-- GradeLevelBandScopeInitializer là ApplicationRunner idempotent-theo-dòng (chỉ INSERT dòng còn
-- thiếu, không UPDATE dòng đã có), nên dữ liệu seed cũ (trần Bậc 4 đồng nhất cho cả 3 khối) không
-- tự sửa lại theo code initializer mới -- cần migration này để chỉnh lại dòng đã tồn tại.
UPDATE grade_level_band_scopes gs
SET hard_max_band_id = gs.default_target_band_id,
    updated_at = now()
FROM grade_levels gl
WHERE gs.grade_level_id = gl.id
  AND gl.code IN ('GRADE_10', 'GRADE_11');
