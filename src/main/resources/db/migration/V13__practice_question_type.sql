-- Câu luyện có DẠNG BÀI, như câu thi.
--
-- Trước đây câu luyện chỉ được mô tả bằng bốn chiều độ khó (here_and_now, num_elements,
-- reasoning_type, abstractness) -- đủ để xếp BẬC nhưng không nói gì về dạng bài. Hệ quả đo
-- được: payload gửi sang Python có questionType = null, nên get_expected_min_words rơi hết
-- xuống nhánh mặc định và kỳ vọng ĐÚNG 10 TỪ cho mọi câu, kể cả câu 45 giây. Đề thi ở cùng
-- thời lượng kỳ vọng 35-40 từ. Tín hiệu length_sufficient vì thế gần như luôn đúng, tức vô
-- nghĩa.
--
-- Bốn dạng, KHÔNG có READ_ALOUD: dạng đó cần văn bản mẫu để đọc theo (questions.prompt_text),
-- mà luyện nói tự do thì không có văn bản nào cả. Đưa vào chỉ tạo ra câu hỏi không dùng được.
--
-- Điền bù SHORT_ANSWER cho kho hiện tại: mọi câu đang có max_response_seconds 45-60s, đúng
-- dải SHORT_ANSWER của đề thi (45-60s). Không đoán bừa -- xem chính bảng questions.

ALTER TABLE practice_question ADD COLUMN question_type VARCHAR(24);

UPDATE practice_question SET question_type = 'SHORT_ANSWER' WHERE question_type IS NULL;

ALTER TABLE practice_question ALTER COLUMN question_type SET NOT NULL;

ALTER TABLE practice_question ADD CONSTRAINT chk_practice_question_type
    CHECK (question_type IN ('SHORT_ANSWER', 'LONG_ANSWER', 'DESCRIPTION', 'OPINION'));

CREATE INDEX idx_practice_question_type
    ON practice_question (practice_topic_id, question_type, difficulty_rank);
