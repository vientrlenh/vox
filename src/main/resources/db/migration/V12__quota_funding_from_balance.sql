-- =============================================================================
-- V12: trường chuyển tiền từ ví TỰ NẠP (school_balances) vào ví HẠN MỨC của một loại quota.
--
-- VÌ SAO KHÔNG "trừ ví khi chia hạn mức cho từng người" -- phương án đầu tiên, và nó trừ tiền HAI LẦN:
--
--   Ví PRACTICE cạn (10tr/10tr), ví tự nạp còn 2tr. Quản trị trường duyệt 500k cho học sinh A và
--   phương án kia trừ thẳng 500k khỏi ví -> còn 1,5tr. A luyện một lượt tốn 300k thật:
--   ConsumeQuotaService.tryConsume hỏng (ví hạn mức đã cạn) -> chargeOverage trừ TIẾP 300k khỏi ví.
--   Tổng cộng ví mất 800k cho 300k chi phí AI có thật, và school_ai_spend_entries chỉ ghi 300k -- hai
--   sổ nói hai con số khác nhau, đúng cái mà javadoc ConsumeQuotaService cảnh báo. Nếu A không luyện
--   buổi nào thì 500k mất hẳn: REFUND đòi order_id (chk_school_balance_entries_credit_from_order) nên
--   không có đường hoàn.
--
--   Gốc rễ: phương án đó bắt dòng phân bổ GIỮ tiền, trong khi cả QuotaType, SchoolBalance lẫn
--   ConsumeQuotaService đều chép cùng một luật -- "trần đó là một GIỚI HẠN, không bao giờ là một số dư".
--
-- CÁCH LÀM Ở ĐÂY: tiền đi vào chỗ tiền vốn đã ở -- ví hạn mức cấp trường. Ví tự nạp giảm ĐÚNG MỘT lần
-- kèm một bút toán, total_allocated_amount_vnd tăng đúng bằng đó, rồi việc chia hạn mức chạy qua đường
-- cũ không sửa gì. Lúc học sinh luyện thật thì tryConsume thấy ví còn chỗ và chargeOverage KHÔNG chạy,
-- nên không có lần trừ thứ hai. Phần chưa tiêu ở lại ví của TRƯỜNG để chia cho người khác, thay vì mắc
-- kẹt trên tên một học sinh.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Phần hạn mức đến từ ví tự nạp, tách khỏi phần gói cấp.
--
--    Cần cột riêng vì OrderSettlementService.seedQuotaRecords dựng LẠI bản ghi hạn mức mỗi kỳ từ
--    subscription_plan_quotas.included_amount_vnd. Không có cột này thì tiền trường vừa bỏ ra bị xoá
--    sạch vào đúng ngày họ gia hạn -- và khác hẳn ca "mất trần chi" mà V-trước đã xử, đây là mất TIỀN
--    THẬT. Có cột thì kỳ mới cộng lại được phần chưa tiêu (xem carryForwardFunding).
--
--    Cũng là cột duy nhất trả lời được "gói cấp bao nhiêu, trường tự bỏ thêm bao nhiêu" cho màn hình
--    và cho báo cáo -- total_allocated_amount_vnd sau lần nạp đầu tiên đã trộn lẫn hai nguồn.
-- -----------------------------------------------------------------------------
ALTER TABLE school_subscription_quota_records
    ADD COLUMN funded_from_balance_vnd numeric(18, 6) NOT NULL DEFAULT 0;

-- Không âm, và không lớn hơn tổng: phần nạp thêm luôn được CỘNG vào total cùng một câu lệnh
-- (addFundingFromBalance), nên hai cột lệch nhau nghĩa là đã có ai đó sửa một cột mà quên cột kia.
ALTER TABLE school_subscription_quota_records
    ADD CONSTRAINT chk_school_subscription_quota_records_funded_within_total CHECK (
        funded_from_balance_vnd >= 0
        AND funded_from_balance_vnd <= total_allocated_amount_vnd);


-- -----------------------------------------------------------------------------
-- 2. Loại bút toán mới trên sổ cái ví.
--
--    QUOTA_FUNDING là tiền RỜI ví (amount_vnd < 0) nhưng KHÔNG phải một khoản chi cho AI: nó chuyển
--    sang một túi khác của cùng nhà trường. Vì thế nó không mang cost_usd/fx_rate_used và không gắn
--    với phiên thi hay phiên luyện nào -- ba cột chỉ có nghĩa với OVERAGE_CHARGE.
--
--    Bất biến SUM(entries.amount_vnd) = balance_vnd vẫn nguyên: một bút toán duy nhất cho một lần trừ
--    duy nhất. Đây cũng là lý do KHÔNG ghi thêm dòng nào ở school_ai_spend_entries -- sổ đó trả lời
--    "trường đã tiêu bao nhiêu cho AI", mà chuyển tiền giữa hai túi thì chưa tiêu đồng nào.
-- -----------------------------------------------------------------------------
ALTER TABLE school_balance_entries DROP CONSTRAINT chk_school_balance_entries_entry_type_valid;
ALTER TABLE school_balance_entries
    ADD CONSTRAINT chk_school_balance_entries_entry_type_valid CHECK (((entry_type)::text = ANY (ARRAY[
        ('TOP_UP'::character varying)::text,
        ('OVERAGE_CHARGE'::character varying)::text,
        ('QUOTA_FUNDING'::character varying)::text,
        ('REFUND'::character varying)::text,
        ('ADJUSTMENT'::character varying)::text])));

-- Mỗi loại bút toán ở bảng này đều có một ràng buộc nói rõ cột nào bắt buộc, cột nào phải rỗng
-- (credit_from_order cho TOP_UP/REFUND, overage_traceable cho OVERAGE_CHARGE, adjustment_audited cho
-- ADJUSTMENT). Bỏ qua ở đây là để đúng loại bút toán mới trở thành loại duy nhất không ai canh.
--
-- actor_id BẮT BUỘC: khác OVERAGE_CHARGE do hệ thống tự sinh, đây là một QUYẾT ĐỊNH của con người và
-- không hoàn lại được, nên sổ phải nói ai đã bấm. quota_type BẮT BUỘC vì tiền đi vào đúng MỘT ví và
-- không có đường quay ngược sang ví kia -- một bút toán không nói rõ túi nào là một khoản không tra
-- ngược được.
ALTER TABLE school_balance_entries
    ADD CONSTRAINT chk_school_balance_entries_quota_funding_traceable CHECK (
        ((entry_type)::text <> 'QUOTA_FUNDING' OR (
            actor_id IS NOT NULL
            AND quota_type IS NOT NULL
            AND amount_vnd < (0)::numeric
            AND order_id IS NULL
            AND cost_usd IS NULL
            AND fx_rate_used IS NULL
            AND num_nonnulls(exam_session_id, practice_session_id) = 0)));
