-- Gói không còn giới hạn theo số học sinh trường -- business đã chuyển hẳn sang mô hình quota
-- USD (GRADING/CLASS_TEST/PRACTICE). Giới hạn cũ enforce rời rạc và có lỗ hổng ở renewal
-- (RenewSubscriptionUseCase không check), nên bỏ hẳn thay vì vá.
alter table subscription_plan
    drop column max_student_count;
