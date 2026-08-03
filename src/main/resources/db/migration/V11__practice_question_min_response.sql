-- Đổi ngân sách thời lượng của câu luyện sang đúng hình dạng của đề thi: một SÀN và một TRẦN.
--
-- Vì sao bỏ max_followup_seconds:
--   Cột này là cột chết. Nó do LLM tự điền mà prompt drafter chưa bao giờ nói cho model biết nó
--   là gì (grep DrafterNode không có một dòng nào nhắc tới các trường thời lượng), nên toàn bộ
--   kho câu hỏi đều có giá trị 0. Không nơi nào trong cả ba repo đọc nó để ra quyết định --
--   đồ thị quyết định follow-up bên Python dừng bằng số lượt (MAX_TURNS), không hề đọc cột này.
--   Chỗ duy nhất dùng tới giá trị là spokenSeconds() = maxResponse + maxFollowup, tức luôn luôn
--   bằng maxResponse.
--
-- Vì sao thêm min_response_seconds:
--   Follow-up VỐN ĐÃ được cộng dồn giây rồi -- SignalNode tính actual_response_seconds trên
--   [*previous_turns, current_turn], tức cả câu chính lẫn mọi lượt hỏi thêm. Nên không cần một
--   ngân sách riêng cho follow-up; chỉ cần biết nói bao nhiêu là ĐỦ. Trước đây thiếu sàn nên
--   _resolve_target_response_seconds rơi xuống lấy TRẦN làm mốc, khiến một câu trả lời trọn vẹn
--   18 giây trên trần 45 giây bị đọc thành "mới đạt 0.4" và ngưỡng pressure=high (ratio >= 1.15)
--   đòi nói 52 giây khi trần là 45 -- không bao giờ với tới.
--
-- Giá trị điền bù: 45% của trần, làm tròn. Không phải con số bịa -- lấy theo tiền lệ có thật
-- trong bảng questions của đề thi, nơi tỉ lệ min/max trải từ 0.33 đến 0.50, và câu có
-- max_response_seconds = 45 (đúng bằng câu luyện hiện tại) có min_response_seconds = 20.

-- Vì sao bỏ preparation_time_seconds (chỉ ở BẢNG LUYỆN, đề thi vẫn giữ):
--   Phiên luyện chạy theo lối bấm-để-đi-tiếp: học sinh đọc thẻ sửa lỗi xong, thấy sẵn sàng thì
--   bấm "Tiếp tục", lúc đó câu hỏi mới được đọc lên. Không có đồng hồ đếm ngược chuẩn bị nào
--   cả -- Flutter chỉ liệt kê cột này trong câu GraphQL rồi không dùng tới, Python thì không
--   đưa nó vào QuestionContext. Thời gian chuẩn bị do học sinh tự quyết, nên một con số cố
--   định do LLM đoán bừa không có nghĩa gì, mà lại đang được cộng vào ngân sách đề.

-- Gỡ ràng buộc TRƯỚC khi bỏ cột: Postgres tự xoá theo mọi ràng buộc có nhắc tới cột bị bỏ,
-- nên nếu để DROP CONSTRAINT xuống sau thì tới lượt nó đã không còn gì để xoá và migration
-- chết với "constraint ... does not exist".
ALTER TABLE practice_question DROP CONSTRAINT chk_practice_question_time_budgets;

ALTER TABLE practice_question ADD COLUMN min_response_seconds INTEGER;

UPDATE practice_question
   SET min_response_seconds = GREATEST(1, ROUND(max_response_seconds * 0.45));

ALTER TABLE practice_question ALTER COLUMN min_response_seconds SET NOT NULL;

ALTER TABLE practice_question DROP COLUMN max_followup_seconds;

ALTER TABLE practice_question DROP COLUMN preparation_time_seconds;

ALTER TABLE practice_question ADD CONSTRAINT chk_practice_question_time_budgets
    CHECK (
        min_response_seconds > 0
        AND max_response_seconds > min_response_seconds
    );
