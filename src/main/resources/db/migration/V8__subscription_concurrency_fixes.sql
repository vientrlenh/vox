-- 1) Optimistic locking cho school_subscription — chống lost-update giữa các use case
-- load-mutate-save (vd CancelSubscriptionUseCase) và bulk UPDATE của SubscriptionExpiryJob
-- (chuyển ACTIVE quá hạn sang EXPIRED). Không có cột version thì save() sau có thể ghi đè
-- ngược lại EXPIRED thành ACTIVE bằng dữ liệu cũ đang cầm trong tay.
alter table school_subscription
    add column version bigint not null default 0;

-- 2) Chốt sẵn plan sẽ áp dụng khi renew ngay lúc tạo invoice/payment link — để lúc settle
-- (PayOSInvoiceSettlementService) dùng lại đúng plan đã báo giá, không resolve lại
-- PlanReplacementResolver một lần nữa. Nếu admin đổi replaced_by_plan_id giữa lúc tạo
-- invoice và lúc PayOS xác nhận thanh toán, trường sẽ không còn bị tính tiền theo 1 gói
-- nhưng lại được cấp quota theo gói khác.
alter table invoice
    add column resolved_plan_id uuid;
alter table invoice
    add constraint fk_invoice_resolved_plan foreign key (resolved_plan_id) references subscription_plan (id);
