-- =============================================================================
-- V11: đánh dấu bài thi đã được nhắc "còn bài chờ soát điểm AI" hay chưa.
--
-- VÌ SAO CẦN MỘT CỘT: thông báo này phải đi khi bài thi CÒN ĐANG DIỄN RA, không phải lúc đóng
-- bài. Ca thi kết thúc sớm đã có kết quả ngay, và bắt người chấm ngồi chờ tới lúc đóng bài là
-- vứt đi phần lớn thời gian chấm được.
--
-- Nhưng "còn đang diễn ra" nghĩa là không còn mốc một-lần nào để bám vào: trước đây mốc đó là
-- chuyển trạng thái IN_PROGRESS -> CLOSED, mà requireTransition chỉ cho qua đúng một lần nên
-- không cần chống trùng gì thêm. Quét định kỳ thì ngược lại -- mỗi phút một lượt, và số bài chờ
-- chấm chỉ tăng dần trong lúc thi. Không có cột này thì mỗi bài thi bị nhắc lại mỗi phút cho tới
-- khi đóng.
--
-- Cùng cơ chế với exam_grading_assignments.reminded_at (V1 baseline): cột thời điểm + truy vấn
-- native FOR UPDATE SKIP LOCKED. reminded_at một mình chỉ chống trùng qua các LƯỢT chạy, không
-- chống trùng giữa các INSTANCE -- xem SpringDataExamRepository.findDueForHumanGradingNotice.
--
-- KHÔNG backfill: đặt NULL cho mọi bài thi cũ nghĩa là bài nào còn dở dang và đang có bài chờ
-- chấm sẽ được nhắc một lần ở lượt quét đầu tiên sau khi deploy. Đó đúng là điều mong muốn --
-- chúng chưa từng được nhắc lần nào. Bài đã đóng xong thì không lọt vào truy vấn.
-- =============================================================================

ALTER TABLE exams
    ADD COLUMN human_grading_notified_at timestamp(6) with time zone;

COMMENT ON COLUMN exams.human_grading_notified_at IS
    'Thời điểm đã phát ExamHumanGradingRequired cho bài thi này. NULL = chưa nhắc lần nào. Chốt chống trùng của lượt quét định kỳ.';

-- Chỉ phục vụ đúng lượt quét: lọc bài chưa nhắc trong hai trạng thái còn chấm được. Partial index
-- vì phần lớn dòng sẽ có giá trị NOT NULL sau khi được nhắc, và chúng không bao giờ cần quét lại.
CREATE INDEX idx_exams_pending_human_grading_notice
    ON exams (status)
    WHERE human_grading_notified_at IS NULL;
