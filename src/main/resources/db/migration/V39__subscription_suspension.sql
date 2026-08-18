-- System Admin cưỡng chế đình chỉ (SUSPENDED) gói subscription của trường, kèm lý do bắt buộc --
-- khác "Hủy" (chỉ tắt gia hạn, vẫn ACTIVE tới hết endDate), đình chỉ có hiệu lực NGAY.

alter table school_subscription
    add column suspended_at timestamptz,
    add column suspended_reason text,
    add column suspended_by uuid;

alter table school_subscription
    drop constraint chk_school_subscription_status_valid;
alter table school_subscription
    add constraint chk_school_subscription_status_valid
    check (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED'));

alter table financial_event
    drop constraint chk_financial_event_event_type_valid;
alter table financial_event
    add constraint chk_financial_event_event_type_valid
    check (event_type IN ('SUB_PURCHASED', 'SUB_RENEWED', 'SUB_CANCELLED', 'SUB_UPGRADED',
        'TOKEN_PURCHASED', 'TOKEN_CONSUMED', 'REFUND_ISSUED', 'SUB_SUSPENDED', 'SUB_UNSUSPENDED'));
