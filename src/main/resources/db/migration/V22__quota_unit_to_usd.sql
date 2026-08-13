-- Đổi đơn vị quota từ "giây" (integer) sang "USD" (numeric(12,6)) trên toàn bộ
-- module subscription/token.
--
-- Lý do: quota bán cho trường trước đây quy đổi theo giây thời lượng bài thi được
-- chấm, không phản ánh chi phí thật trả cho nhà cung cấp AI (LLM theo token, STT/
-- TTS/avatar theo duration -- xem ai_usage_record ở V14). Đổi sang USD để quota
-- gắn trực tiếp với chi phí thật, ConsumeQuotaUseCase trừ theo cost_usd tổng hợp
-- từ ai_usage_record thay vì tổng số giây câu trả lời.
--
-- Môi trường hiện tại chưa có dữ liệu purchase/quota thật (xác nhận với người yêu
-- cầu thay đổi), nên ALTER COLUMN TYPE trực tiếp mà không cần logic quy đổi giá trị
-- cũ -- số giây cũ (nếu có) không còn ý nghĩa nghiệp vụ sau khi đổi đơn vị.
--
-- token_unit_price (plan_quota) và unit_price_snapshot/subtotal (token_purchase_item)
-- KHÔNG đổi: vẫn là giá VND, chỉ đổi ý nghĩa từ "giá mỗi giây" sang "giá mỗi $1
-- quota" -- không cần đổi kiểu cột.

alter table plan_quota
    alter column included_quantity type numeric(12,6) using included_quantity::numeric(12,6);

alter table subscription_quota
    alter column total_allocated type numeric(12,6) using total_allocated::numeric(12,6),
    alter column used_quantity type numeric(12,6) using used_quantity::numeric(12,6);

alter table subscription_quota_user_allocations
    alter column allocated_quantity type numeric(12,6) using allocated_quantity::numeric(12,6),
    alter column used_quantity type numeric(12,6) using used_quantity::numeric(12,6);

alter table token_purchase_item
    alter column quantity type numeric(12,6) using quantity::numeric(12,6);

alter table token_usage_event
    alter column tokens_consumed type numeric(12,6) using tokens_consumed::numeric(12,6);