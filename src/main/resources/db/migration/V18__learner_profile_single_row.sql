-- Hồ sơ học sinh chuyển từ append-only (mỗi thay đổi một dòng mới, phân biệt bằng cột `version`)
-- sang MỘT dòng cho MỘT học sinh, cập nhật tại chỗ.
--
-- Lịch sử version ĐƯỢC GHI MÀ KHÔNG AI ĐỌC: quét toàn bộ src/main chỉ thấy các truy vấn dạng
-- `Top...OrderByVersionDesc`, tức luôn chỉ lấy bản mới nhất. Câu duy nhất mà lịch sử trả lời được
-- -- "lúc làm phiên này mục tiêu là gì" -- thì practice_paper đã chụp sẵn goal_type và
-- target_framework_band_id vào chính nó. Cái giá phải trả cho version là mỗi bản mới phải CHÉP 6
-- dòng dimension_interest_score sang id mới; đổi mục tiêu ba lần là 18 dòng cho 6 chiều.
--
-- Vì sao là migration MỚI chứ không sửa thẳng V15__personalize.sql (bản tạo bảng): file đó đã chạy
-- rồi (flyway_schema_history ghi success=t), nên sửa nội dung nó chỉ làm lệch checksum -> Flyway
-- chặn khởi động, mà cột thì vẫn nằm nguyên trong DB. Muốn cột biến mất thật thì phải DROP bằng
-- một bản mới.

-- Dọn phòng thủ: gộp về bản version cao nhất của mỗi học sinh trước khi ép unique trên student_id.
-- dimension_interest_score KHÔNG có ràng buộc khoá ngoại tới learner_profile nên phải tự xoá con
-- trước, nếu không sẽ còn lại dòng điểm mồ côi.
DELETE FROM dimension_interest_score
WHERE learner_profile_id IN (
    SELECT lp.id
    FROM learner_profile lp
    WHERE lp.version < (
        SELECT MAX(newer.version)
        FROM learner_profile newer
        WHERE newer.student_id = lp.student_id
    )
);

DELETE FROM learner_profile lp
WHERE lp.version < (
    SELECT MAX(newer.version)
    FROM learner_profile newer
    WHERE newer.student_id = lp.student_id
);

DROP INDEX IF EXISTS idx_learner_profile_student_version;

-- IF EXISTS để chạy lại được trên DB đã dọn tay.
ALTER TABLE learner_profile
    DROP COLUMN IF EXISTS version;

-- Đây là thứ THẬT SỰ ép bất biến 1-1. Thiếu nó thì code vẫn có thể sinh hai dòng cho một học sinh
-- mà không ai biết; và nó cũng là chốt phát hiện đua ghi khi hai request cùng tạo hồ sơ.
CREATE UNIQUE INDEX IF NOT EXISTS idx_learner_profile_student
    ON learner_profile (student_id);
