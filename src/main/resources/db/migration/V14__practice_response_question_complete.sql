-- "Câu này đã trả lời xong chưa" là một sự thật mà hệ thống BIẾT ngay lúc nộp lượt cuối
-- (SubmitPracticeTurnUseCase đọc turn.isQuestionComplete()) nhưng chưa từng lưu lại ở đâu.
--
-- Không lưu thì hỏng hai chỗ:
--   1. Không phân biệt được "câu đang chờ chấm" với "câu sẽ không bao giờ được chấm" -- màn
--      tổng kết không biết nên đợi hay nên thôi.
--   2. Điểm phiên không loại được câu dở dang, nên học sinh rớt mạng giữa câu bị chấm theo
--      rubric của một câu trả lời đầy đủ.
ALTER TABLE practice_item_response
    ADD COLUMN question_complete boolean NOT NULL DEFAULT false;

-- Backfill theo LỊCH SỬ chứ không để false hàng loạt: trước đây bài chấm CHỈ được kích hoạt
-- khi questionComplete=true (SubmitPracticeTurnUseCase.evaluationQueued), nên "đã có bản chấm"
-- chính là bằng chứng câu đó từng hoàn thành. Để false hết sẽ khiến mọi câu cũ đột nhiên bị
-- loại khỏi điểm phiên khi truy vấn mới có hiệu lực.
UPDATE practice_item_response response
   SET question_complete = true
 WHERE EXISTS (
     SELECT 1
     FROM practice_item_evaluation evaluation
     WHERE evaluation.practice_response_id = response.id
 );

-- Truy vấn nóng là "phiên này còn câu nào chưa chấm" (màn tổng kết poll mỗi vài giây) và
-- "xả chấm các câu dở lúc đóng phiên" -- cả hai đều lọc theo phiên rồi mới xét cờ này.
CREATE INDEX IF NOT EXISTS idx_practice_response_session_complete
    ON practice_item_response (practice_session_id, question_complete);
