-- Lịch sử mỗi lần QuotaPricingCalibrationJob tự tính lại estimatedCostPerExamSecondUsd
-- từ dữ liệu ai_usage_record + exam_item_responses thật (xem QuotaPricingCalibrationService),
-- thay cho việc phải tự tay đoán/sửa VOX_QUOTA_ESTIMATED_COST_PER_EXAM_SECOND_USD trong .env.
--
-- Chỉ insert row khi job tính THÀNH CÔNG (đủ mẫu tối thiểu) -- không ghi row khi bị skip vì
-- thiếu dữ liệu, để phía đọc (QuotaPricingService) luôn chỉ cần lấy đúng 1 row mới nhất theo
-- computed_at, không phải lọc thêm điều kiện.
--
-- raw_rate = tính thẳng từ dữ liệu (total_cost_usd / total_answered_seconds), applied_rate =
-- số THẬT SỰ dùng cho guard sau khi làm mượt (không đổi quá maxChangeRatio so với lần trước) và
-- chặn biên an toàn (min/max bound) -- tách riêng 2 cột để sau này audit được smoothing có kéo
-- lệch nhiều so với số thô hay không.
create table quota_pricing_calibration (
    id                            UUID DEFAULT uuidv7() not null,
    computed_at                   timestamp(6) with time zone not null,
    window_days                   integer not null,
    session_count                 integer not null,
    total_cost_usd                numeric(12,6) not null,
    total_answered_seconds        bigint not null,
    raw_rate_usd_per_second       numeric(12,6) not null,
    applied_rate_usd_per_second   numeric(12,6) not null,
    note                          text,
    primary key (id)
);

create index idx_quota_pricing_calibration_computed_at on quota_pricing_calibration (computed_at desc);
