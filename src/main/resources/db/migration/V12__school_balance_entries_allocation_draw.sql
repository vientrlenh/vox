-- =============================================================================
-- V12: bút toán ALLOCATION_DRAW -- ghi lại + trừ THẬT ví tự nạp khi SCHOOL_ADMIN cấp hạn mức cá nhân
-- (SchoolSubscriptionQuotaUserAllocation) vượt phần pool của gói.
--
-- Trước migration này, DistributeQuotaToUsersService.computeManualAmounts chỉ ĐỌC school_balances để
-- kiểm tra còn đủ ví hay không (walletHeadroomVnd, findBySchoolId -- bản chỉ đọc), rồi cho phép nới
-- trần nếu quản trị viên xác nhận (confirmWalletDraw). KHÔNG có gì thật sự bị trừ và KHÔNG có bút
-- toán nào được ghi -- "Sao kê" (school_balance_entries) hoàn toàn im lặng về hành động này, và nhiều
-- lần cấp liên tiếp cho nhiều người có thể cộng dồn vượt xa số dư ví thật vì lần sau vẫn đọc đúng số
-- dư CŨ (chưa ai trừ đi).
--
-- Bốn loại bút toán sẵn có đều không khớp: OVERAGE_CHARGE bắt buộc gắn với đúng 1 phiên thi/luyện nói
-- + cost_usd/fx_rate_used (chk_school_balance_entries_overage_traceable) -- hành động cấp hạn mức
-- không có phiên nào. ADJUSTMENT có trong enum nhưng CHƯA TỪNG có factory nào dựng (xem javadoc
-- OrderSettlementService.creditTopUp), và mô tả của nó là System Admin sửa tay -- sai vai trò, đây là
-- SCHOOL_ADMIN chủ động cấp phát, không phải sửa lỗi.
--
-- target_user_id là cột MỚI: không có cột nào sẵn mang đúng nghĩa "người được cấp/hạ hạn mức".
-- actor_id đã có nhưng mang nghĩa khác (người BẤM nút, tái dùng nguyên nghĩa "người thực hiện" của
-- ADJUSTMENT) -- một dòng ALLOCATION_DRAW cần cả hai, không cột nào thay được cột kia.
-- =============================================================================

ALTER TABLE school_balance_entries ADD COLUMN target_user_id uuid;

ALTER TABLE school_balance_entries
    ADD CONSTRAINT fk_school_balance_entries_target_user
    FOREIGN KEY (target_user_id) REFERENCES users(id);

CREATE INDEX idx_school_balance_entries_target_user
    ON school_balance_entries USING btree (school_id, target_user_id)
    WHERE target_user_id IS NOT NULL;

ALTER TABLE school_balance_entries DROP CONSTRAINT chk_school_balance_entries_entry_type_valid;
ALTER TABLE school_balance_entries
    ADD CONSTRAINT chk_school_balance_entries_entry_type_valid CHECK (
        entry_type IN ('TOP_UP', 'OVERAGE_CHARGE', 'REFUND', 'ADJUSTMENT', 'ALLOCATION_DRAW'));

-- Hình dạng bắt buộc của một dòng ALLOCATION_DRAW: phải nói rõ ví nào (quota_type), cấp cho AI
-- (target_user_id), do AI xác nhận (actor_id), và số tiền thật sự đổi (khác 0 -- dấu tự do: âm = cấp
-- hạn mức ăn vào ví, dương = hạ hạn mức, hoàn lại phần đã ăn). Cùng khuôn với
-- chk_school_balance_entries_overage_traceable: ràng buộc theo entry_type, không phải theo nullability
-- rời rạc từng cột.
ALTER TABLE school_balance_entries
    ADD CONSTRAINT chk_school_balance_entries_allocation_draw_traceable CHECK (
        (entry_type)::text <> 'ALLOCATION_DRAW' OR (
            quota_type IS NOT NULL
            AND target_user_id IS NOT NULL
            AND actor_id IS NOT NULL
            AND amount_vnd <> 0));

COMMENT ON COLUMN school_balance_entries.target_user_id IS
    'ALLOCATION_DRAW: người được cấp/hạ hạn mức cá nhân. NULL với mọi loại bút toán khác.';
