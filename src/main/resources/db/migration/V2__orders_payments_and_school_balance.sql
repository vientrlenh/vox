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
-- UNIQUE chứ không phải index thường: "một đơn đúng một hóa đơn" là bất biến nghiệp vụ, mà chốt duy
-- nhất hiện chỉ nằm ở existsByOrderId trong OrderSettlementService -- tức ở tầng ứng dụng. Đường đọc
-- (Order.invoice) gom 1-1 bằng Collectors.toMap nên một dòng thừa không hỏng riêng một đơn, nó ném
-- lỗi cả trang lịch sử. Unique index vừa chặn ở đúng chỗ, vừa thay được index tra cứu cũ.
CREATE UNIQUE INDEX uq_invoices_order ON invoices USING btree (order_id);
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


-- -----------------------------------------------------------------------------
-- 12. school_subscription_events -- sổ audit cho những lần System Admin can thiệp
--     vào vòng đời một gói (đình chỉ / gỡ đình chỉ).
--
--     Ba cột suspended_at/suspended_reason/suspended_by trên school_subscription là TRẠNG THÁI,
--     không phải LỊCH SỬ: gỡ đình chỉ xóa cả ba về null nên sau đó không còn dấu vết nào cho thấy
--     trường từng bị đình chỉ, ai làm, vì sao. Đây là thao tác cưỡng chế nhắm vào khách hàng đang
--     trả tiền, mất dấu vết là không chấp nhận được.
--
--     KHÔNG dùng lại financial_event: bảng đó sinh ra cho TIỀN (amount_signed / currency /
--     payment_method đều NOT NULL), nên mỗi lần đình chỉ phải nhét vào một khoản 0 VND trả bằng
--     "MANUAL" -- ba giá trị vô nghĩa chỉ để thỏa ràng buộc. Phần tiền của bảng đó giờ thuộc về
--     orders/payment_records/invoices/school_balance_entries.
-- -----------------------------------------------------------------------------
CREATE TABLE school_subscription_events (
    occurred_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    subscription_id uuid NOT NULL,
    -- Người thực hiện luôn có: cả hai loại sự kiện đều là hành động của System Admin, không phải
    -- thứ hệ thống tự sinh ra.
    actor_id uuid NOT NULL,
    event_type character varying(20) NOT NULL,
    reason character varying(2048),
    CONSTRAINT chk_school_subscription_events_event_type_valid CHECK (((event_type)::text = ANY (ARRAY[
        ('SUSPENDED'::character varying)::text,
        ('UNSUSPENDED'::character varying)::text]))),
    -- Đình chỉ BẮT BUỘC nêu lý do; gỡ đình chỉ thì ghi chú là tùy chọn. Cắt quyền dùng của một
    -- trường mà không ghi lại vì sao là đúng thứ sổ audit này sinh ra để ngăn.
    CONSTRAINT chk_school_subscription_events_suspend_has_reason CHECK (
        ((event_type)::text <> 'SUSPENDED' OR reason IS NOT NULL))
);

ALTER TABLE ONLY school_subscription_events
    ADD CONSTRAINT school_subscription_events_pkey PRIMARY KEY (id);

CREATE INDEX idx_school_subscription_events_school ON school_subscription_events
    USING btree (school_id, occurred_at DESC);

ALTER TABLE ONLY school_subscription_events
    ADD CONSTRAINT fk_school_subscription_events_school FOREIGN KEY (school_id) REFERENCES schools(id);

ALTER TABLE ONLY school_subscription_events
    ADD CONSTRAINT fk_school_subscription_events_subscription
    FOREIGN KEY (subscription_id) REFERENCES school_subscription(id);


-- -----------------------------------------------------------------------------
-- 13. Đổi tên 6 bảng còn sót sang số nhiều cho khớp entity.
--
--     Postgres tự cập nhật mọi FK đang trỏ tới bảng bị đổi tên, nên các ràng buộc dựng ở mục 10/12
--     (còn viết `REFERENCES school_subscription(id)`) vẫn đúng sau bước này -- chúng chạy TRƯỚC, lúc
--     tên cũ còn hiệu lực. Chỉ TÊN CONSTRAINT là không tự đổi theo, phải sửa tay từng cái như mục 1.
--
--     Tên constraint bên dưới bám theo đúng cái entity khai báo, kể cả khi entity vẫn giữ dạng số ít
--     (vd chk_ai_usage_record_usage_type_valid): đổi cho "đẹp" mà lệch khỏi entity thì Hibernate sẽ
--     dựng lại constraint trùng nội dung dưới tên khác ở mọi môi trường tạo schema từ entity.
-- -----------------------------------------------------------------------------
ALTER TABLE ai_usage_record RENAME TO ai_usage_records;
ALTER TABLE ai_usage_records RENAME CONSTRAINT ai_usage_record_pkey TO ai_usage_records_pkey;

ALTER TABLE school_debt_event RENAME TO school_debt_events;
ALTER TABLE school_debt_events RENAME CONSTRAINT school_debt_event_pkey TO school_debt_events_pkey;
ALTER TABLE school_debt_events
    RENAME CONSTRAINT chk_school_debt_event_quota_type_valid TO chk_school_debt_events_quota_type_valid;

ALTER TABLE exchange_rate_snapshot RENAME TO exchange_rate_snapshots;
ALTER TABLE exchange_rate_snapshots RENAME CONSTRAINT exchange_rate_snapshot_pkey TO exchange_rate_snapshots_pkey;

ALTER TABLE quota_pricing_calibration RENAME TO quota_pricing_calibrations;
ALTER TABLE quota_pricing_calibrations
    RENAME CONSTRAINT quota_pricing_calibration_pkey TO quota_pricing_calibrations_pkey;

ALTER TABLE subscription_plan RENAME TO subscription_plans;
ALTER TABLE subscription_plans RENAME CONSTRAINT subscription_plan_pkey TO subscription_plans_pkey;

ALTER TABLE school_subscription RENAME TO school_subscriptions;
ALTER TABLE school_subscriptions RENAME CONSTRAINT school_subscription_pkey TO school_subscriptions_pkey;
ALTER TABLE school_subscriptions
    RENAME CONSTRAINT chk_school_subscription_status_valid TO chk_school_subscriptions_status_valid;


-- -----------------------------------------------------------------------------
-- 14. ai_usage_records: ghi thêm chi phí đã quy sang VND.
--
--     cost_usd Ở LẠI: đó là hóa đơn nhà cung cấp tính cho mình, cần để đối soát ngược và làm đầu vào
--     cho QuotaPricingCalibrationService (calibrate theo chi phí thật, không theo tỷ giá).
--
--     cost_vnd numeric(18,6) chứ không phải (15,0): đây là giá trị lẻ nhất trong hệ thống -- một lượt
--     nói có thể chỉ tốn vài phần trăm đồng -- và nó là đầu vào cộng dồn cho
--     school_subscription_quota_records.used_amount_vnd. Làm tròn từng dòng về số nguyên là mất trắng
--     khoản trừ, và sai số HALF_UP tích lũy lệch một chiều qua hàng nghìn lượt.
--
--     fx_rate_used numeric(12,4) trùng school_balance_entries.fx_rate_used: tỷ giá là HỆ SỐ NHÂN, không
--     phải tiền, nên một khái niệm chỉ có đúng một hình dạng số. Chốt theo TỪNG DÒNG chứ không quy đổi
--     lại lúc đọc: retry Kafka/DLT có thể đẩy một sự kiện sang hôm sau, và một phiên thi vắt qua ngày
--     đổi tỷ giá phải cộng ra đúng số tiền thật -- xem RecordAiUsageUseCase.
-- -----------------------------------------------------------------------------
ALTER TABLE ai_usage_records ADD COLUMN cost_vnd numeric(18,6);
ALTER TABLE ai_usage_records ADD COLUMN fx_rate_used numeric(12,4);

-- Bảng chưa có dữ liệu thật (xem cảnh báo đầu file); backfill 0 chỉ để NOT NULL bên dưới không vỡ nếu
-- có vài dòng rác từ lần chạy thử.
UPDATE ai_usage_records SET cost_vnd = 0 WHERE cost_vnd IS NULL;
UPDATE ai_usage_records SET fx_rate_used = 0 WHERE fx_rate_used IS NULL;

ALTER TABLE ai_usage_records ALTER COLUMN cost_vnd SET NOT NULL;
ALTER TABLE ai_usage_records ALTER COLUMN fx_rate_used SET NOT NULL;


-- -----------------------------------------------------------------------------
-- 15. school_debt_events: mọi cột tiền chuyển sang VND.
--
--     Chỉ đổi TÊN, không đổi kiểu: numeric(18,6) vốn đã đúng cho VND ở mức đo lường (cùng nhóm với
--     school_balances.balance_vnd). Giá trị cũ tính bằng USD không cần quy đổi vì bảng chưa có dữ liệu
--     thật -- nếu có, đây sẽ phải là một bước UPDATE nhân tỷ giá chứ không phải rename.
--
--     used_quantity_usd -> used_amount_vnd chứ không phải used_quantity_vnd: "quantity" là tàn dư từ
--     thời hạn mức đếm theo ĐƠN VỊ token. Giờ nó là tiền, và tên phải trùng
--     school_subscription_quota_records.used_amount_vnd -- cùng một đại lượng thì cùng một tên.
-- -----------------------------------------------------------------------------
ALTER TABLE school_debt_events RENAME COLUMN trigger_amount_usd TO trigger_amount_vnd;
ALTER TABLE school_debt_events RENAME COLUMN total_allocated_usd TO total_allocated_vnd;
ALTER TABLE school_debt_events RENAME COLUMN used_quantity_usd TO used_amount_vnd;
ALTER TABLE school_debt_events RENAME COLUMN overage_usd TO overage_vnd;


-- -----------------------------------------------------------------------------
-- 16. school_balance_entries: cho phép khoản trừ đến từ một phiên LUYỆN NÓI.
--
--     Cột RIÊNG chứ không dùng chung exam_session_id: cột đó mang khóa ngoại thật tới exam_sessions,
--     nên nhét practice session id vào sẽ vi phạm FK. Đây cũng đúng lựa chọn "cột CÓ KIỂU" mà bảng này
--     đã cố ý dùng thay cho cặp (source_type, source_id) đa hình kiểu invoice cũ -- xem mục 7.
--
--     Vì sao PRACTICE giờ được tiêu vào số dư: lúc trừ thì Azure đã tính tiền xong rồi (chi phí thật
--     của lượt vừa nói, Python gửi kèm ngay trong request submit_turn). Chặn ở đó không giữ lại được
--     đồng nào, chỉ làm khoản chi biến mất khỏi sổ sách. Việc học sinh có được nói tiếp hay không là
--     câu hỏi khác, trả lời bằng cờ fundsExhausted -- xem ConsumeQuotaService.
-- -----------------------------------------------------------------------------
ALTER TABLE school_balance_entries ADD COLUMN practice_session_id uuid;

ALTER TABLE ONLY school_balance_entries
    ADD CONSTRAINT fk_school_balance_entries_practice_session
    FOREIGN KEY (practice_session_id) REFERENCES practice_session(id);

-- ĐÚNG MỘT trong hai cột session được set, không phải "exam_session_id NOT NULL" như bản cũ.
-- num_nonnulls đọc thẳng ra ý định; viết bằng OR/AND lồng nhau thì lần sửa sau rất dễ nới thành
-- "ít nhất một", và một bút toán mang cả hai id là một khoản trừ không biết thuộc về phiên nào.
ALTER TABLE school_balance_entries DROP CONSTRAINT chk_school_balance_entries_overage_traceable;
ALTER TABLE school_balance_entries
    ADD CONSTRAINT chk_school_balance_entries_overage_traceable CHECK (
        ((entry_type)::text <> 'OVERAGE_CHARGE' OR (
            num_nonnulls(exam_session_id, practice_session_id) = 1
            AND quota_type IS NOT NULL
            AND cost_usd IS NOT NULL AND fx_rate_used IS NOT NULL AND amount_vnd < (0)::numeric)));

CREATE INDEX idx_school_balance_entries_practice_session ON school_balance_entries
    USING btree (practice_session_id) WHERE practice_session_id IS NOT NULL;


-- -----------------------------------------------------------------------------
-- 17. exchange_rate_snapshots: nói rõ đang chốt tỷ giá của ĐỒNG TIỀN NÀO.
--
--     Bảng cũ ngầm định "chỉ có USD" ngay trong tên cột (usd_to_vnd_rate). Thêm currency_code để
--     findLatest lọc được theo đồng tiền -- không có nó, thêm đồng thứ hai là "mới nhất" trả về bản
--     ghi của đồng nào vừa chạy job sau cùng, và giá bán quota lặng lẽ nhảy sang tỷ giá đó mà không
--     có lỗi nào báo ra. CHECK tạm khóa ở 'USD' vì hôm nay mới nuôi đúng một đồng; nới CHECK là bước
--     có ý thức, khác hẳn việc lặng lẽ đọc nhầm.
-- -----------------------------------------------------------------------------
ALTER TABLE exchange_rate_snapshots RENAME COLUMN usd_to_vnd_rate TO exchange_rate_to_vnd;
ALTER TABLE exchange_rate_snapshots RENAME COLUMN source TO source_url;

ALTER TABLE exchange_rate_snapshots
    ADD COLUMN currency_code character varying(20) DEFAULT 'USD' NOT NULL;
ALTER TABLE exchange_rate_snapshots
    ADD CONSTRAINT chk_exchange_rate_snapshots_currency_code_valid CHECK (
        ((currency_code)::text = 'USD'));


-- -----------------------------------------------------------------------------
-- 18. subscription_plans: giá theo VND + kỳ hạn linh hoạt.
--
--     price_per_year -> price_vnd: gói không còn buộc phải là một NĂM, nên tên cột không được khoá
--     cứng kỳ hạn vào giá. Kiểu numeric(15,0) giữ nguyên và phải TRÙNG orders.total_amount_vnd --
--     rộng hơn ở đây thì Postgres làm tròn im lặng lúc tạo đơn, gói niêm yết một giá mà thu một giá.
--
--     validity_days -> (period_type, period_count): chuyển thẳng sang DAY/n nên không mất thông tin;
--     gói mới có thể khai MONTH/3 hay YEAR/1 cho đúng ngôn ngữ nghiệp vụ.
--
--     service_fee_ratio BỎ khỏi gói: phí dịch vụ giờ là một DÒNG RIÊNG trên đơn hàng
--     (orders.charged_fee_vnd) chứ không phải hệ số nhân giấu trong giá -- xem mục 1, chỗ bỏ
--     token_unit_price vì cùng một lý do.
-- -----------------------------------------------------------------------------
ALTER TABLE subscription_plans RENAME COLUMN price_per_year TO price_vnd;

ALTER TABLE subscription_plans
    ADD COLUMN period_type character varying(255) DEFAULT 'DAY' NOT NULL;
ALTER TABLE subscription_plans ADD COLUMN period_count integer;
UPDATE subscription_plans SET period_count = validity_days WHERE period_count IS NULL;
ALTER TABLE subscription_plans ALTER COLUMN period_count SET NOT NULL;
ALTER TABLE subscription_plans DROP COLUMN validity_days;
ALTER TABLE subscription_plans DROP COLUMN service_fee_ratio;

ALTER TABLE subscription_plans
    ADD CONSTRAINT chk_subscription_plans_period_type_valid CHECK (((period_type)::text = ANY (ARRAY[
        ('DAY'::character varying)::text,
        ('MONTH'::character varying)::text,
        ('YEAR'::character varying)::text])));
ALTER TABLE subscription_plans
    ADD CONSTRAINT chk_subscription_plans_period_count_positive CHECK ((period_count > 0));
ALTER TABLE subscription_plans
    ADD CONSTRAINT chk_subscription_plans_max_time_per_attempt_min_positive CHECK (
        (max_time_per_attempt_min > 0));

-- @Version của Hibernate map sang Long; integer tràn ở 2.1 tỷ lần cập nhật thì xa, nhưng kiểu ở DB
-- lệch kiểu ở entity là thứ ddl-auto: validate sẽ chặn ngay lúc khởi động.
ALTER TABLE subscription_plans ALTER COLUMN version TYPE bigint;

-- tagline là câu mô tả bán hàng, 255 ký tự chật cho tiếng Việt có dấu.
ALTER TABLE subscription_plans ALTER COLUMN tagline TYPE character varying(2048);
UPDATE subscription_plans SET tagline = '' WHERE tagline IS NULL;
ALTER TABLE subscription_plans ALTER COLUMN tagline SET NOT NULL;

-- Gói ĐÃ published thì bị khoá sửa (xem UpdateSubscriptionPlanUseCase), nhưng bản nháp vẫn sửa được
-- và ai sửa/lúc nào là thông tin phải giữ.
ALTER TABLE subscription_plans ADD COLUMN updated_at timestamp(6) with time zone;
ALTER TABLE subscription_plans ADD COLUMN updated_by uuid;
UPDATE subscription_plans SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE subscription_plans ALTER COLUMN updated_at SET NOT NULL;


-- -----------------------------------------------------------------------------
-- 19. school_subscriptions: khoá ngoại gọi đúng tên, mốc thời gian thành thời điểm.
--
--     plan_id -> subscription_plan_id: trùng subscription_plan_quotas.subscription_plan_id, để cùng
--     một khoá ngoại không mang hai tên trong cùng một domain.
--
--     date -> timestamptz: gói hết hạn vào một THỜI ĐIỂM, không phải một ngày. Với `date`, câu hỏi
--     "hết hạn lúc 0h hay 24h theo múi giờ nào" không có chỗ nào trả lời, và job expireOverdue phải
--     so một Instant với một LocalDate -- so sai một chiều là cắt dịch vụ sớm cả ngày.
-- -----------------------------------------------------------------------------
ALTER TABLE school_subscriptions RENAME COLUMN plan_id TO subscription_plan_id;

ALTER TABLE school_subscriptions
    ALTER COLUMN start_date TYPE timestamp(6) with time zone
    USING start_date::timestamp with time zone;
ALTER TABLE school_subscriptions
    ALTER COLUMN end_date TYPE timestamp(6) with time zone
    USING end_date::timestamp with time zone;

ALTER TABLE school_subscriptions
    ALTER COLUMN suspended_reason TYPE character varying(255);


-- -----------------------------------------------------------------------------
-- 20. Đổi nốt các bảng còn ở dạng số ít sang số nhiều.
--
--     Đặt ở CUỐI file là có chủ đích: mọi DDL phía trên (index, khóa ngoại, CHECK ở V1 lẫn V2, kể cả
--     fk_school_balance_entries_practice_session ở mục 16) đều chạy khi tên cũ còn hiệu lực. Postgres
--     tự chuyển index/khóa ngoại/ràng buộc theo bảng khi RENAME, nên không cần dựng lại cái nào --
--     chỉ TÊN của chúng là giữ nguyên dạng cũ, và đó là thứ Hibernate không soi.
--
--     framework_criteria KHÔNG nằm trong danh sách: 'criteria' đã là số nhiều của 'criterion'.
-- -----------------------------------------------------------------------------
ALTER TABLE dimension_interest_score RENAME TO dimension_interest_scores;
ALTER TABLE dimension_interest_scores RENAME CONSTRAINT dimension_interest_score_pkey TO dimension_interest_scores_pkey;
ALTER TABLE financial_event RENAME TO financial_events;
ALTER TABLE financial_events RENAME CONSTRAINT financial_event_pkey TO financial_events_pkey;
ALTER TABLE interest_dimension RENAME TO interest_dimensions;
ALTER TABLE interest_dimensions RENAME CONSTRAINT interest_dimension_pkey TO interest_dimensions_pkey;
ALTER TABLE interest_quiz_item RENAME TO interest_quiz_items;
ALTER TABLE interest_quiz_items RENAME CONSTRAINT interest_quiz_item_pkey TO interest_quiz_items_pkey;
ALTER TABLE learner_profile RENAME TO learner_profiles;
ALTER TABLE learner_profiles RENAME CONSTRAINT learner_profile_pkey TO learner_profiles_pkey;
ALTER TABLE practice_criterion_score RENAME TO practice_criterion_scores;
ALTER TABLE practice_criterion_scores RENAME CONSTRAINT practice_criterion_score_pkey TO practice_criterion_scores_pkey;
ALTER TABLE practice_item_evaluation RENAME TO practice_item_evaluations;
ALTER TABLE practice_item_evaluations RENAME CONSTRAINT practice_item_evaluation_pkey TO practice_item_evaluations_pkey;
ALTER TABLE practice_item_response RENAME TO practice_item_responses;
ALTER TABLE practice_item_responses RENAME CONSTRAINT practice_item_response_pkey TO practice_item_responses_pkey;
ALTER TABLE practice_paper_item RENAME TO practice_paper_items;
ALTER TABLE practice_paper_items RENAME CONSTRAINT practice_paper_item_pkey TO practice_paper_items_pkey;
ALTER TABLE practice_paper RENAME TO practice_papers;
ALTER TABLE practice_papers RENAME CONSTRAINT practice_paper_pkey TO practice_papers_pkey;
ALTER TABLE practice_question RENAME TO practice_questions;
ALTER TABLE practice_questions RENAME CONSTRAINT practice_question_pkey TO practice_questions_pkey;
ALTER TABLE practice_response_turn RENAME TO practice_response_turns;
ALTER TABLE practice_response_turns RENAME CONSTRAINT practice_response_turn_pkey TO practice_response_turns_pkey;
ALTER TABLE practice_session RENAME TO practice_sessions;
ALTER TABLE practice_sessions RENAME CONSTRAINT practice_session_pkey TO practice_sessions_pkey;
ALTER TABLE practice_topic RENAME TO practice_topics;
ALTER TABLE practice_topics RENAME CONSTRAINT practice_topic_pkey TO practice_topics_pkey;
ALTER TABLE saved_topic RENAME TO saved_topics;
ALTER TABLE saved_topics RENAME CONSTRAINT saved_topic_pkey TO saved_topics_pkey;
ALTER TABLE student_question_exposure RENAME TO student_question_exposures;
ALTER TABLE student_question_exposures RENAME CONSTRAINT student_question_exposure_pkey TO student_question_exposures_pkey;
ALTER TABLE token_usage_event RENAME TO token_usage_events;
ALTER TABLE token_usage_events RENAME CONSTRAINT token_usage_event_pkey TO token_usage_events_pkey;
ALTER TABLE topic_interest_event RENAME TO topic_interest_events;
ALTER TABLE topic_interest_events RENAME CONSTRAINT topic_interest_event_pkey TO topic_interest_events_pkey;
ALTER TABLE topic_interest_score RENAME TO topic_interest_scores;
ALTER TABLE topic_interest_scores RENAME CONSTRAINT topic_interest_score_pkey TO topic_interest_scores_pkey;
ALTER TABLE topic_suggestion RENAME TO topic_suggestions;
ALTER TABLE topic_suggestions RENAME CONSTRAINT topic_suggestion_pkey TO topic_suggestions_pkey;
ALTER TABLE turn_correction RENAME TO turn_corrections;
ALTER TABLE turn_corrections RENAME CONSTRAINT turn_correction_pkey TO turn_corrections_pkey;


-- -----------------------------------------------------------------------------
-- 21. ai_usage_records.charged_at -- đánh dấu dòng chi phí NÀO đã được trừ vào ví.
--
--     Trước đây CompleteExamSessionGradingUseCase trừ SUM(cost_vnd) của cả phiên, mỗi lần chấm xong.
--     Nhưng một phiên được phép chấm LẠI: UpdateExamSessionStatusUseCase cho GRADED -> GRADING (và
--     GRADING_FAILED -> GRADING). Lần chấm thứ hai sinh thêm chi phí thật, nên nó PHẢI được trừ --
--     nhưng tổng lúc đó đã bao gồm cả chi phí lần chấm đầu, tức phần đó bị thu tiền hai lần.
--
--     Vì vậy KHÔNG dùng ràng buộc duy nhất trên school_balance_entries(exam_session_id) để chống
--     trùng: nó sẽ chặn đúng khoản trừ hợp lệ của lần chấm lại. Cái cần định danh là TỪNG DÒNG CHI
--     PHÍ, không phải phiên thi -- một dòng chỉ được thu tiền đúng một lần, còn một phiên thì thu bao
--     nhiêu lần cũng được, miễn mỗi lần chỉ thu phần chưa thu.
--
--     Cách dùng (xem CompleteExamSessionGradingUseCase): UPDATE ... SET charged_at = :now WHERE
--     charged_at IS NULL để GIÀNH các dòng, rồi SUM đúng những dòng vừa mang mốc :now. Giành trước --
--     cộng sau, nên một dòng usage do Kafka chèn vào giữa chừng vẫn còn charged_at NULL và sẽ được
--     lần chấm sau thu, thay vì bị đóng dấu đã thu mà không thu.
--
--     NULL = chưa thu, nên cột phải nullable -- và đó cũng là backfill đúng cho dữ liệu cũ: bảng chưa
--     có dữ liệu thật (xem cảnh báo đầu file), còn nếu có thì để NULL nghĩa là "sẽ thu ở lần chấm tới"
--     chứ không âm thầm bỏ qua một khoản tiền.
-- -----------------------------------------------------------------------------
ALTER TABLE ai_usage_records ADD COLUMN charged_at timestamp(6) with time zone;

-- Partial index: câu chạy nóng là "còn dòng nào của phiên này chưa thu không", và các dòng đã thu chỉ
-- lớn dần lên mãi mãi. Đánh chỉ mục lên phần chưa thu giữ index nhỏ bằng đúng phần việc còn lại.
CREATE INDEX idx_ai_usage_records_uncharged
    ON ai_usage_records USING btree (exam_session_id) WHERE charged_at IS NULL;

-- Đọc lại theo mốc vừa giành, ngay sau câu UPDATE ở trên.
CREATE INDEX idx_ai_usage_records_charged_at
    ON ai_usage_records USING btree (exam_session_id, charged_at);


-- -----------------------------------------------------------------------------
-- 22. Đổi tên cột cho khớp entity: 3 bảng quota + 2 cột interest_dimension.
--
--     Mục 1-2 mới đổi TÊN BẢNG của nhóm quota, cột thì vẫn mang tên từ thời hạn mức đếm bằng TOKEN.
--     Đây là phần còn lại đó. Native query không được Hibernate soi lúc khởi động, nên tới trước bước
--     này thì entity và schema đang nói hai thứ khác nhau mà không chỗ nào báo.
--
--     *_quantity -> *_amount_vnd: "quantity" là tàn dư của thời đếm token. Giờ mọi cột này là TIỀN,
--     cùng đơn vị và cùng cách gọi với school_balances.balance_vnd và school_debt_events.used_amount_vnd
--     (mục 15) -- cùng một đại lượng thì cùng một tên, nếu không thì mỗi lần đọc lại phải nhớ cột nào
--     đang đếm cái gì.
--
--     subscription_id -> school_subscription_id: gọi đúng tên bảng nó trỏ tới (school_subscriptions,
--     mục 13), đồng bộ với subscription_plan_quotas.subscription_plan_id ngay bên dưới và với
--     school_subscriptions.subscription_plan_id (mục 19).
--
--     KHÔNG đổi tên các ràng buộc NOT NULL đi kèm (subscription_quota_total_allocated_not_null...):
--     không có gì tham chiếu tới chúng -- Hibernate validate chỉ soi bảng/cột/kiểu, còn V2 chỉ gọi
--     đích danh các ràng buộc CHECK và pkey. Đổi thì tên tự nhiên mới ("school_subscription_quota_
--     records_school_subscription_id_not_null" = 65 ký tự) vượt giới hạn 63 ký tự của Postgres và bị
--     cắt âm thầm, tức viết một đằng ra một nẻo -- xem NOTICE ở mục 3.
-- -----------------------------------------------------------------------------
ALTER TABLE school_subscription_quota_records RENAME COLUMN subscription_id TO school_subscription_id;
ALTER TABLE school_subscription_quota_records RENAME COLUMN total_allocated TO total_allocated_amount_vnd;
ALTER TABLE school_subscription_quota_records RENAME COLUMN used_quantity TO used_amount_vnd;

ALTER TABLE school_subscription_quota_user_allocations RENAME COLUMN subscription_id TO school_subscription_id;
ALTER TABLE school_subscription_quota_user_allocations RENAME COLUMN allocated_quantity TO allocated_amount_vnd;
ALTER TABLE school_subscription_quota_user_allocations RENAME COLUMN used_quantity TO used_amount_vnd;

ALTER TABLE subscription_plan_quotas RENAME COLUMN plan_id TO subscription_plan_id;

-- Hai cột dưới đây KHÔNG thuộc refactor thanh toán: entity đã để số nhiều từ trước (kèm cả
-- @Index(columnList = "interest_dimensions, active")), còn V1 tạo ra dạng số ít. Sai lệch này có sẵn từ
-- phần personalization, chỉ là chưa ai chạy ddl-auto=validate trên một DB sạch nên chưa lộ. Đổi luôn ở
-- đây vì cùng một loại lỗi và cùng một lần sửa; index/ràng buộc tự đi theo cột.
ALTER TABLE practice_topics RENAME COLUMN interest_dimension TO interest_dimensions;
ALTER TABLE topic_suggestions RENAME COLUMN interest_dimension TO interest_dimensions;
