-- Chặn bão yêu cầu chấm trùng lặp.
--
-- Job quét mồ côi (PracticeSessionHeartbeatCleanupJob, 5 phút/lần) bắn lại yêu cầu chấm cho
-- mọi câu CHƯA có bản chấm. Nhưng "chưa có bản chấm" không phân biệt được hai chuyện khác hẳn
-- nhau: yêu cầu bị thất lạc thật, và yêu cầu ĐANG được chấm dở.
--
-- Chấm một câu mất ~3,5 phút (LLM từng lượt + Azure phát âm + đánh giá tổng hợp), tức LÂU HƠN
-- nhịp quét. Nên mọi câu đều bị bắn lại ít nhất một lần trước khi kịp chấm xong, và bản sao đó
-- lại chiếm thêm 3,5 phút của consumer, đẩy các câu sau chậm thêm -> lại bị bắn lại. Vòng lặp
-- tự khuếch đại.
--
-- Đo trên production 2026-08-12: câu 15655874-ace3-4cd8-8530-f5c5f272c071 bị nhận và chấm
-- LẠI 8 LẦN; hàng đợi practice-attempt-evaluation-requested tồn 15 message và vẫn đang lớn dần
-- trong khi consumer offset đứng yên.
--
-- Cột này ghi mốc ĐÃ GỬI yêu cầu, để job quét phân biệt được "đang chấm" với "mất tích".
ALTER TABLE practice_item_response
    ADD COLUMN grading_requested_at TIMESTAMPTZ;

-- Backfill cho dữ liệu cũ: câu đã có bản chấm coi như đã từng gửi (mốc lấy theo lúc chấm xong).
-- Không backfill câu chưa chấm -- để NULL thì job quét bắn lại ngay lượt tới, đúng ý: đó mới
-- thật sự là những câu đang mồ côi.
UPDATE practice_item_response response
SET grading_requested_at = evaluation.evaluated_at
FROM practice_item_evaluation evaluation
WHERE evaluation.practice_response_id = response.id
  AND response.grading_requested_at IS NULL;

ALTER TABLE practice_item_response
    ADD COLUMN grading_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN grading_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE practice_item_response
    ADD CONSTRAINT chk_practice_item_response_grading_status
    CHECK (grading_status IN ('PENDING', 'GRADING', 'GRADED', 'GRADING_FAILED'));

-- Backfill: đã có bản chấm -> GRADED. Chưa có -> để PENDING, tức bắn lại ngay lượt quét tới --
-- đúng diện mồ côi thật.
UPDATE practice_item_response response
SET grading_status = 'GRADED'
WHERE EXISTS (
    SELECT 1 FROM practice_item_evaluation evaluation
    WHERE evaluation.practice_response_id = response.id
);

-- Phục vụ đúng điều kiện của findResponsesAwaitingFlush.
CREATE INDEX IF NOT EXISTS idx_practice_item_response_grading_requested
    ON practice_item_response (practice_session_id, grading_status, grading_requested_at);
