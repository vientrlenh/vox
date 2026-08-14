-- Sổ audit "nguyên nhân nợ hạn mức AI" -- mỗi dòng là đúng 1 bucket quota (GRADING/CLASS_TEST) của 1
-- trường vừa đổi trạng thái (vượt hạn mức lần đầu / vượt trần cảnh báo / hết nợ), kèm session/số USD
-- đã gây ra transition đó -- xem SchoolDebtEvent (domain model).
create table school_debt_event (
    id UUID DEFAULT uuidv7() not null,
    school_id uuid not null,
    subscription_id uuid not null,
    event_type varchar(20) not null
        constraint chk_school_debt_event_event_type_valid
        check (event_type IN ('LOCKED', 'CAP_EXCEEDED', 'CLEARED')),
    quota_type varchar(20) not null
        constraint chk_school_debt_event_quota_type_valid
        check (quota_type IN ('GRADING', 'CLASS_TEST', 'PRACTICE')),
    trigger_exam_session_id uuid,
    trigger_amount_usd numeric(18,6),
    total_allocated_usd numeric(18,6) not null,
    used_quantity_usd numeric(18,6) not null,
    overage_usd numeric(18,6) not null,
    occurred_at timestamp(6) with time zone not null,
    primary key (id)
);

create index idx_school_debt_event_subscription on school_debt_event (subscription_id, occurred_at);
create index idx_school_debt_event_school on school_debt_event (school_id, occurred_at);
