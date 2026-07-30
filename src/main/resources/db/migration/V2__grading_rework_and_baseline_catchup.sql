-- Đưa schema từ V1__baseline.sql lên đúng metadata JPA hiện tại.
--
-- Phần lớn nội dung dưới đây do Hibernate sinh (schema-generation action=update chạy
-- trên một DB vừa apply V1), sau đó bổ sung tay những thứ Hibernate KHÔNG bao giờ phát:
-- DROP bảng/constraint đã bỏ, thay đổi CHECK constraint, và backfill cho các cột
-- NOT NULL thêm vào bảng đã có dữ liệu.
--
-- Ngoài delta của grading rework, file này còn vá hai chỗ V1 vốn đã lạc hậu so với
-- chính nhánh default: bảng subscription_quota_user_allocations và check constraint
-- chk_import_sessions_type_valid (thiếu SCHOOL_ROOM, RUBRIC_RESULT_BAND). Không vá thì
-- ddl-auto: validate chặn app khởi động.

-- ---------------------------------------------------------------------------
-- 1. exam_grading_assignments: một bảng cho cả bốn vòng chấm
-- ---------------------------------------------------------------------------

-- Thêm dạng NULL trước để bảng đang có dữ liệu vẫn alter được; siết NOT NULL sau backfill.
alter table if exists exam_grading_assignments add column if not exists active_result_id uuid;
alter table if exists exam_grading_assignments add column if not exists appeal_id uuid;
alter table if exists exam_grading_assignments add column if not exists deadline_at timestamp(6) with time zone;
alter table if exists exam_grading_assignments add column if not exists outcome varchar(20);
alter table if exists exam_grading_assignments add column if not exists reason TEXT;
alter table if exists exam_grading_assignments add column if not exists reminded_at timestamp(6) with time zone;
alter table if exists exam_grading_assignments add column if not exists round_type varchar(20);
alter table if exists exam_grading_assignments add column if not exists score_before numeric(5,2);
alter table if exists exam_grading_assignments add column if not exists version integer;

-- Mọi phân công có trước rework đều là vòng chấm lần đầu.
update exam_grading_assignments set round_type = 'INITIAL' where round_type is null;
update exam_grading_assignments set version = 0 where version is null;

-- Bất biến "mỗi bài tối đa một phân công đang mở" nằm ở unique index trên
-- active_result_id: bằng chính bài khi ASSIGNED, NULL khi COMPLETED (Postgres coi các
-- NULL là khác nhau nên unique index thường cho đúng ngữ nghĩa partial index).
update exam_grading_assignments
   set active_result_id = case when status = 'ASSIGNED' then candidate_result_id else null end
 where active_result_id is null;

alter table if exists exam_grading_assignments alter column round_type set not null;
alter table if exists exam_grading_assignments alter column version set not null;

alter table if exists exam_grading_assignments
    drop constraint if exists chk_exam_grading_assignments_round_type_valid;
alter table if exists exam_grading_assignments
    add constraint chk_exam_grading_assignments_round_type_valid
    check (round_type IN ('INITIAL', 'SPOT_CHECK', 'REMEDIATION', 'APPEAL'));

alter table if exists exam_grading_assignments
    drop constraint if exists chk_exam_grading_assignments_outcome_valid;
alter table if exists exam_grading_assignments
    add constraint chk_exam_grading_assignments_outcome_valid
    check (outcome IS NULL OR outcome IN ('UPHELD', 'REGRADED', 'INVALIDATED', 'CLEARED_INVALID', 'DECLINED'));

-- uq cũ trên candidate_result_id chặn bài đi qua vòng thứ hai — phải nhường chỗ cho
-- uq trên active_result_id. Vì uq cũ đảm bảo mỗi bài chỉ có một dòng, backfill ở trên
-- không thể sinh hai dòng ASSIGNED cùng bài, nên uq mới thêm được an toàn.
alter table if exists exam_grading_assignments drop constraint if exists uq_grading_assignment_result;
alter table if exists exam_grading_assignments drop constraint if exists uq_grading_assignment_active_result;
alter table if exists exam_grading_assignments
    add constraint uq_grading_assignment_active_result unique (active_result_id);

create index if not exists idx_grading_assignments_result on exam_grading_assignments (candidate_result_id);
create index if not exists idx_grading_assignments_appeal on exam_grading_assignments (appeal_id);
create index if not exists idx_grading_assignments_status_deadline on exam_grading_assignments (status, deadline_at);

-- ---------------------------------------------------------------------------
-- 2. exam_result_status_histories: dòng thời gian điểm của một bài
-- ---------------------------------------------------------------------------

create table if not exists exam_result_status_histories (
    id UUID DEFAULT uuidv7() not null,
    actor_id uuid,
    candidate_result_id uuid not null,
    created_at timestamp(6) with time zone not null,
    from_status varchar(30),
    reason TEXT,
    score_after numeric(5,2),
    score_before numeric(5,2),
    source varchar(30) not null
        constraint chk_exam_result_status_histories_source_valid
        check (source IN ('AI_EVALUATION', 'TEACHER_INITIAL', 'TEACHER_SPOT_CHECK', 'TEACHER_REMEDIATION',
                          'TEACHER_APPEAL', 'ADMIN_BULK_FINALIZE', 'EXAM_PUBLISH', 'SYSTEM')),
    to_status varchar(30) not null,
    primary key (id)
);

create index if not exists idx_result_status_histories_result_time
    on exam_result_status_histories (candidate_result_id, created_at);

-- ---------------------------------------------------------------------------
-- 3. exam_result_appeals: rút đơn + lý do phá lệ xung đột lợi ích
-- ---------------------------------------------------------------------------

alter table if exists exam_result_appeals add column if not exists reviewer_override_reason TEXT;
alter table if exists exam_result_appeals add column if not exists withdrawn_at timestamp(6) with time zone;

-- Rework bỏ bước admin đối chiếu (COMPARING): giáo viên nộp là đơn được công bố luôn.
-- Đơn đang treo ở COMPARING lùi về GRADING để còn xử lý được — KHÔNG đẩy lên PUBLISHED,
-- vì như vậy là công bố một kết quả chưa từng được công bố.
update exam_result_appeals set status = 'GRADING' where status = 'COMPARING';

alter table if exists exam_result_appeals drop constraint if exists chk_exam_result_appeals_status_valid;
alter table if exists exam_result_appeals
    add constraint chk_exam_result_appeals_status_valid
    check (status IN ('PENDING', 'APPROVED', 'GRADING', 'PUBLISHED', 'REJECTED', 'WITHDRAWN'));

-- ---------------------------------------------------------------------------
-- 4. Bỏ bảng phân công giám khảo phúc khảo nhiều người
-- ---------------------------------------------------------------------------
-- Rework chuyển phúc khảo về một người chấm trên chính exam_grading_assignments
-- (round_type = 'APPEAL'), nên hai bảng này không còn entity nào map tới. Schema không
-- có FK nào trỏ vào chúng.

drop table if exists exam_appeal_reviewer_items;
drop table if exists exam_appeal_reviewers;

-- ---------------------------------------------------------------------------
-- 5. Vá chỗ V1 lạc hậu so với default (không liên quan grading rework)
-- ---------------------------------------------------------------------------

create table if not exists subscription_quota_user_allocations (
    id UUID DEFAULT uuidv7() not null,
    allocated_quantity integer not null,
    quota_type varchar(20) not null
        constraint chk_subscription_quota_user_allocations_quota_type_valid
        check (quota_type IN ('CLASS_TEST', 'PRACTICE')),
    subscription_id uuid not null,
    used_quantity integer not null,
    user_id uuid not null,
    primary key (id)
);

alter table if exists subscription_quota_user_allocations
    drop constraint if exists uk_subscription_quota_user_allocations_subscription_quota_user;
alter table if exists subscription_quota_user_allocations
    add constraint uk_subscription_quota_user_allocations_subscription_quota_user
    unique (subscription_id, quota_type, user_id);

-- V1 thiếu SCHOOL_ROOM và RUBRIC_RESULT_BAND; RUBRIC_CRITERION_BAND đã bị bỏ ở 048c2295.
alter table if exists import_sessions drop constraint if exists chk_import_sessions_type_valid;
alter table if exists import_sessions
    add constraint chk_import_sessions_type_valid
    check (type IN ('USER', 'SCHOOL_CLASS', 'SCHOOL_CLASS_USER', 'QUESTION', 'SCHOOL_DIRECTORY',
                    'SCHOOL_GRADE_LEVEL', 'SCHOOL_GRADE', 'SCHOOL_ROOM', 'RUBRIC_VERSION',
                    'RUBRIC_CRITERION', 'RUBRIC_RESULT_BAND', 'ASSESSMENT_POLICY'));
