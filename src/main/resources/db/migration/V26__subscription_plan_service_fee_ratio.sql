-- Phí dịch vụ (margin) trường tự chọn theo từng gói khi tạo/sửa (khác gói có thể khác % margin, vd
-- gói cao cấp margin cao hơn gói phổ thông) -- dùng để FE tự tính giá gợi ý tokenUnitPrice
-- (suggestedTokenUnitPriceVnd = usdToVndRate × (1 + serviceFeeRatio), usdToVndRate là config toàn hệ
-- thống lấy qua quotaPricing query), KHÔNG ảnh hưởng quota deduction/guard/ghi nợ.
alter table subscription_plan
    add column service_fee_ratio numeric(5,4) not null default 0.20;
