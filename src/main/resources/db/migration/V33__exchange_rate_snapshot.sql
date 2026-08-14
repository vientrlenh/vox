-- Lịch sử mỗi lần ExchangeRateRefreshJob lấy tỷ giá USD->VND thật từ API bên ngoài (xem
-- ExchangeRateRefreshService), thay cho việc phải tự tay đoán/sửa VOX_QUOTA_USD_TO_VND_RATE
-- trong .env (QuotaSellingPriceProperties -- giờ chỉ còn là fallback tĩnh khi chưa có row nào).
--
-- Chỉ insert row khi fetch THÀNH CÔNG và giá trị nằm trong khoảng hợp lý (xem
-- ExchangeRateApiProperties.minRateBound/maxRateBound) -- không ghi row khi API lỗi hoặc trả số
-- bất thường, để phía đọc (QuotaPricingService) luôn chỉ cần lấy đúng 1 row mới nhất theo
-- fetched_at, không phải lọc thêm điều kiện.
create table exchange_rate_snapshot (
    id                UUID DEFAULT uuidv7() not null,
    fetched_at        timestamp(6) with time zone not null,
    usd_to_vnd_rate   numeric(12,4) not null,
    source            varchar(255) not null,
    primary key (id)
);

create index idx_exchange_rate_snapshot_fetched_at on exchange_rate_snapshot (fetched_at desc);
