-- =============================================================================
-- V2 -- Tách vòng đời mua hàng ra khỏi hóa đơn, và đưa hạn mức lên cấp TRƯỜNG.
--
--  1. orders / order_items / payment_records
--     Thay cho subscription_request + token_purchase(+item), và cho phần "trạng thái
--     thanh toán" trước đây nằm lẫn trong invoice. invoice.source_id vốn là con trỏ đa
--     hình trỏ vào 3 bảng khác nhau nên KHÔNG khóa ngoại được; giờ thành orders.id có FK thật.
--
--  2. school_balances / school_balance_entries
--     Số dư thuộc về TRƯỜNG, sống xuyên qua mọi lần gia hạn/đổi gói. Trước đây quota gắn
--     vào school_subscription nên tiền trường tự nạp (token_purchase) bị xóa sạch mỗi lần
--     tạo subscription mới -- đây chính là lỗi mà refactor này sửa.
--
--  3. Đổi tên 3 bảng quota cho khớp domain model mới, đồng thời chuyển sang dạng số nhiều.
--
--  4. QuotaType còn 2 giá trị: EXAM (đổi tên từ GRADING) và PRACTICE. CLASS_TEST bị bỏ vì nó là
--     TRẦN CHI trong ví chấm thi chứ không phải ví thứ ba -- xem mục 11.
--
-- CẢNH BÁO: script này XÓA DỮ LIỆU (drop invoice / token_purchase / token_purchase_item /
-- subscription_request; xóa các dòng quota CLASS_TEST ở mục 11). Chỉ chạy được vì hệ thống chưa
-- có dữ liệu thật.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Đổi tên 3 bảng quota (+ ràng buộc đi kèm) cho khớp entity đã đổi tên.
--    PostgreSQL GIỮ NGUYÊN tên constraint khi rename bảng, nên phải đổi tay từng cái.
-- -----------------------------------------------------------------------------
ALTER TABLE plan_quota RENAME TO subscription_plan_quotas;
ALTER TABLE subscription_plan_quotas RENAME CONSTRAINT plan_quota_pkey TO subscription_plan_quotas_pkey;
ALTER TABLE subscription_plan_quotas
    RENAME CONSTRAINT chk_plan_quota_quota_type_valid TO chk_subscription_plan_quotas_quota_type_valid;
ALTER TABLE subscription_plan_quotas RENAME COLUMN included_quantity TO included_amount_vnd;
-- token_unit_price (VND cho mỗi $1 hạn mức) mất lý do tồn tại:
--   * Nó là fx x (1 + service_fee_ratio), tức MỘT TỶ GIÁ ĐÃ CỘNG LÃI -- trường nhìn thấy "1 USD =
--     31.200đ" trong khi tỷ giá thật là 26.000đ. Phần lãi giờ nằm thành một dòng riêng trên đơn hàng.
--   * Nó là cầu nối giữa ví hạn mức tính bằng USD và tiền thu bằng VND. Cả hai đầu đều đã đổi:
--     ví chuyển sang school_balances.balance_vnd, còn quy đổi USD->VND ghi theo từng lượt dùng ở
--     school_balance_entries (cost_usd + fx_rate_used) và ai_usage_record.
--   * Trong code cũ nó vốn đã là cột CHỈ GHI: BuyTokensUseCase/CreatePaymentLinkForTokenPurchaseUseCase
--     đều tự tính lại từ tỷ giá hiện tại chứ không đọc giá trị đóng băng này.
ALTER TABLE subscription_plan_quotas DROP COLUMN token_unit_price;

ALTER TABLE subscription_quota RENAME TO school_subscription_quota_records;
ALTER TABLE school_subscription_quota_records
    RENAME CONSTRAINT subscription_quota_pkey TO school_subscription_quota_records_pkey;
ALTER TABLE school_subscription_quota_records
    RENAME CONSTRAINT chk_subscription_quota_quota_type_valid TO chk_school_subscription_quota_records_quota_type_valid;

ALTER TABLE subscription_quota_user_allocations RENAME TO school_subscription_quota_user_allocations;
ALTER TABLE school_subscription_quota_user_allocations
    RENAME CONSTRAINT subscription_quota_user_allocations_pkey TO school_subscription_quota_user_allocations_pkey;
ALTER TABLE school_subscription_quota_user_allocations
    RENAME CONSTRAINT chk_subscription_quota_user_allocations_quota_type_valid
                   TO chk_school_subscription_quota_user_allocations_quota_type_valid;
ALTER TABLE school_subscription_quota_user_allocations
    RENAME CONSTRAINT uk_subscription_quota_user_allocations_subscription_quota_user
                   TO uk_school_subscription_quota_user_allocations_subscription_user;


-- -----------------------------------------------------------------------------
-- 2. Xóa các bảng đã bị orders/payment_records thay thế.
--    Bỏ invoice trước vì nó có FK trỏ sang subscription_plan.
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS invoice;
DROP TABLE IF EXISTS token_purchase_item;
DROP TABLE IF EXISTS token_purchase;
DROP TABLE IF EXISTS subscription_request;


-- -----------------------------------------------------------------------------
-- 3. orders -- một "ý định mua" duy nhất cho đăng ký / nâng cấp / nạp thêm.
--    Tên bảng BẮT BUỘC số nhiều: `order` là từ khóa SQL.
-- -----------------------------------------------------------------------------
CREATE TABLE orders (
    -- Tiền hàng TRƯỚC phí và giảm giá. Phải ghi thẳng chứ không suy ra được từ tổng: đơn nạp thêm
    -- không có order_items nào để mà cộng lại, nên nếu thiếu cột này thì "trường mua bao nhiêu số dư"
    -- chỉ còn cách tính ngược total - fee -- sai ngay khi discount khác 0, và sai về phía cộng dư
    -- tiền cho trường.
    subtotal_amount_vnd numeric(15,0) NOT NULL,
    total_amount_vnd numeric(15,0) NOT NULL,
    charged_fee_vnd numeric(15,0) DEFAULT 0 NOT NULL,
    discount_amount_vnd numeric(15,0) DEFAULT 0 NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    -- Hạn chót trả tiền, CHỐT LÚC TẠO ĐƠN. Cột riêng chứ không suy từ updated_at vì ba lý do:
    --   * updated_at đổi khi System Admin sửa notes (cột duy nhất được sửa sau khi tạo) -- hạn thanh
    --     toán sẽ tự lùi ra chỉ vì có người ghi chú.
    --   * lúc job chuyển sang EXPIRED thì updated_at thành "lúc mình expire", mất hẳn thông tin "đáng
    --     lẽ hết hạn lúc nào".
    --   * đây là hạn ĐÃ GỬI CHO CỔNG (PayOS expiredAt). Link không được sống lâu hơn đơn, nếu không
    --     trường trả tiền cho một đơn đã chết; đơn cũng không được sống lâu hơn link, nếu không trường
    --     bị khóa mà không còn cách nào trả. Phải lưu đúng con số đã thỏa thuận với cổng.
    expires_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    created_by uuid,
    updated_by uuid,
    version bigint DEFAULT 0 NOT NULL,
    type character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    description character varying(512),
    notes character varying(2048),
    CONSTRAINT chk_orders_type_valid CHECK (((type)::text = ANY (ARRAY[
        ('SUBSCRIPTION_REQUEST'::character varying)::text,
        ('SUBSCRIPTION_UPGRADE'::character varying)::text,
        ('TOPUP'::character varying)::text]))),
    CONSTRAINT chk_orders_status_valid CHECK (((status)::text = ANY (ARRAY[
        ('PENDING'::character varying)::text,
        ('SUCCESS'::character varying)::text,
        ('FAILED'::character varying)::text,
        ('CANCELLED'::character varying)::text,
        ('EXPIRED'::character varying)::text]))),
    -- Số học của đơn phải đóng. Đây là bất biến DUY NHẤT cho cả ba loại đơn, và là thứ chặn được lỗi
    -- "cấp số dư nhiều hơn số tiền đã thu": settlement lấy subtotal làm số dư cộng vào ví, còn cổng
    -- thu đúng total -- hai số đó chỉ khớp nhau khi phép cộng này đúng.
    CONSTRAINT chk_orders_total_amount_vnd_matches_amount_combination CHECK (
        total_amount_vnd = subtotal_amount_vnd + charged_fee_vnd - discount_amount_vnd),
    -- Tách từng cột thay vì gộp một CHECK cho cả bốn: Postgres chỉ báo TÊN ràng buộc bị vi phạm, nên
    -- gộp lại thì lỗi chỉ nói "có một số nào đó âm" mà không nói số nào.
    CONSTRAINT chk_orders_subtotal_amount_vnd_non_negative CHECK (subtotal_amount_vnd >= 0),
    -- Suy ra được từ hai ràng buộc kia (total = subtotal + fee - discount, mà discount <= subtotal +
    -- fee thì total >= 0), giữ lại vì nó tự nói ra ý định thay vì bắt người đọc tự chứng minh.
    CONSTRAINT chk_orders_total_amount_vnd_non_negative CHECK (total_amount_vnd >= 0),
    CONSTRAINT chk_orders_charged_fee_vnd_non_negative CHECK (charged_fee_vnd >= 0),
    CONSTRAINT chk_orders_discount_amount_vnd_non_negative CHECK (discount_amount_vnd >= 0),
    -- Giảm giá không được vượt tiền hàng cộng phí -- nếu không, tổng âm và cổng nhận một đơn thu tiền
    -- ngược về phía trường.
    CONSTRAINT chk_orders_discount_amount_vnd_lower_or_equals_than_subtotal_and_charged_fee CHECK (
        discount_amount_vnd <= subtotal_amount_vnd + charged_fee_vnd),
    CONSTRAINT chk_orders_expires_at_after_created_at CHECK (expires_at > created_at)
);

CREATE TABLE order_items (
    unit_price_vnd numeric(15,0) NOT NULL,
    amount_vnd numeric(15,0) NOT NULL,
    quantity integer NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    order_id uuid NOT NULL,
    -- Trỏ tới thực thể tương ứng với `type` (hiện chỉ SUBSCRIPTION -> subscription_plan.id).
    -- Chưa FK được vì đa hình; nếu về sau chỉ còn đúng một loại thì đổi thành FK thật.
    item_id uuid NOT NULL,
    type character varying(20) NOT NULL,
    CONSTRAINT chk_order_items_type_valid CHECK (((type)::text = ANY (ARRAY[
        ('SUBSCRIPTION'::character varying)::text]))),
    CONSTRAINT chk_order_items_quantity_positive CHECK ((quantity > 0))
);


-- -----------------------------------------------------------------------------
-- 4. payment_records -- MỘT LẦN THỬ thanh toán, không phải chỉ kết quả cuối.
--
--    Dòng sinh ra ngay lúc PHÁT LINK (PENDING) chứ không đợi webhook. Bắt buộc phải vậy:
--    provider_order_ref là thứ DUY NHẤT mà cả hai cổng gửi kèm khi báo về (PayOS orderCode,
--    SePay order_invoice_number), nên nếu chưa có dòng nào mang mã đó thì webhook không tra
--    ngược được callback về đơn nào, và job đối soát cũng không có mã nào để đi hỏi.
--
--    Mã đơn KHÔNG BAO GIỜ dùng lại giữa các lần thử: PayOS trả lỗi "Đơn thanh toán đã tồn tại"
--    với orderCode trùng, còn tài liệu SePay ghi rõ order_invoice_number phải duy nhất. Vì vậy
--    trả lại sau khi thất bại = một DÒNG MỚI với mã mới, không phải sửa dòng cũ.
--
--    Chỉ status và paid_at đổi được sau khi tạo. Sắc thái vì sao không ra tiền (hủy/hết hạn)
--    nằm ở orders, ở đây chỉ cần biết lần thử này ra tiền hay không.
-- -----------------------------------------------------------------------------
CREATE TABLE payment_records (
    amount_vnd numeric(15,0) NOT NULL,
    paid_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    order_id uuid NOT NULL,
    method character varying(20) NOT NULL,
    provider character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    provider_order_ref character varying(100) NOT NULL,
    -- Link đã phát cho lần thử này. Lưu lại để trường bấm "thanh toán" lần nữa thì nhận LẠI ĐÚNG link
    -- cũ, thay vì phải phát link mới: uq_payment_records_one_pending_per_order chỉ cho một lần thử
    -- treo, nên không lưu thì lựa chọn duy nhất còn lại là bỏ link cũ đi -- mà link cũ vẫn trả được,
    -- trường trả vào đó là tiền vào tài khoản nhưng không còn dòng nào đang chờ nó.
    checkout_url character varying(2048),
    -- Mọi thứ CHỈ RIÊNG một cổng mới có, dạng JSON (vd PayOS paymentLinkId). Không tách thành cột
    -- riêng cho từng cổng: paymentLinkId là khái niệm của PayOS, SePay định danh đơn bằng chính
    -- order_invoice_number nên cột đó sẽ NULL vĩnh viễn với một nửa số cổng -- và cổng thứ ba lại
    -- thêm một cột NULL nữa. PaymentProcessPort đã đặt luật "không rò rỉ quy ước riêng của một cổng
    -- qua hợp đồng chung"; một cột mang hình dạng PayOS trong bảng dùng chung chính là rò rỉ đó.
    --
    -- KHÔNG chứa chữ ký: bộ field FORM_POST của SePay có HMAC, lưu lại là ai đọc được DB cũng dựng
    -- lại được một checkout hợp lệ. Chúng cũng tính lại được từ (ref, amount, description) nên lưu
    -- không được thêm gì.
    provider_payload_json text,
    CONSTRAINT chk_payment_records_method_valid CHECK (((method)::text = ANY (ARRAY[
        ('E_BANKING'::character varying)::text,
        ('CARD'::character varying)::text]))),
    CONSTRAINT chk_payment_records_provider_valid CHECK (((provider)::text = ANY (ARRAY[
        ('PAYOS'::character varying)::text,
        ('SEPAY'::character varying)::text]))),
    CONSTRAINT chk_payment_records_status_valid CHECK (((status)::text = ANY (ARRAY[
        ('PENDING'::character varying)::text,
        ('PAID'::character varying)::text,
        ('FAILED'::character varying)::text]))),
    -- Đã trả tiền thì bắt buộc có mốc thời gian cổng ghi nhận, và ngược lại chưa chốt thì
    -- không được có. Thiếu ràng buộc này, một dòng PENDING mang paid_at sẽ lọt vào mọi báo
    -- cáo doanh thu theo ngày.
    CONSTRAINT chk_payment_records_paid_at_matches_status CHECK (
        ((status)::text <> 'PAID' AND paid_at IS NULL) OR ((status)::text = 'PAID' AND paid_at IS NOT NULL))
);


-- -----------------------------------------------------------------------------
-- 5. invoices -- CHỨNG TỪ phát hành sau khi tiền đã về.
--    Không còn mang vòng đời thanh toán (đã sang orders) lẫn phiên cổng (đã sang
--    payment_records). Append-only: sai thì phát hành hóa đơn điều chỉnh mới.
-- -----------------------------------------------------------------------------
CREATE TABLE invoices (
    issue_date timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    order_id uuid NOT NULL,
    -- Lần thanh toán THÀNH CÔNG của đơn. Không mơ hồ vì mỗi order chỉ có tối đa một dòng
    -- payment_records ở trạng thái PAID (xem uq_payment_records_one_paid_per_order).
    payment_id uuid NOT NULL,
    invoice_number character varying(255) NOT NULL
);


-- -----------------------------------------------------------------------------
-- 6. school_balances -- ví tiền TỰ NẠP của trường. MỘT cột duy nhất.
--
--    CỐ TÌNH không có cột "hạn mức kèm gói" ở đây: gói cấp hạn mức theo TỪNG QuotaType
--    (EXAM / PRACTICE) và đã được school_subscription_quota_records theo dõi
--    đầy đủ (total_allocated_amount_vnd + used_amount_vnd). Một số dư cấp trường duy nhất
--    không diễn đạt được "còn 300.000 nhưng không được dùng để chấm bài", nên nhân đôi số tiền
--    đó ra đây là tạo hai nguồn sự thật chắc chắn sẽ lệch nhau.
--
--    Nhờ vậy giữ được bất biến sạch: SUM(school_balance_entries.amount_vnd) = balance_vnd.
-- -----------------------------------------------------------------------------
CREATE TABLE school_balances (
    -- Không bao giờ hết hạn. CỐ Ý không có CHECK >= 0 -- phần âm ở đây CHÍNH LÀ nợ, thay cho
    -- điều kiện used_quantity > total_allocated cũ.
    balance_vnd numeric(18,6) DEFAULT 0 NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


-- -----------------------------------------------------------------------------
-- 7. school_balance_entries -- sổ cái append-only của school_balances.
--    CHỈ ghi phần VƯỢT hạn mức kèm gói; tiêu dùng còn trong hạn mức đã nằm ở
--    school_subscription_quota_records + token_usage_event.
--
--    Tham chiếu nguồn dùng cột CÓ KIỂU (order_id / exam_session_id / actor_id) thay cho cặp
--    (source_type, source_id) đa hình như invoice cũ -- mỗi cột khóa ngoại được thật.
-- -----------------------------------------------------------------------------
CREATE TABLE school_balance_entries (
    -- Dương = nạp/hoàn/điều chỉnh tăng, âm = trừ. numeric(18,6) chứ không phải (15,0) như tiền
    -- mặt qua cổng: một lượt luyện nói có thể chỉ tốn vài phần trăm đồng, làm tròn về số nguyên
    -- là mất trắng khoản trừ đó.
    amount_vnd numeric(18,6) NOT NULL,
    balance_after_vnd numeric(18,6) NOT NULL,
    -- Giữ USD: đây là hóa đơn NHÀ CUNG CẤP tính cho mình, dùng để đối soát ngược với
    -- ai_usage_record và làm đầu vào cho QuotaPricingCalibrationService. Quy sang VND ở đây sẽ
    -- làm rate calibrate trôi theo tỷ giá thay vì theo chi phí thật.
    cost_usd numeric(18,6),
    fx_rate_used numeric(12,4),
    occurred_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    -- Gói đang ACTIVE lúc phát sinh -- CHỈ để truy vết, số dư không thuộc về gói nào.
    subscription_id uuid,
    order_id uuid,
    exam_session_id uuid,
    actor_id uuid,
    entry_type character varying(20) NOT NULL,
    quota_type character varying(20),
    reason character varying(2048),
    CONSTRAINT chk_school_balance_entries_entry_type_valid CHECK (((entry_type)::text = ANY (ARRAY[
        ('TOP_UP'::character varying)::text,
        ('OVERAGE_CHARGE'::character varying)::text,
        ('REFUND'::character varying)::text,
        ('ADJUSTMENT'::character varying)::text]))),
    CONSTRAINT chk_school_balance_entries_quota_type_valid CHECK ((quota_type IS NULL OR (quota_type)::text = ANY (ARRAY[
        ('EXAM'::character varying)::text,
        ('PRACTICE'::character varying)::text]))),
    -- Nạp/hoàn tiền BẮT BUỘC gắn với một đơn hàng -- không cho cộng tiền "từ hư không".
    CONSTRAINT chk_school_balance_entries_credit_from_order CHECK (
        ((entry_type)::text NOT IN ('TOP_UP', 'REFUND') OR (order_id IS NOT NULL AND amount_vnd > (0)::numeric))),
    -- Trừ vượt hạn mức phải nói rõ phiên nào, loại nào, và chi phí gốc bao nhiêu.
    CONSTRAINT chk_school_balance_entries_overage_traceable CHECK (
        ((entry_type)::text <> 'OVERAGE_CHARGE' OR (
            exam_session_id IS NOT NULL AND quota_type IS NOT NULL
            AND cost_usd IS NOT NULL AND fx_rate_used IS NOT NULL AND amount_vnd < (0)::numeric))),
    -- KHÔNG còn chk_school_balance_entries_no_class_test_charge: nó tồn tại chỉ để chặn khoản trừ
    -- TRÙNG do CLASS_TEST bị coi là ví thứ hai bên cạnh GRADING (một bài kiểm tra trên lớp bị trừ
    -- cùng totalCostUsd hai lần). CLASS_TEST giờ không còn là QuotaType nữa nên nguồn gây trùng đã
    -- mất -- giữ lại một ràng buộc canh giá trị không thể tồn tại là canh một cánh cửa đã xây bịt.
    -- Điều chỉnh tay luôn phải có người thực hiện + lý do, cùng chuẩn ForceSuspendSubscriptionUseCase.
    CONSTRAINT chk_school_balance_entries_adjustment_audited CHECK (
        ((entry_type)::text <> 'ADJUSTMENT' OR (actor_id IS NOT NULL AND reason IS NOT NULL)))
);


-- -----------------------------------------------------------------------------
-- 8. Khóa chính
-- -----------------------------------------------------------------------------
ALTER TABLE ONLY orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY payment_records
    ADD CONSTRAINT payment_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);

ALTER TABLE ONLY invoices
    ADD CONSTRAINT invoices_invoice_number_key UNIQUE (invoice_number);

ALTER TABLE ONLY school_balances
    ADD CONSTRAINT school_balances_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_balance_entries
    ADD CONSTRAINT school_balance_entries_pkey PRIMARY KEY (id);


-- -----------------------------------------------------------------------------
-- 9. Index
-- -----------------------------------------------------------------------------
-- Mỗi trường đúng MỘT dòng số dư.
CREATE UNIQUE INDEX uq_school_balances_school ON school_balances USING btree (school_id);

-- Cùng một giao dịch phía cổng không được ghi nhận hai lần khi webhook và
-- PendingOrderReconciler cùng xử lý.
CREATE UNIQUE INDEX uq_payment_records_provider_ref
    ON payment_records USING btree (provider, provider_order_ref);

-- Mỗi đơn chỉ được thanh toán THÀNH CÔNG một lần. Không có ràng buộc này thì việc trả trùng
-- (tạo 2 payment link, trả cả hai) sẽ cộng tiền vào số dư hai lần.
CREATE UNIQUE INDEX uq_payment_records_one_paid_per_order
    ON payment_records USING btree (order_id) WHERE ((status)::text = 'PAID');

-- Mỗi đơn chỉ được có MỘT lần thử đang treo. Đây là ràng buộc CHẶN TRƯỚC, còn
-- uq_payment_records_one_paid_per_order chỉ chặn SAU khi tiền đã vào: muốn phát link mới thì
-- buộc phải chốt lần thử cũ, mà muốn chốt thì phải đi hỏi cổng xem nó đã ra tiền chưa. Không có
-- nó, trường bấm "thanh toán lại" vài lần rồi trả hai link — tiền vào tài khoản hai lần nhưng
-- lần thứ hai bị index PAID từ chối, thành tiền thật không có dòng nào đại diện.
CREATE UNIQUE INDEX uq_payment_records_one_pending_per_order
    ON payment_records USING btree (order_id) WHERE ((status)::text = 'PENDING');

-- Mỗi đơn chỉ được cộng tiền vào sổ cái ĐÚNG MỘT LẦN. Không áp cho OVERAGE_CHARGE vì một
-- exam_session sinh nhiều lượt trừ.
CREATE UNIQUE INDEX uq_school_balance_entries_order
    ON school_balance_entries USING btree (order_id, entry_type) WHERE (order_id IS NOT NULL);

-- Mỗi trường chỉ được có MỘT đơn gói đang mở -- chốt chặn cho lỗi "tạo 2 payment link, trả link
-- thứ hai thì tiền vào nhưng không cấp gì".
CREATE UNIQUE INDEX uq_orders_one_open_subscription_order
    ON orders USING btree (school_id)
    WHERE (((status)::text = 'PENDING') AND ((type)::text IN ('SUBSCRIPTION_REQUEST', 'SUBSCRIPTION_UPGRADE')));

CREATE INDEX idx_orders_school_created ON orders USING btree (school_id, created_at DESC);

-- Job quét đơn quá hạn chỉ quan tâm đơn còn treo, mà đơn treo luôn là thiểu số so với đơn đã chốt --
-- index từng phần để nó không phải đi qua toàn bộ lịch sử đơn hàng mỗi lần chạy.
CREATE INDEX idx_orders_pending_expires_at ON orders USING btree (expires_at)
    WHERE ((status)::text = 'PENDING');
CREATE INDEX idx_order_items_order ON order_items USING btree (order_id);
CREATE INDEX idx_payment_records_order ON payment_records USING btree (order_id);
CREATE INDEX idx_invoices_order ON invoices USING btree (order_id);
CREATE INDEX idx_school_balance_entries_school_occurred
    ON school_balance_entries USING btree (school_id, occurred_at DESC);


-- -----------------------------------------------------------------------------
-- 10. Khóa ngoại
-- -----------------------------------------------------------------------------
ALTER TABLE ONLY orders
    ADD CONSTRAINT fk_orders_school FOREIGN KEY (school_id) REFERENCES schools(id);

ALTER TABLE ONLY order_items
    ADD CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE ONLY payment_records
    ADD CONSTRAINT fk_payment_records_order FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE ONLY invoices
    ADD CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE ONLY invoices
    ADD CONSTRAINT fk_invoices_payment FOREIGN KEY (payment_id) REFERENCES payment_records(id);

ALTER TABLE ONLY school_balances
    ADD CONSTRAINT fk_school_balances_school FOREIGN KEY (school_id) REFERENCES schools(id);

ALTER TABLE ONLY school_balance_entries
    ADD CONSTRAINT fk_school_balance_entries_school FOREIGN KEY (school_id) REFERENCES schools(id);

ALTER TABLE ONLY school_balance_entries
    ADD CONSTRAINT fk_school_balance_entries_order FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE ONLY school_balance_entries
    ADD CONSTRAINT fk_school_balance_entries_exam_session FOREIGN KEY (exam_session_id) REFERENCES exam_sessions(id);

ALTER TABLE ONLY school_balance_entries
    ADD CONSTRAINT fk_school_balance_entries_subscription FOREIGN KEY (subscription_id) REFERENCES school_subscription(id);


-- -----------------------------------------------------------------------------
-- 11. QuotaType: bỏ CLASS_TEST, đổi GRADING -> EXAM.
--
--     CLASS_TEST không phải một ví thứ ba mà là TRẦN CHI nằm trong ví chấm thi: mỗi bài kiểm tra
--     trên lớp bị trừ CÙNG MỘT khoản totalCostUsd hai lần -- một lần dưới GRADING, một lần dưới
--     CLASS_TEST -- nên used_amount_vnd cấp trường luôn cao hơn tiền thật, và phải dựng riêng
--     chk_school_balance_entries_no_class_test_charge để chặn khoản trùng đó chạm vào tiền.
--
--     Trần chi theo GIÁO VIÊN thì vẫn là tính năng thật và được giữ nguyên: nó sống ở
--     school_subscription_quota_user_allocations, chỉ đổi quota_type sang EXAM. Việc trần đó chỉ
--     áp cho bài kiểm tra trên lớp là do phía soi quyết định (chỉ truyền userId khi kind =
--     CLASS_TEST), không phải do quota_type.
--
--     Đổi GRADING -> EXAM để hai ví trùng tên với QuotaPricingSource (EXAM / PRACTICE) -- cùng một
--     ranh giới thì nên cùng một tên, vì đây chính là ranh giới hai pipeline AI khác nhau.
-- -----------------------------------------------------------------------------

-- Thứ tự bắt buộc: nới CHECK ra trước, sửa dữ liệu, rồi mới siết lại. Sửa dữ liệu khi CHECK cũ còn
-- hiệu lực sẽ vỡ ngay ở dòng UPDATE đầu tiên vì 'EXAM' không nằm trong danh sách cũ.
ALTER TABLE subscription_plan_quotas DROP CONSTRAINT chk_subscription_plan_quotas_quota_type_valid;
ALTER TABLE school_subscription_quota_records DROP CONSTRAINT chk_school_subscription_quota_records_quota_type_valid;
ALTER TABLE school_subscription_quota_user_allocations
    DROP CONSTRAINT chk_school_subscription_quota_user_allocations_quota_type_valid;
ALTER TABLE school_debt_event DROP CONSTRAINT chk_school_debt_event_quota_type_valid;
ALTER TABLE token_usage_event DROP CONSTRAINT chk_token_usage_event_quota_type_valid;

UPDATE subscription_plan_quotas SET quota_type = 'EXAM' WHERE quota_type = 'GRADING';
UPDATE school_subscription_quota_records SET quota_type = 'EXAM' WHERE quota_type = 'GRADING';
UPDATE school_subscription_quota_user_allocations SET quota_type = 'EXAM' WHERE quota_type = 'CLASS_TEST';
UPDATE school_balance_entries SET quota_type = 'EXAM' WHERE quota_type IN ('GRADING', 'CLASS_TEST');
-- school_debt_event và token_usage_event là sổ APPEND-ONLY: map sang EXAM chứ không xóa. Dòng
-- CLASS_TEST ở đây ghi lại một sự kiện CÓ THẬT (trường từng vượt trần loại đó), xóa đi là sửa lịch
-- sử; để lại dưới tên ví mới thì vẫn tra ngược được.
UPDATE school_debt_event SET quota_type = 'EXAM' WHERE quota_type IN ('GRADING', 'CLASS_TEST');
UPDATE token_usage_event SET quota_type = 'EXAM' WHERE quota_type = 'GRADING';

-- Ví CLASS_TEST cấp trường thì XÓA hẳn, không map: nó không mang tiền riêng nào. included_amount_vnd
-- của nó là một con số TRẦN, còn used_amount_vnd là bản sao của phần đã tính dưới GRADING -- map
-- sang EXAM sẽ cộng dồn vào ví thật và nhân đôi cả định mức lẫn số đã dùng.
DELETE FROM subscription_plan_quotas WHERE quota_type = 'CLASS_TEST';
DELETE FROM school_subscription_quota_records WHERE quota_type = 'CLASS_TEST';
-- Cùng lý do: dòng CLASS_TEST ở đây là lần trừ TRÙNG của chính khoản đã trừ dưới GRADING.
DELETE FROM token_usage_event WHERE quota_type = 'CLASS_TEST';

ALTER TABLE subscription_plan_quotas
    ADD CONSTRAINT chk_subscription_plan_quotas_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[
        ('EXAM'::character varying)::text,
        ('PRACTICE'::character varying)::text])));
ALTER TABLE school_subscription_quota_records
    ADD CONSTRAINT chk_school_subscription_quota_records_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[
        ('EXAM'::character varying)::text,
        ('PRACTICE'::character varying)::text])));
ALTER TABLE school_subscription_quota_user_allocations
    ADD CONSTRAINT chk_school_subscription_quota_user_allocations_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[
        ('EXAM'::character varying)::text,
        ('PRACTICE'::character varying)::text])));
ALTER TABLE school_debt_event
    ADD CONSTRAINT chk_school_debt_event_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[
        ('EXAM'::character varying)::text,
        ('PRACTICE'::character varying)::text])));
ALTER TABLE token_usage_event
    ADD CONSTRAINT chk_token_usage_event_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[
        ('EXAM'::character varying)::text,
        ('PRACTICE'::character varying)::text])));
