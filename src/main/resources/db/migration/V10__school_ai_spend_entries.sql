-- =============================================================================
-- V10: sổ chi phí AI của TRƯỜNG -- ai tiêu, tiêu lúc nào, tiêu bao nhiêu.
--
-- VÌ SAO CẦN MỘT BẢNG NỮA, TRONG KHI ĐÃ CÓ HAI SỔ TIỀN:
--
--   * ai_usage_records  = giá vốn của NỀN TẢNG, một dòng cho mỗi lượt gọi nhà cung cấp. Nó KHÔNG
--     phải thứ trường bị thu: V9 vừa tách hai con số đó ra khi cho phép miễn (waived_at) chi phí của
--     một lượt chấm hỏng. Vẽ biểu đồ "chi phí AI của trường" từ bảng này là tính cho trường cả những
--     đồng mà chính ta đã quyết định không thu.
--     Bảng đó cũng chỉ có exam_session_id NOT NULL, tức không biết gì về đường luyện nói.
--
--   * school_balance_entries = sổ cái của VÍ, và cố ý chỉ ghi phần tiêu VƯỢT hạn mức (xem V2 mục 7:
--     ghi cả phần trong hạn mức là đếm hai lần cùng một đồng và phá bất biến
--     SUM(entries.amount_vnd) = balance_vnd). Cộng sổ đó ra tổng chi thì thiếu đúng phần lớn nhất.
--
-- Bảng này trả lời câu thứ ba mà hai sổ trên không trả lời được: TRƯỜNG đã tiêu bao nhiêu, theo
-- ngày, và của ai. Một dòng cho mỗi lần trừ hạn mức, ghi ĐỦ số tiền chứ không chỉ phần vượt.
--
-- CHỖ GHI DUY NHẤT là ConsumeQuotaService.consumeAllowingDebt -- cái phễu mà cả đường thi lẫn đường
-- luyện nói đều đi qua, và cũng là chỗ duy nhất biết đồng thời (trường, loại ví, người tiêu, số
-- tiền, phiên). Thêm một nguồn tiêu tiền mới trong tương lai mà quên bảng này thì nó sẽ vắng mặt ở
-- mọi báo cáo, y như đường luyện nói đang vắng mặt hôm nay.
--
-- KHÔNG BACKFILL. Với đường thi thì về lý có thể dựng lại từ ai_usage_records, nhưng số dựng lại sẽ
-- KHÁC số đã thu thật (phần miễn của V9, và ranh giới chia giữa hạn mức với ví), nên một sổ "gần
-- đúng" cho quá khứ còn tệ hơn là không có: nó trông như số liệu thật. Biểu đồ vì thế bắt đầu từ
-- ngày triển khai, và giao diện phải nói rõ điều đó.
--
-- VÌ SAO user_id CHO PHÉP NULL: kỳ thi tập trung do nhà trường tổ chức, không thuộc túi riêng của ai
-- -- CompleteExamSessionGradingUseCase cố ý truyền null ở đó và chỉ truyền người ra đề với bài kiểm
-- tra trên lớp. NULL ở đây là một câu trả lời ("của cả trường"), không phải dữ liệu thiếu.
-- =============================================================================

CREATE TABLE school_ai_spend_entries (
    id                  uuid                        NOT NULL DEFAULT uuidv7(),
    school_id           uuid                        NOT NULL,
    subscription_id     uuid                        NOT NULL,
    quota_type          varchar(20)                 NOT NULL,
    -- NULL = khoản chi của cả trường, không thuộc trần chi cá nhân nào.
    user_id             uuid,
    exam_session_id     uuid,
    practice_session_id uuid,
    -- ĐỦ số tiền của lần trừ, gồm cả phần nằm trong hạn mức. Khác school_balance_entries.
    amount_vnd          numeric(18, 6)              NOT NULL,
    occurred_at         timestamp(6) with time zone NOT NULL,

    CONSTRAINT pk_school_ai_spend_entries PRIMARY KEY (id),

    CONSTRAINT chk_school_ai_spend_entries_quota_type_valid CHECK (
        quota_type IN ('EXAM', 'PRACTICE')),

    -- Không ghi dòng 0 đồng: một lần trừ 0 đồng không tồn tại, và để lọt thì mọi phép đếm "số lần
    -- tiêu" đều sai. Cùng tinh thần với chk_school_balance_entries_overage_traceable.
    CONSTRAINT chk_school_ai_spend_entries_amount_positive CHECK (amount_vnd > 0),

    -- ĐÚNG MỘT nguồn, và nguồn phải khớp loại ví. Thiếu ràng buộc này thì một dòng "EXAM kèm
    -- practice_session_id" là hợp lệ về kiểu dữ liệu -- một dòng tự mâu thuẫn mà không tầng Java nào
    -- chặn được, đúng cái bẫy đã ghi trong javadoc của ConsumeQuotaService.consumeExamAllowingDebt.
    CONSTRAINT chk_school_ai_spend_entries_source_matches_quota_type CHECK (
        (quota_type = 'EXAM'
            AND exam_session_id IS NOT NULL AND practice_session_id IS NULL)
        OR (quota_type = 'PRACTICE'
            AND practice_session_id IS NOT NULL AND exam_session_id IS NULL))
);

-- Biểu đồ theo ngày của một trường: luôn lọc school_id rồi cắt theo occurred_at.
CREATE INDEX idx_school_ai_spend_entries_school_occurred
    ON school_ai_spend_entries USING btree (school_id, occurred_at);

-- Bảng "ai đang tiêu": chỉ gom những dòng CÓ người, nên index một phần vừa nhỏ hơn vừa bỏ qua đúng
-- phần lớn dòng của kỳ thi tập trung.
CREATE INDEX idx_school_ai_spend_entries_school_user_occurred
    ON school_ai_spend_entries USING btree (school_id, user_id, occurred_at)
    WHERE user_id IS NOT NULL;

COMMENT ON TABLE school_ai_spend_entries IS
    'Chi phí AI mà TRƯỜNG bị trừ, một dòng mỗi lần trừ hạn mức. Khác ai_usage_records (giá vốn nền '
    'tảng, có khoản được miễn) và khác school_balance_entries (chỉ phần tiêu vượt hạn mức).';

COMMENT ON COLUMN school_ai_spend_entries.user_id IS
    'NULL = khoản chi của cả trường (kỳ thi tập trung). Có giá trị = tính vào trần chi cá nhân của '
    'người đó: giáo viên ra đề kiểm tra trên lớp, hoặc học sinh luyện nói.';
