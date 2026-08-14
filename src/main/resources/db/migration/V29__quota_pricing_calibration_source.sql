-- Tách rate calibrate riêng cho PRACTICE khỏi EXAM (xem QuotaPricingCalibrationService).
-- PRACTICE dùng pipeline AI nhẹ hơn hẳn EXAM (evalGraph), nên guard PRACTICE cần rate riêng
-- thay vì mượn rate calibrate từ hành vi EXAM. Default 'EXAM' giữ đúng ý nghĩa mọi row lịch sử
-- hiện có (chỉ EXAM tồn tại trước migration này).
ALTER TABLE quota_pricing_calibration ADD COLUMN pricing_source VARCHAR(16) NOT NULL DEFAULT 'EXAM';

DROP INDEX idx_quota_pricing_calibration_computed_at;

CREATE INDEX idx_quota_pricing_calibration_source_computed_at
    ON quota_pricing_calibration (pricing_source, computed_at DESC);
