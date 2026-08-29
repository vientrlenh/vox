-- =============================================================================
-- V4: school_debt_events kể được cả nợ sinh từ phiên LUYỆN NÓI, và kể được lúc trường HẾT nợ.
--
-- Ba lỗ hổng cùng một hình dạng, sửa chung một lượt vì sửa lẻ cái nào cũng để lại một cái bẫy:
--
--  1. Nợ sinh từ phiên luyện nói mất dấu vết. ConsumeQuotaService.chargeOverage phân biệt đúng
--     hai nguồn khi ghi school_balance_entries, rồi lại truyền mỗi examSessionId xuống
--     checkDebtCapTransition -- practiceSessionId nằm ngay trong tầm với mà không được dùng. Hệ
--     quả: dòng CAP_EXCEEDED do luyện nói gây ra ghi trigger_exam_session_id = NULL, nói được
--     "có một khoản đẩy trường vượt trần" mà không nói được khoản nào.
--
--  2. Nợ sinh từ phiên luyện nói KHÔNG ghi dòng LOCKED nào. crossedIntoDebt được trả về ở cả hai
--     đường, nhưng chỉ CompleteExamSessionGradingUseCase tiêu thụ nó. Trường bị khoá thật (guard
--     suy từ dấu số dư, không quan tâm nguồn) nhưng sổ audit im lặng hoàn toàn.
--
--  3. Không có dòng CLEARED nào, bao giờ. publishSchoolDebtCleared đã viết xong, mẫu email đã có,
--     mà không chỗ nào trong mã nguồn gọi tới. Với hiệu trưởng, sổ này chỉ toàn tin xấu rồi im
--     lặng -- không bao giờ nói trường đã thoát ra.
--
-- CLEARED là sự kiện cấp TRƯỜNG, không thuộc ví hạn mức nào: số dư là MỘT con số dùng chung, nên
-- "ví nào vừa hết nợ" không có câu trả lời. Vì thế quota_type / total_allocated_vnd /
-- used_amount_vnd bỏ NOT NULL và phải để trống ở đúng loại sự kiện đó. Bịa ra một quotaType để
-- lấp chỗ trống là làm bẩn chính quyển sổ dùng để đối soát.
--
-- CẢNH BÁO: script XOÁ những dòng không thể thoả ràng buộc mới (CAP_EXCEEDED do luyện nói, vốn đã
-- ghi thiếu trigger). Chỉ chạy được vì hệ thống chưa có dữ liệu thật.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Cột nguồn thứ hai, và FK thật cho CẢ HAI.
--    trigger_exam_session_id từ V1 tới giờ vẫn là uuid trần không FK -- khác hẳn
--    school_balance_entries, nơi mỗi cột session đều là một FK được thật (V2 mục 14). Không có
--    ràng buộc, một id rác vẫn ghi xuống được và chỉ lộ ra lúc ai đó bấm vào để tra ca thi.
-- -----------------------------------------------------------------------------
ALTER TABLE school_debt_events ADD COLUMN trigger_practice_session_id uuid;

ALTER TABLE school_debt_events
    ADD CONSTRAINT fk_school_debt_events_trigger_exam_session
    FOREIGN KEY (trigger_exam_session_id) REFERENCES exam_sessions(id);

ALTER TABLE school_debt_events
    ADD CONSTRAINT fk_school_debt_events_trigger_practice_session
    FOREIGN KEY (trigger_practice_session_id) REFERENCES practice_sessions(id);

CREATE INDEX idx_school_debt_events_trigger_practice_session
    ON school_debt_events USING btree (trigger_practice_session_id)
    WHERE trigger_practice_session_id IS NOT NULL;


-- -----------------------------------------------------------------------------
-- 2. Ba cột chỉ có nghĩa với sự kiện gắn vào một ví hạn mức.
-- -----------------------------------------------------------------------------
ALTER TABLE school_debt_events ALTER COLUMN quota_type DROP NOT NULL;
ALTER TABLE school_debt_events ALTER COLUMN total_allocated_vnd DROP NOT NULL;
ALTER TABLE school_debt_events ALTER COLUMN used_amount_vnd DROP NOT NULL;


-- -----------------------------------------------------------------------------
-- 3. Dọn những dòng không thể thoả ràng buộc mới.
--    Đây chính là các dòng lỗi số 1 đã sinh ra: CAP_EXCEEDED của ví PRACTICE, không có trigger nào.
--    Không vá được -- id phiên đã mất từ lúc ghi, không có đường nào tra ngược lại.
-- -----------------------------------------------------------------------------
DELETE FROM school_debt_events
WHERE (event_type)::text <> 'CLEARED'
  AND trigger_exam_session_id IS NULL
  AND trigger_practice_session_id IS NULL;


-- -----------------------------------------------------------------------------
-- 4. Hình dạng của một dòng do event_type quyết định.
--    Ràng buộc này là chốt chặn để mục 1 và mục 2 không trôi khỏi nhau: sửa cột mà quên sửa chỗ
--    phát sự kiện thì INSERT hỏng ngay, thay vì âm thầm ghi thêm một dòng mất dấu vết nữa.
-- -----------------------------------------------------------------------------
ALTER TABLE school_debt_events
    ADD CONSTRAINT chk_school_debt_events_shape_matches_event_type CHECK (
        (
            (event_type)::text <> 'CLEARED'
            AND quota_type IS NOT NULL
            AND total_allocated_vnd IS NOT NULL
            AND used_amount_vnd IS NOT NULL
            -- ĐÚNG MỘT nguồn, cùng khuôn với chk_school_balance_entries_overage_traceable.
            AND num_nonnulls(trigger_exam_session_id, trigger_practice_session_id) = 1
        )
        OR
        (
            (event_type)::text = 'CLEARED'
            AND quota_type IS NULL
            AND total_allocated_vnd IS NULL
            AND used_amount_vnd IS NULL
            AND trigger_exam_session_id IS NULL
            AND trigger_practice_session_id IS NULL
            -- Hết nợ nghĩa là số dư đã về không âm, tức phần vượt bằng 0 theo đúng định nghĩa.
            AND overage_vnd = 0
        )
    );
