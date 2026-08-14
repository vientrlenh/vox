-- numeric(12,6) chỉ chứa được tối đa 999,999.999999 -- không đủ cho trường/sở lớn khi cộng dồn
-- total_allocated qua nhiều lần mua thêm token theo thời gian (không chỉ lúc admin đặt
-- included_quantity cho 1 gói). Nới lên numeric(18,6), đủ headroom tới hàng nghìn tỷ USD.
alter table plan_quota
    alter column included_quantity type numeric(18,6);

alter table subscription_quota
    alter column total_allocated type numeric(18,6),
    alter column used_quantity type numeric(18,6);

alter table subscription_quota_user_allocations
    alter column allocated_quantity type numeric(18,6),
    alter column used_quantity type numeric(18,6);

alter table token_purchase_item
    alter column quantity type numeric(18,6);

alter table token_usage_event
    alter column tokens_consumed type numeric(18,6);
