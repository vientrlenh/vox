-- =============================================================================
-- V5: trần phân phối hạn mức -- trường chỉ được chia ra tối đa bao nhiêu phần trăm ví hạn mức.
--
-- Phần không chia ra là khoản DỰ PHÒNG: nó không bị giữ ở đâu cả, chỉ đơn giản là phần ví chưa ai
-- có trần chi để tiêu vào. Quản trị trường giữ nó lại để cấp thêm cho người thật sự cần giữa kỳ,
-- thay vì chia sạch ngay từ đầu rồi hết đường xoay.
--
-- VÌ SAO LÀ BẢNG RIÊNG, KHÔNG PHẢI CỘT TRÊN school_subscriptions HAY school_subscription_quota_records:
--
--   Cả hai bảng đó được dựng LẠI mỗi kỳ. OrderSettlementService.seedQuotaRecords tạo bản ghi hạn
--   mức mới từ định mức của GÓI cho từng kỳ, và chính javadoc của nó nói rõ đó là chỗ
--   used_amount_vnd bắt đầu lại từ 0 -- đối lập với tiền tự nạp vốn nằm ở school_balances để sống
--   xuyên kỳ. Chính sách phân phối thuộc loại thứ hai: đặt nó lên một hàng theo kỳ thì mỗi lần gia
--   hạn hay nâng cấp, trường lặng lẽ quay về 100% mà không ai được báo.
--
--   Tách bảng còn cho một tính chất mà đặt-trên-hàng-theo-kỳ không có: đổi chính sách là đổi ĐÚNG
--   MỘT dòng và có hiệu lực ngay cho kỳ đang chạy lẫn mọi kỳ sau. Nếu tỷ lệ nằm trên bản ghi hạn
--   mức thì "đổi chính sách" trở thành "ghi lại các hàng", và hàng của kỳ sau vẫn sinh ra với giá
--   trị mặc định.
--
-- TÁCH THEO TỪNG LOẠI HẠN MỨC: ví EXAM chia cho vài giáo viên và một ca chấm hỏng thì rất đắt; ví
-- PRACTICE chia cho hàng trăm học sinh với những khoản nhỏ. Muốn giữ lại 30% ví thi mà chia hết ví
-- luyện nói là một chính sách hợp lý, không phải trường hợp giả tưởng.
-- =============================================================================

CREATE TABLE school_quota_policies (
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    quota_type character varying(20) NOT NULL,
    -- Mặc định 1.0 = chia được toàn bộ, tức hành vi y hệt trước V5. Trường nào chưa từng đụng tới
    -- màn cấu hình thì không thấy gì thay đổi.
    distributable_ratio numeric(5,4) DEFAULT 1.0 NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,

    CONSTRAINT school_quota_policies_pkey PRIMARY KEY (id),
    CONSTRAINT chk_school_quota_policies_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[
        ('EXAM'::character varying)::text,
        ('PRACTICE'::character varying)::text]))),
    -- Chặn ở tầng DB chứ không chỉ ở tầng ứng dụng: một tỷ lệ âm làm phần chia được thành số âm, còn
    -- lớn hơn 1 thì cho chia vượt ví -- đúng thứ ràng buộc này sinh ra để cấm.
    CONSTRAINT chk_school_quota_policies_ratio_in_range CHECK (
        distributable_ratio >= 0 AND distributable_ratio <= 1),
    CONSTRAINT fk_school_quota_policies_school FOREIGN KEY (school_id) REFERENCES schools(id)
);

-- Mỗi trường đúng MỘT chính sách cho mỗi loại hạn mức. Không có ràng buộc này thì hai dòng cùng
-- (school, quota_type) sẽ cho ra trần khác nhau tuỳ dòng nào đọc trúng trước.
CREATE UNIQUE INDEX uq_school_quota_policies_school_quota_type
    ON school_quota_policies USING btree (school_id, quota_type);
