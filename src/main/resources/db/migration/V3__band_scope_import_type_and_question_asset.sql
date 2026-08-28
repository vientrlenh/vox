-- Gộp V41..V47 cũ thành MỘT migration, đặt sau V2 (orders/payments/school balance) của main.
--
-- Vì sao phải gộp: nhánh này và main không còn chung lịch sử migration. Main chỉ có V1 + V2, còn
-- nhánh này có V1 + V41..V47. Giữ nguyên số cũ thì V2 (số nhỏ) nằm sau V47 (số lớn) trong lịch sử
-- đã chạy, và Flyway từ chối khởi động: "Detected resolved migration not applied to database: 2".
--
-- BỎ HẲN V43__invoice_created_by.sql. Nó chạy `ALTER TABLE invoice ADD COLUMN created_by`, mà V2
-- của main đã `DROP TABLE IF EXISTS invoice` -- cả miền hoá đơn được thiết kế lại thành
-- orders/order_items/payment_records/invoices. Giữ lại là chắc chắn chết ở dòng đầu tiên.
--
-- Giữ nguyên thứ tự và giữ CẢ V42 lẫn V44 dù nhìn qua V44 có vẻ ghi đè V42: V44 chỉ có tác dụng
-- khi tra được framework_result_bands.code = 'BAC_5' khớp framework_version_id. Không có dòng đó
-- thì UPDATE của V44 chạm 0 dòng và phần V42 làm cho Khối 10/11 mới là thứ còn lại. Bỏ V42 là đổi
-- hành vi trong đúng ca đó -- không đáng đổi lấy vài dòng gọn hơn.


-- === V41: nới lại ràng buộc rubric version trên assessment_policies ===
-- 1 RubricVersion được phép gắn với nhiều AssessmentPolicy (khác scope). Hạ unique index thêm ở
-- V38 cũ xuống lại thành index thường, chỉ để tăng tốc truy vấn theo rubric_version_id.
drop index if exists idx_assessment_policies_rubric_version;

create index idx_assessment_policies_rubric_version on assessment_policies (rubric_version_id);


-- === V42: ép trần Khối 10/11 về đúng mặc định ===
-- GradeLevelBandScopeInitializer là ApplicationRunner idempotent-theo-dòng (chỉ INSERT dòng còn
-- thiếu, không UPDATE dòng đã có), nên dữ liệu seed cũ không tự sửa lại theo code initializer mới.
UPDATE grade_level_band_scopes gs
SET hard_max_band_id = gs.default_target_band_id,
    updated_at = now()
FROM grade_levels gl
WHERE gs.grade_level_id = gl.id
  AND gl.code IN ('GRADE_10', 'GRADE_11');


-- === V44: trần chung của Khối 10/11/12 nới lên Bậc 5 ===
-- Không còn phân biệt theo khối như đoạn V42 ở trên -- xem GradeLevelBandScopeInitializer đã đổi
-- BAND_CODE_HARD_MAX.
UPDATE grade_level_band_scopes gs
SET hard_max_band_id = band5.id,
    updated_at = now()
FROM grade_levels gl,
     framework_result_bands band5
WHERE gs.grade_level_id = gl.id
  AND gl.code IN ('GRADE_10', 'GRADE_11', 'GRADE_12')
  AND band5.framework_version_id = gs.framework_version_id
  AND band5.code = 'BAC_5';


-- === V45: trần CENTRALIZE của Khối 10/11/12 nới lên Bậc 4 ===
-- Kiểm tra tập trung, không neo vào một Lớp cụ thể. Trần CLASS_TEST (hard_max_band_id) đã đúng
-- Bậc 5 từ đoạn trên, không cần đụng.
UPDATE grade_level_band_scopes gs
SET default_target_band_id = band4.id,
    updated_at = now()
FROM grade_levels gl,
     framework_result_bands band4
WHERE gs.grade_level_id = gl.id
  AND gl.code IN ('GRADE_10', 'GRADE_11', 'GRADE_12')
  AND band4.framework_version_id = gs.framework_version_id
  AND band4.code = 'BAC_4';


-- === V46: ImportType thêm QUESTION_BANK và QUESTION_TOPIC ===
-- Check constraint chưa được nới theo enum, nên mọi phiên import hai loại đó bị Postgres từ chối
-- ngay ở câu insert đầu tiên.
--
-- Danh sách phải khớp @CheckConstraint trên ImportSessionJpaEntity.type -- lệch thì Hibernate
-- validate schema sẽ báo, và lần vá tiếp theo lại phải mò cả hai chỗ.
alter table if exists import_sessions drop constraint if exists chk_import_sessions_type_valid;
alter table if exists import_sessions
    add constraint chk_import_sessions_type_valid
    check (type IN ('USER', 'SCHOOL_CLASS', 'SCHOOL_CLASS_USER', 'QUESTION', 'QUESTION_BANK',
                    'QUESTION_TOPIC', 'SCHOOL_DIRECTORY', 'SCHOOL_GRADE_LEVEL', 'SCHOOL_GRADE',
                    'SCHOOL_ROOM', 'RUBRIC_VERSION', 'RUBRIC_CRITERION', 'RUBRIC_RESULT_BAND',
                    'ASSESSMENT_POLICY'));


-- === V47: mô tả tài nguyên câu hỏi varchar(2048) -> TEXT ===
-- description là THỨ DUY NHẤT AI biết về tài nguyên (nó không nhìn được ảnh, không nghe được tệp),
-- nên mô tả một đoạn nghe dài hay một bức ảnh nhiều chi tiết dễ vượt 2048 ký tự. Phía request
-- KHÔNG có @Size chặn trước, nên vượt hạn mức là nổ thẳng ở tầng DB với thông báo khó hiểu.
--
-- Nới rộng cột nên an toàn với dữ liệu sẵn có; Postgres đổi varchar(n) -> text không viết lại bảng.
ALTER TABLE question_assets
    ALTER COLUMN description TYPE TEXT;
