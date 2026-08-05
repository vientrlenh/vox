-- Chuẩn bị cho việc chạy song song nhiều cổng thanh toán (PayOS + SePay). Trước đây invoice
-- chỉ có payos_order_code, tức là bảng ngầm giả định mọi hóa đơn đều đi qua PayOS: không có
-- chỗ nào ghi lại hóa đơn này thuộc cổng nào, nên job đối soát và luồng sync trạng thái
-- không thể biết phải hỏi cổng nào về hóa đơn nào.

-- 1) Cổng thanh toán của hóa đơn -----------------------------------------------------------
-- Giá trị khớp enum PaymentMethod (PAYOS / SEPAY / MANUAL). MANUAL dành cho hóa đơn đối soát
-- tay, không đi qua cổng nào.
alter table invoice
    add column payment_provider varchar(20);

-- 2) Mã đơn phía cổng ----------------------------------------------------------------------
-- Kiểu chuỗi chứ không phải bigint: orderCode dạng số là quy ước riêng của PayOS, còn SePay
-- Payment Gateway định danh đơn bằng order_invoice_number dạng chuỗi. Để bigint thì mọi
-- provider mới đều phải bẻ mã của mình về số.
alter table invoice
    add column provider_order_ref varchar(100);

-- Chuyển dữ liệu sẵn có sang cặp cột mới trước khi bỏ cột cũ.
update invoice
   set payment_provider   = 'PAYOS',
       provider_order_ref = payos_order_code::text
 where payos_order_code is not null;

-- Hóa đơn không có payos_order_code (không nên tồn tại, nhưng dữ liệu dev có thể có) coi như
-- đối soát tay, để câu set not null bên dưới không vỡ.
update invoice
   set payment_provider = 'MANUAL'
 where payment_provider is null;

alter table invoice
    alter column payment_provider set not null;

alter table invoice
    add constraint chk_invoice_payment_provider_valid
    check (payment_provider in ('PAYOS', 'SEPAY', 'MANUAL'));

-- Thay cho ràng buộc unique cũ trên payos_order_code. Mã đơn chỉ cần duy nhất TRONG PHẠM VI
-- một cổng — hai cổng khác nhau hoàn toàn có thể sinh ra cùng một chuỗi mã. provider_order_ref
-- để null cho hóa đơn MANUAL, và Postgres coi các NULL là khác nhau nên nhiều hóa đơn MANUAL
-- cùng tồn tại được.
create unique index idx_invoice_provider_order_ref
    on invoice (payment_provider, provider_order_ref);

-- Bỏ cột cũ (kéo theo ràng buộc unique ngầm invoice_payos_order_code_key).
alter table invoice
    drop column payos_order_code;

-- 3) Nới ràng buộc phương thức thanh toán của financial_event ------------------------------
-- Không nới thì mọi giao dịch SePay sẽ vỡ ngay tại bước ghi sổ, sau khi tiền đã thu.
alter table financial_event
    drop constraint chk_financial_event_payment_method_valid;

alter table financial_event
    add constraint chk_financial_event_payment_method_valid
    check (payment_method in ('PAYOS', 'SEPAY', 'MANUAL'));
