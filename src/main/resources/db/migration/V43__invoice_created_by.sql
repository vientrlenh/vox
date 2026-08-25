-- Lưu lại người thật sự khởi tạo đơn (bấm "Mua gói"/"Gia hạn"/"Mua thêm token"), để
-- InvoiceSettlementService (chạy khi webhook PayOS/SePay báo PAID, không có user đăng nhập) có thể
-- gán actor_id đúng cho FinancialEvent thay vì luôn null.
ALTER TABLE invoice ADD COLUMN created_by uuid;
