-- popular giờ tính tự động (gói ACTIVE có nhiều trường đang đăng ký nhất), không còn là cờ
-- admin tự set khi tạo/sửa gói nữa — bỏ cột lưu trữ để tránh dữ liệu chết/lệch với giá trị tính
-- live ở JpaSubscriptionPlanQueryRepository.
alter table subscription_plan
    drop column is_popular;
