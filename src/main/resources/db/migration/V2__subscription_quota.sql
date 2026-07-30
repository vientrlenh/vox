create table subscription_quota_user_allocations (
    allocated_quantity integer not null,
    used_quantity integer not null,
    id UUID DEFAULT uuidv7() not null,
    subscription_id uuid not null,
    user_id uuid not null,
    quota_type varchar(20) not null
        constraint chk_subscription_quota_user_allocations_quota_type_valid
        check (quota_type IN ('CLASS_TEST', 'PRACTICE')),
    primary key (id),
    constraint uk_subscription_quota_user_allocations_subscription_quota_user
        unique (subscription_id, quota_type, user_id)
);
