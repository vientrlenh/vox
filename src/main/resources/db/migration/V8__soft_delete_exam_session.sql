-- Xoá mềm phiên thi (và kết quả của phiên đó).
--
-- Trước đây DeleteExamSessionUseCase xoá CỨNG cả cây dữ liệu: câu trả lời, lượt nói, đánh giá,
-- điểm theo tiêu chí, kết quả, đơn phúc khảo, phân công chấm, nhật ký trạng thái và bản ghi hình.
-- Không hoàn tác được, và cũng không kiểm chứng lại được khi có tranh chấp điểm.
--
-- Hai cột trạng thái đi kèm nhau:
--   * status = 'DELETED' để mọi query lọc theo trạng thái nhìn thấy ngay, không phải nhớ thêm điều kiện;
--   * deleted_at / deleted_reason để biết xoá lúc nào và VÌ SAO — xoá một bài thi là chuyện phải giải
--     trình được với học sinh và phụ huynh.
-- CHECK bên dưới buộc hai cột luôn nhất quán: không thể có DELETED mà thiếu mốc thời gian, và ngược lại.
--
-- Không có unique constraint nào trên hai bảng này ngoài khoá chính, nên dòng đã xoá mềm nằm lại
-- không chặn phiên thi mới của cùng thí sinh.

ALTER TABLE exam_sessions
    ADD COLUMN deleted_at timestamp(6) with time zone,
    ADD COLUMN deleted_reason text;

ALTER TABLE exam_candidate_results
    ADD COLUMN deleted_at timestamp(6) with time zone,
    ADD COLUMN deleted_reason text;

-- Bổ sung 'DELETED' vào danh sách trạng thái hợp lệ.
ALTER TABLE exam_sessions
    DROP CONSTRAINT chk_exam_sessions_status_valid;
ALTER TABLE exam_sessions
    ADD CONSTRAINT chk_exam_sessions_status_valid CHECK (
        (status)::text = ANY (ARRAY[
            ('IN_PROGRESS'::character varying)::text,
            ('SUBMITTED'::character varying)::text,
            ('INTERRUPTED'::character varying)::text,
            ('GRADING'::character varying)::text,
            ('GRADED'::character varying)::text,
            ('EXPIRED'::character varying)::text,
            ('GRADING_FAILED'::character varying)::text,
            ('DELETED'::character varying)::text
        ])
    );

ALTER TABLE exam_candidate_results
    DROP CONSTRAINT chk_exam_candidate_results_status_valid;
ALTER TABLE exam_candidate_results
    ADD CONSTRAINT chk_exam_candidate_results_status_valid CHECK (
        (status)::text = ANY (ARRAY[
            ('PENDING_REVIEW'::character varying)::text,
            ('RELEASED'::character varying)::text,
            ('APPEALED'::character varying)::text,
            ('RE_GRADING'::character varying)::text,
            ('FINAL'::character varying)::text,
            ('INVALID'::character varying)::text,
            ('RETAKE_REQUIRED'::character varying)::text,
            ('PASSED'::character varying)::text,
            ('FAILED'::character varying)::text,
            ('DELETED'::character varying)::text
        ])
    );

-- status và deleted_at phải cùng nói một chuyện: có đúng một nguồn sự thật về "đã xoá hay chưa".
ALTER TABLE exam_sessions
    ADD CONSTRAINT chk_exam_sessions_deleted_consistent
    CHECK (((status)::text = 'DELETED') = (deleted_at IS NOT NULL));

ALTER TABLE exam_candidate_results
    ADD CONSTRAINT chk_exam_candidate_results_deleted_consistent
    CHECK (((status)::text = 'DELETED') = (deleted_at IS NOT NULL));

-- Mọi truy vấn đọc từ nay đều kèm `deleted_at IS NULL` (@SQLRestriction ở tầng entity), nên đánh
-- index một phần theo đúng hai lối tra nóng nhất: lượt thi của một thí sinh, và kết quả của một phiên.
CREATE INDEX idx_exam_sessions_candidate_not_deleted
    ON exam_sessions (candidate_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_exam_candidate_results_session_not_deleted
    ON exam_candidate_results (session_id)
    WHERE deleted_at IS NULL;
