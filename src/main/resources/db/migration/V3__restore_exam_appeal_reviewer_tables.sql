-- V2 xóa exam_appeal_reviewers / exam_appeal_reviewer_items với giả định code đã bỏ
-- entity ExamAppealReviewer/ExamAppealReviewerItem cho rework phúc khảo một người chấm.
-- Trên nhánh này, ExamAppealReviewerJpaEntity/ExamAppealReviewerItemJpaEntity vẫn còn map
-- tới hai bảng đó, nên ddl-auto: validate chặn app khởi động. Tạo lại đúng định nghĩa gốc
-- (V1__baseline.sql) cho tới khi phần grading rework thật sự xóa hai entity này.

create table if not exists exam_appeal_reviewers (
    id UUID DEFAULT uuidv7() not null,
    appeal_id uuid not null,
    reviewer_id uuid not null,
    status varchar(20) not null
        constraint chk_exam_appeal_reviewers_status_valid
        check (status IN ('ASSIGNED', 'SUBMITTED')),
    assigned_at timestamp(6) with time zone not null,
    assigned_by uuid,
    submitted_at timestamp(6) with time zone,
    primary key (id),
    constraint uq_appeal_reviewer unique (appeal_id, reviewer_id)
);

create table if not exists exam_appeal_reviewer_items (
    id UUID DEFAULT uuidv7() not null,
    appeal_reviewer_id uuid not null,
    appeal_item_id uuid not null,
    evaluation_id uuid not null,
    suggested_score numeric(5,2) not null,
    note varchar(2048),
    primary key (id),
    constraint uq_appeal_reviewer_item unique (appeal_reviewer_id, appeal_item_id)
);

create index if not exists idx_appeal_reviewers_reviewer_status on exam_appeal_reviewers (reviewer_id, status);
create index if not exists idx_appeal_reviewer_items_reviewer on exam_appeal_reviewer_items (appeal_reviewer_id);