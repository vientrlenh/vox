-- Bỏ 4 cột chưa từng chở dữ liệu thật trong learner_profile.
--
-- Vì sao là migration MỚI chứ không sửa thẳng V13: V13 đã chạy rồi (flyway_schema_history ghi
-- success=t), nên sửa nội dung nó chỉ làm lệch checksum -> Flyway chặn khởi động, mà cột thì vẫn
-- nằm nguyên trong DB. Muốn cột biến mất thật thì phải DROP bằng một bản mới.
--
--   target_exam, target_date
--     Chưa từng được GHI. appendProfile() không có tham số cho chúng, không setter nào được gọi,
--     và next() chỉ chép giá trị cũ sang bản mới -- nên chúng vĩnh viễn NULL kể từ dòng đầu tiên.
--     Không xuất hiện ở GraphQL lẫn hai client.
--
--   flsa_score, flsa_raw_answers_json
--     Thang tự đánh giá lo lắng ngoại ngữ (FLSA). Chúng CÓ được ghi, nhưng chưa từng được ĐỌC để
--     đổi hành vi: không luồng sinh câu hỏi, chọn độ khó hay xếp hạng chủ đề nào tra tới. Chỉ ghi
--     rồi dội ngược lại qua GraphQL. Client đã bỏ màn hỏi từ trước, nên mutation
--     submitFlsaSelfReport không còn nguồn gọi -- đã gỡ cùng lượt này.
--
-- Kiểm trước khi xoá trên DB hiện tại: cả 4 cột đều 0 dòng khác NULL.
--
-- IF EXISTS để chạy lại được trên DB đã dọn tay.

ALTER TABLE learner_profile
    DROP COLUMN IF EXISTS target_exam,
    DROP COLUMN IF EXISTS target_date,
    DROP COLUMN IF EXISTS flsa_score,
    DROP COLUMN IF EXISTS flsa_raw_answers_json;
