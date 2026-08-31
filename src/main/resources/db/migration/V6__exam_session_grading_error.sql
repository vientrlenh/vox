-- =============================================================================
-- V6: lưu LÝ DO phiên thi chấm lỗi, và một hàm sinh chữ ký nhóm lỗi.
--
-- VÌ SAO CẦN: sự kiện Kafka ExamAttemptEvaluationFailed đã mang sẵn payload.error và
-- payload.retryCount, nhưng UpdateExamSessionStatusCommand chỉ nhận (sessionId, status) nên lý do
-- rơi mất ngay tại consumer. Hệ quả: exam_sessions.status = 'GRADING_FAILED' là TẤT CẢ những gì hệ
-- thống còn biết về một phiên hỏng -- không trả lời được câu hỏi đầu tiên của người trực, "một sự cố
-- dịch vụ hay 1.284 sự cố riêng lẻ?".
--
-- NULL CÓ NGHĨA RIÊNG, KHÔNG PHẢI "CHƯA ĐIỀN": nhánh @DltHandler đánh dấu GRADING_FAILED khi bản tin
-- đã hết đường retry, và ở đó KHÔNG có lý do nào để lưu -- bản tin có thể còn không parse được.
-- Những phiên đó là nhóm "không rõ nguyên nhân" thật sự, khác hẳn với phiên có lý do. Mọi phiên
-- GRADING_FAILED tồn tại TRƯỚC V6 cũng rơi vào nhóm này, và đó là mô tả đúng: hệ thống thật sự
-- không biết vì sao chúng hỏng. Không backfill, vì không có nguồn nào để backfill TỪ.
-- =============================================================================

ALTER TABLE exam_sessions
    -- text chứ không varchar(n): đây là thông điệp lỗi của một dịch vụ bên ngoài, không có trần độ
    -- dài nào mà mình kiểm soát được. Cắt bớt ở tầng DB sẽ cắt mất đúng phần đuôi thường mang chi
    -- tiết hữu ích nhất.
    ADD COLUMN grading_error text,
    -- Dịch vụ chấm đã thử bao nhiêu lần trước khi bỏ cuộc. Tách khỏi thông điệp vì nó là con số để
    -- SO SÁNH (sắp xếp, lọc "thử >= 3 lần"), không phải chữ để đọc.
    ADD COLUMN grading_retry_count integer;

-- Chỉ phiên đang lỗi mới được mang lý do. Không có ràng buộc này thì một phiên chấm lại thành công
-- vẫn giữ thông điệp lỗi cũ, và trang phân loại sẽ đếm nó vào nhóm sự cố mãi mãi.
ALTER TABLE exam_sessions
    ADD CONSTRAINT chk_exam_sessions_grading_error_only_when_failed CHECK (
        status = 'GRADING_FAILED' OR (grading_error IS NULL AND grading_retry_count IS NULL));

-- =============================================================================
-- Chữ ký nhóm lỗi.
--
-- Gom theo thông điệp THÔ là vô dụng: mỗi thông điệp thường nhúng session id, mốc thời gian, số
-- giây timeout -- nên một sự cố duy nhất sẽ nở ra thành hàng nghìn nhóm một-phần-tử, đúng cái danh
-- sách phẳng mà việc gom nhóm sinh ra để tránh.
--
-- Đặt thành HÀM chứ không viết thẳng biểu thức vào câu query, vì có HAI chỗ dùng: câu đếm theo nhóm
-- và câu lọc phiên theo một nhóm. Hai bản sao của cùng một biểu thức chỉ cần lệch một ký tự là thẻ
-- nhóm hiện 1.147 còn bấm vào lại ra danh sách rỗng.
--
-- IMMUTABLE để về sau đánh index biểu thức được nếu bảng lớn lên -- lower() và regexp_replace() đều
-- không phụ thuộc trạng thái phiên làm việc.
-- =============================================================================
CREATE OR REPLACE FUNCTION vox_grading_error_signature(raw text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT CASE
        WHEN raw IS NULL OR btrim(raw) = '' THEN NULL
        ELSE left(
            btrim(
                -- Gộp mọi khoảng trắng (kể cả xuống dòng của stack trace) về một dấu cách, để hai
                -- lần in cùng một lỗi với cách xuống dòng khác nhau vẫn vào chung nhóm.
                regexp_replace(
                    -- Số: mốc thời gian, số giây, mã cổng. Chạy SAU uuid vì uuid cũng chứa chữ số.
                    regexp_replace(
                        -- UUID: session id, answer id, request id -- phần biến thiên nhiều nhất.
                        regexp_replace(
                            lower(raw),
                            '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}',
                            '<id>', 'g'),
                        '\d+', '<n>', 'g'),
                    '\s+', ' ', 'g')
            ),
            -- Đủ dài để hai lỗi khác nhau không đụng nhau, đủ ngắn để phần đuôi biến thiên của một
            -- stack trace không tách nhóm.
            200)
    END;
$$;

-- Trang phân loại luôn hỏi cùng một hình dạng: các phiên GRADING_FAILED, lọc theo submitted_at.
-- Index một phần vì phiên lỗi là thiểu số rất nhỏ so với toàn bảng, và tỷ lệ đó chỉ càng nhỏ đi.
CREATE INDEX idx_exam_sessions_grading_failed_submitted_at
    ON exam_sessions USING btree (submitted_at)
    WHERE status = 'GRADING_FAILED';
