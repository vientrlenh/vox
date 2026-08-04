-- Cho phép admin liên kết 1 gói đã ARCHIVED tới gói kế nhiệm (replaced_by_plan_id).
--
-- Ca dùng: gói school_subscription_plan hết hạn giữa chừng, trong lúc đó admin tạo gói
-- mới cùng giá nhưng nhiều tính năng hơn rồi archive gói cũ kèm link sang gói mới. Khi
-- trường gia hạn (qua PayOS), hệ thống sẽ tự chuyển sang gói kế nhiệm thay vì tiếp tục
-- gia hạn trên gói đã archived.
alter table subscription_plan
    add column replaced_by_plan_id uuid;

alter table subscription_plan
    add constraint fk_subscription_plan_replaced_by
        foreign key (replaced_by_plan_id) references subscription_plan (id);