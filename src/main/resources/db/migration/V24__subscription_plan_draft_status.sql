-- Thêm trạng thái DRAFT cho subscription_plan: gói mới tạo ở DRAFT (chỉnh sửa/xóa cứng được),
-- publish sang ACTIVE mới cho phép bán, archive (xóa mềm) chỉ áp dụng cho ACTIVE -> ARCHIVED.
alter table subscription_plan
    drop constraint chk_subscription_plan_status_valid;

alter table subscription_plan
    add constraint chk_subscription_plan_status_valid
        check (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));
