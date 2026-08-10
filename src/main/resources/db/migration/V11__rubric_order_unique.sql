-- Dữ liệu cũ có thể đã bị trùng order do thiếu validate trước đây (bug đang fix) — dồn lại về
-- thứ tự liên tục 1..N cho từng rubric_version_id, giữ nguyên thứ tự tương đối hiện có, trước khi
-- ép unique constraint để tránh migration fail trên DB đã tồn tại dữ liệu trùng.
WITH renumbered AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY rubric_version_id ORDER BY criterion_order, created_at, id) AS rn
    FROM rubric_criterions
)
UPDATE rubric_criterions c
SET criterion_order = renumbered.rn
FROM renumbered
WHERE c.id = renumbered.id AND c.criterion_order <> renumbered.rn;

alter table rubric_criterions
    add constraint idx_rubric_criterions_version_order unique (rubric_version_id, criterion_order);

WITH renumbered AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY rubric_version_id ORDER BY result_order, created_at, id) AS rn
    FROM rubric_result_bands
)
UPDATE rubric_result_bands r
SET result_order = renumbered.rn
FROM renumbered
WHERE r.id = renumbered.id AND r.result_order <> renumbered.rn;

alter table rubric_result_bands
    add constraint idx_rubric_result_bands_version_order unique (rubric_version_id, result_order);