-- BASELINE GỘP -- thay cho toàn bộ V1..V44 của giai đoạn phát triển.
--
-- Bốn mươi bốn migration cũ là lịch sử của một schema còn đang đổi từng tuần: nhiều bảng được
-- dựng lên rồi đổi tên, đổi kiểu, tách ra, bỏ đi ngay trong cùng giai đoạn. Đọc lại chuỗi đó
-- không cho biết schema hiện tại trông thế nào, mà mỗi lần dựng DB mới lại phải diễn lại toàn
-- bộ đường vòng. Nên gộp lại thành đúng một bản mô tả trạng thái cuối.
--
-- CÁCH DỰNG RA FILE NÀY: chạy lần lượt V1..V44 lên một postgres:18-alpine trắng, rồi
-- pg_dump --schema-only --no-owner --no-privileges --no-comments. Nghĩa là nội dung dưới đây
-- là schema THẬT mà chuỗi migration cũ tạo ra, không phải bản chép tay hay bản Hibernate suy
-- từ entity -- nên không thiếu ràng buộc nào chỉ tồn tại trong SQL.
--
-- HỆ QUẢ: KHÔNG có đường nâng cấp từ DB cũ. Mọi database đã từng chạy migration cũ đều phải
-- xóa và dựng lại -- flyway_schema_history của chúng ghi checksum của V1 cũ, nên Flyway sẽ từ
-- chối khởi động. Xem hướng dẫn xóa DB dev ở README.
--
-- YÊU CẦU: PostgreSQL 18 trở lên. uuidv7() dùng làm default cho hầu hết khóa chính là hàm dựng
-- sẵn của PG18, không phải extension -- bản Postgres cũ hơn sẽ lỗi ngay câu CREATE TABLE đầu tiên.
--
-- DỮ LIỆU SEED KHÔNG NẰM Ở ĐÂY: khối lớp mặc định, khung năng lực KNLNNVN, bộ tiêu chí mẫu,
-- trần bậc theo khối và chính sách chấm mẫu đều do các ApplicationRunner trong
-- com.sep.vox.infrastructure.initializer dựng lúc khởi động. Migration chỉ lo cấu trúc.

CREATE TABLE ai_usage_record (
    id uuid DEFAULT uuidv7() NOT NULL,
    exam_session_id uuid NOT NULL,
    turn_id uuid NOT NULL,
    usage_event_id uuid NOT NULL,
    usage_type character varying(20) NOT NULL,
    provider character varying(50) NOT NULL,
    model_name character varying(100),
    input_tokens integer,
    output_tokens integer,
    cache_creation_input_tokens integer,
    cache_read_input_tokens integer,
    duration_ms bigint,
    unit_price_json text NOT NULL,
    cost_usd numeric(12,6) NOT NULL,
    occurred_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT chk_ai_usage_record_usage_type_valid CHECK (((usage_type)::text = ANY (ARRAY[('LLM_TOKEN'::character varying)::text, ('DURATION'::character varying)::text])))
);

CREATE TABLE assessment_policies (
    passing_score numeric(6,2),
    version integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) with time zone NOT NULL,
    effective_to timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    framework_version_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    language_id uuid NOT NULL,
    rubric_version_id uuid NOT NULL,
    school_class_id uuid,
    school_grade_id uuid,
    grade_level_id uuid,
    school_id uuid,
    target_framework_band_id uuid NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    strictness character varying(20) NOT NULL,
    CONSTRAINT chk_assessment_policies_effective_range_valid CHECK (((effective_to IS NULL) OR (effective_from <= effective_to))),
    CONSTRAINT chk_assessment_policies_passing_score_non_negative CHECK (((passing_score IS NULL) OR (passing_score >= (0)::numeric))),
    CONSTRAINT chk_assessment_policies_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text, ('ARCHIVED'::character varying)::text]))),
    CONSTRAINT chk_assessment_policies_strictness_valid CHECK (((strictness)::text = ANY (ARRAY[('LENIENT'::character varying)::text, ('STANDARD'::character varying)::text, ('STRICT'::character varying)::text]))),
    CONSTRAINT chk_assessment_policies_version_positive CHECK ((version > 0))
);

CREATE TABLE device_sessions (
    revoked_at timestamp(6) with time zone,
    id uuid DEFAULT uuidv7() NOT NULL,
    user_id uuid NOT NULL,
    platform character varying(20) NOT NULL,
    device_id character varying(255) NOT NULL,
    device_name character varying(255) NOT NULL,
    ip_address character varying(255) NOT NULL,
    user_agent character varying(255),
    CONSTRAINT chk_device_sessions_platform_valid CHECK (((platform)::text = ANY (ARRAY[('WEB'::character varying)::text, ('ANDROID'::character varying)::text, ('IOS'::character varying)::text, ('DESKTOP'::character varying)::text])))
);

CREATE TABLE dimension_interest_score (
    id uuid DEFAULT uuidv7() NOT NULL,
    learner_profile_id uuid NOT NULL,
    dimension character varying(32) NOT NULL,
    score numeric(5,4) NOT NULL,
    baseline_score numeric(5,4)
);

CREATE TABLE exam_blueprint_sections (
    section_order integer NOT NULL,
    section_time_limits_seconds integer,
    section_weight numeric(3,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    blueprint_version_id uuid NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    instruction character varying(512),
    title character varying(255) NOT NULL
);

CREATE TABLE exam_blueprint_slots (
    prep_time_seconds_override integer,
    response_time_seconds_override integer,
    slot_order integer NOT NULL,
    weight numeric(3,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    blueprint_version_id uuid,
    created_by uuid,
    fixed_question_id uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    section_id uuid,
    updated_by uuid,
    slot_type character varying(20) NOT NULL,
    selection_spec text,
    CONSTRAINT chk_exam_blueprint_slots_type_valid CHECK (((slot_type)::text = ANY (ARRAY[('FIXED'::character varying)::text, ('SELECTION'::character varying)::text])))
);

CREATE TABLE exam_blueprint_versions (
    total_time_limit_seconds integer,
    version integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) with time zone NOT NULL,
    effective_to timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    blue_print_id uuid NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(2048),
    CONSTRAINT chk_exam_blueprint_versions_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text, ('ARCHIVED'::character varying)::text]))),
    CONSTRAINT chk_exam_blueprint_versions_total_time_limit_seconds_valid CHECK ((total_time_limit_seconds > 0))
);

CREATE TABLE exam_blueprints (
    is_active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    language_id uuid NOT NULL,
    grade_level_id uuid,
    school_id uuid,
    updated_by uuid,
    code character varying(100) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL
);

CREATE TABLE exam_candidate_results (
    policy_version integer NOT NULL,
    total_score numeric(5,2),
    created_at timestamp(6) with time zone NOT NULL,
    finalized_at timestamp(6) with time zone,
    released_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    assessment_policy_id uuid NOT NULL,
    candidate_id uuid NOT NULL,
    created_by uuid,
    exam_id uuid NOT NULL,
    framework_version_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    rubric_result_band_id uuid,
    rubric_version_id uuid NOT NULL,
    session_id uuid NOT NULL,
    target_framework_band_id uuid NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    CONSTRAINT chk_exam_candidate_results_status_valid CHECK (((status)::text = ANY (ARRAY[('PENDING_REVIEW'::character varying)::text, ('RELEASED'::character varying)::text, ('APPEALED'::character varying)::text, ('RE_GRADING'::character varying)::text, ('FINAL'::character varying)::text, ('INVALID'::character varying)::text, ('RETAKE_REQUIRED'::character varying)::text, ('PASSED'::character varying)::text, ('FAILED'::character varying)::text])))
);

CREATE TABLE exam_candidates (
    assigned_at timestamp(6) with time zone NOT NULL,
    blocked_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    assigned_by uuid,
    assigned_paper_id uuid,
    exam_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    schedule_id uuid,
    student_id uuid NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    CONSTRAINT chk_exam_candidates_status_valid CHECK (((status)::text = ANY (ARRAY[('ASSIGNED'::character varying)::text, ('ATTENDED'::character varying)::text, ('ABSENT'::character varying)::text, ('COMPLETED'::character varying)::text, ('EXEMPTED'::character varying)::text, ('CANCELLED'::character varying)::text])))
);

CREATE TABLE exam_grading_assignments (
    assigned_at timestamp(6) with time zone NOT NULL,
    completed_at timestamp(6) with time zone,
    assigned_by uuid,
    candidate_result_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    teacher_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    active_result_id uuid,
    appeal_id uuid,
    deadline_at timestamp(6) with time zone,
    outcome character varying(20),
    reason text,
    reminded_at timestamp(6) with time zone,
    round_type character varying(20) NOT NULL,
    score_before numeric(5,2),
    CONSTRAINT chk_exam_grading_assignments_outcome_valid CHECK (((outcome IS NULL) OR ((outcome)::text = ANY (ARRAY[('UPHELD'::character varying)::text, ('REGRADED'::character varying)::text, ('INVALIDATED'::character varying)::text, ('CLEARED_INVALID'::character varying)::text, ('DECLINED'::character varying)::text])))),
    CONSTRAINT chk_exam_grading_assignments_round_type_valid CHECK (((round_type)::text = ANY (ARRAY[('INITIAL'::character varying)::text, ('SPOT_CHECK'::character varying)::text, ('REMEDIATION'::character varying)::text, ('APPEAL'::character varying)::text]))),
    CONSTRAINT chk_exam_grading_assignments_status_valid CHECK (((status)::text = ANY (ARRAY[('ASSIGNED'::character varying)::text, ('COMPLETED'::character varying)::text])))
);

CREATE TABLE exam_item_criterion_scores (
    final_score numeric(5,2) NOT NULL,
    raw_score numeric(5,2) NOT NULL,
    evaluation_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    rubric_criterion_id uuid NOT NULL,
    rationale text,
    matched_band_code character varying(64)
);

CREATE TABLE exam_item_evaluation_turns (
    asr_confidence double precision,
    duration_seconds integer,
    turn_order integer NOT NULL,
    word_count integer NOT NULL,
    evaluation_id uuid NOT NULL,
    id uuid NOT NULL,
    turn_type character varying(20) NOT NULL,
    audio_url text NOT NULL,
    prompt_text text,
    pronunciation_overall text,
    transcript text NOT NULL,
    word_feedback text,
    CONSTRAINT chk_exam_item_evaluation_turns_turn_type_valid CHECK (((turn_type)::text = ANY (ARRAY[('MAIN'::character varying)::text, ('FOLLOWUP'::character varying)::text])))
);

CREATE TABLE exam_item_evaluations (
    item_score numeric(5,2) NOT NULL,
    marked_invalid boolean NOT NULL,
    overall_confidence numeric(3,2),
    raw_item_score numeric(5,2) NOT NULL,
    requires_human_review boolean NOT NULL,
    requires_retake boolean NOT NULL,
    sample_count integer,
    evaluated_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    paper_item_id uuid NOT NULL,
    response_id uuid NOT NULL,
    reviewer_id uuid,
    status character varying(20) NOT NULL,
    graded_by_model character varying(100) NOT NULL,
    prompt_version character varying(100),
    review_reason_code character varying(512),
    engine_type character varying(255) NOT NULL,
    feedback_summary text,
    signals text,
    suggestions text,
    validity_json text,
    CONSTRAINT chk_exam_item_evaluations_engine_type_valid CHECK (((engine_type)::text = ANY (ARRAY[('AI_SINGLE'::character varying)::text, ('AI_ENSEMBLE'::character varying)::text, ('HUMAN'::character varying)::text]))),
    CONSTRAINT chk_exam_item_evaluations_status_valid CHECK (((status)::text = ANY (ARRAY[('AUTO_GRADED'::character varying)::text, ('UNDER_REVIEW'::character varying)::text, ('FINALIZED'::character varying)::text, ('SUPERSEDED'::character varying)::text])))
);

CREATE TABLE exam_item_response_turns (
    duration_seconds integer,
    turn_order integer NOT NULL,
    word_count integer,
    answered_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    exam_item_response_id uuid NOT NULL,
    id uuid NOT NULL,
    turn_type character varying(20),
    audio_url text,
    prompt_text text,
    transcript text,
    CONSTRAINT chk_exam_item_response_turns_turn_type_valid CHECK (((turn_type)::text = ANY (ARRAY[('MAIN'::character varying)::text, ('FOLLOWUP'::character varying)::text])))
);

CREATE TABLE exam_item_responses (
    duration_seconds integer,
    submitted_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    paper_item_id uuid,
    session_id uuid NOT NULL,
    audio_url character varying(4096),
    termination_reason text,
    transcript text
);

CREATE TABLE exam_item_rule_hits (
    applied_order integer NOT NULL,
    observed_value numeric(3,2) NOT NULL,
    threshold numeric(3,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    evaluation_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    scoring_rule_id uuid NOT NULL,
    severity character varying(20) NOT NULL,
    effect_summary character varying(512) NOT NULL,
    action_type text NOT NULL,
    condition_type text NOT NULL,
    reason_code character varying(255) NOT NULL,
    rule_code character varying(255) NOT NULL,
    CONSTRAINT chk_exam_item_rule_hits_severity_status CHECK (((severity)::text = ANY (ARRAY[('INFO'::character varying)::text, ('WARNING'::character varying)::text, ('BLOCKING'::character varying)::text])))
);

CREATE TABLE exam_members (
    granted_at timestamp(6) with time zone NOT NULL,
    exam_id uuid NOT NULL,
    granted_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    user_id uuid NOT NULL,
    role character varying(20) NOT NULL,
    CONSTRAINT role CHECK (((role)::text = ANY (ARRAY[('CHAIR'::character varying)::text, ('AUTHOR'::character varying)::text, ('REVIEWER'::character varying)::text])))
);

CREATE TABLE exam_paper_items (
    item_order integer NOT NULL,
    weight numeric(5,2) NOT NULL,
    blueprint_slot_id uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    paper_id uuid,
    question_id uuid,
    section_id uuid
);

CREATE TABLE exam_paper_sections (
    section_order integer NOT NULL,
    section_time_limit_seconds integer,
    weight numeric(8,4),
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    paper_id uuid NOT NULL,
    updated_by uuid,
    title character varying(1024) NOT NULL,
    instruction character varying(2048)
);

CREATE TABLE exam_papers (
    time_duration_seconds integer,
    variant integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    blueprint_version_id uuid,
    created_by uuid,
    exam_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    CONSTRAINT chk_exam_papers_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('IN_REVIEW'::character varying)::text, ('APPROVED'::character varying)::text, ('LOCKED'::character varying)::text])))
);

CREATE TABLE exam_proctoring_alerts (
    id uuid DEFAULT uuidv7() NOT NULL,
    event_id character varying(64) NOT NULL,
    exam_session_id uuid NOT NULL,
    candidate_id uuid,
    stream_id character varying(64),
    stream_type character varying(20),
    alert_type character varying(64) NOT NULL,
    level character varying(16),
    source character varying(32),
    detail character varying(1024),
    confidence numeric(5,4),
    sequence_no bigint,
    captured_at timestamp(6) with time zone NOT NULL,
    raised_at timestamp(6) with time zone NOT NULL,
    created_at timestamp(6) with time zone NOT NULL
);

CREATE TABLE exam_recordings (
    duration_seconds integer,
    assembled_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    size_bytes bigint,
    candidate_id uuid NOT NULL,
    exam_session_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    status character varying(20) NOT NULL,
    stream_type character varying(20) NOT NULL,
    source character varying(32),
    s3_key character varying(1024) NOT NULL,
    bucket character varying(2048) NOT NULL,
    CONSTRAINT chk_exam_recordings_status_valid CHECK (((status)::text = ANY (ARRAY[('PROCESSING'::character varying)::text, ('READY'::character varying)::text, ('PARTIAL'::character varying)::text, ('FAILED'::character varying)::text, ('ABANDONED'::character varying)::text]))),
    CONSTRAINT chk_exam_recordings_stream_type_valid CHECK (((stream_type)::text = ANY (ARRAY[('SCREEN'::character varying)::text, ('CAMERA'::character varying)::text])))
);

CREATE TABLE exam_result_appeal_items (
    final_score numeric(5,2),
    appeal_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    paper_item_id uuid NOT NULL,
    response_id uuid NOT NULL
);

CREATE TABLE exam_result_appeals (
    score_after numeric(5,2),
    score_before numeric(5,2),
    approved_at timestamp(6) with time zone,
    deadline timestamp(6) with time zone,
    requested_at timestamp(6) with time zone NOT NULL,
    resolved_at timestamp(6) with time zone,
    candidate_result_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    requested_by uuid NOT NULL,
    resolved_by uuid,
    status character varying(20) NOT NULL,
    decision_note character varying(512),
    notes character varying(512),
    reason character varying(512) NOT NULL,
    reviewer_override_reason text,
    withdrawn_at timestamp(6) with time zone,
    CONSTRAINT chk_exam_result_appeals_status_valid CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('APPROVED'::character varying)::text, ('GRADING'::character varying)::text, ('PUBLISHED'::character varying)::text, ('REJECTED'::character varying)::text, ('WITHDRAWN'::character varying)::text])))
);

CREATE TABLE exam_result_status_histories (
    id uuid DEFAULT uuidv7() NOT NULL,
    actor_id uuid,
    candidate_result_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    from_status character varying(30),
    reason text,
    score_after numeric(5,2),
    score_before numeric(5,2),
    source character varying(30) NOT NULL,
    to_status character varying(30) NOT NULL,
    CONSTRAINT chk_exam_result_status_histories_source_valid CHECK (((source)::text = ANY (ARRAY[('AI_EVALUATION'::character varying)::text, ('TEACHER_INITIAL'::character varying)::text, ('TEACHER_SPOT_CHECK'::character varying)::text, ('TEACHER_REMEDIATION'::character varying)::text, ('TEACHER_APPEAL'::character varying)::text, ('ADMIN_BULK_FINALIZE'::character varying)::text, ('EXAM_PUBLISH'::character varying)::text, ('SYSTEM'::character varying)::text])))
);

CREATE TABLE exam_schedule_proctors (
    id uuid DEFAULT uuidv7() NOT NULL,
    schedule_id uuid NOT NULL,
    teacher_id uuid NOT NULL
);

CREATE TABLE exam_schedules (
    created_at timestamp(6) with time zone NOT NULL,
    end_date timestamp(6) with time zone NOT NULL,
    start_date timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    exam_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    moved_to_schedule_id uuid,
    school_room_id uuid,
    updated_by uuid,
    status character varying(20) NOT NULL,
    CONSTRAINT chk_exam_schedules_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text, ('COMPLETED'::character varying)::text, ('MOVED'::character varying)::text, ('CANCELLED'::character varying)::text, ('DELETED'::character varying)::text])))
);

CREATE TABLE exam_secure_pools (
    created_at timestamp(6) with time zone NOT NULL,
    embargo_until timestamp(6) with time zone,
    released_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    exam_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    released_by uuid,
    updated_by uuid,
    release_mode character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    CONSTRAINT chk_exam_secure_pools_release_mode_valid CHECK (((release_mode)::text = ANY (ARRAY[('MANUAL'::character varying)::text, ('AUTO_AFTER_CLOSE'::character varying)::text]))),
    CONSTRAINT chk_exam_secure_pools_status_valid CHECK (((status)::text = ANY (ARRAY[('SEALED'::character varying)::text, ('RELEASED'::character varying)::text])))
);

CREATE TABLE exam_sessions (
    flagged boolean NOT NULL,
    remaining_seconds integer,
    started_at timestamp(6) with time zone NOT NULL,
    submitted_at timestamp(6) with time zone,
    candidate_id uuid NOT NULL,
    exam_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    paper_id uuid NOT NULL,
    chosen_stream_type character varying(20),
    status character varying(20) NOT NULL,
    flag_reason text,
    CONSTRAINT chk_exam_sessions_chosen_stream_type_valid CHECK (((chosen_stream_type)::text = ANY (ARRAY[('CAMERA'::character varying)::text, ('SCREEN'::character varying)::text, ('CAMERA_AND_SCREEN'::character varying)::text]))),
    CONSTRAINT chk_exam_sessions_status_valid CHECK (((status)::text = ANY (ARRAY[('IN_PROGRESS'::character varying)::text, ('SUBMITTED'::character varying)::text, ('INTERRUPTED'::character varying)::text, ('GRADING'::character varying)::text, ('GRADED'::character varying)::text, ('EXPIRED'::character varying)::text, ('GRADING_FAILED'::character varying)::text])))
);

CREATE TABLE exams (
    exam_time_duration_second integer,
    max_attempt integer,
    requires_otp boolean DEFAULT true NOT NULL,
    close_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    open_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    assessment_policy_id uuid,
    blueprint_id uuid,
    blueprint_version_id uuid,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    language_id uuid NOT NULL,
    school_id uuid,
    updated_by uuid,
    delivery_mode character varying(20),
    kind character varying(20) NOT NULL,
    required_stream_type character varying(20),
    result_decision_method character varying(20),
    status character varying(20) NOT NULL,
    stream_type_permission character varying(20),
    code character varying(100) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL,
    ai_confidence_threshold_percent numeric(5,2),
    CONSTRAINT chk_exams_ai_confidence_threshold_percent CHECK (((ai_confidence_threshold_percent IS NULL) OR ((ai_confidence_threshold_percent >= (0)::numeric) AND (ai_confidence_threshold_percent <= (100)::numeric)))),
    CONSTRAINT chk_exams_delivery_mode_valid CHECK (((delivery_mode)::text = ANY (ARRAY[('STUDENT_DEVICE'::character varying)::text, ('LAB'::character varying)::text]))),
    CONSTRAINT chk_exams_kind_valid CHECK (((kind)::text = ANY (ARRAY[('CENTRALIZED'::character varying)::text, ('CLASS_TEST'::character varying)::text]))),
    CONSTRAINT chk_exams_required_stream_type_and_stream_type_permission_valid CHECK ((((required_stream_type IS NULL) AND (stream_type_permission IS NULL)) OR (((required_stream_type)::text = 'CAMERA_AND_SCREEN'::text) AND ((stream_type_permission)::text = ANY (ARRAY[('ANY'::character varying)::text, ('ALL'::character varying)::text]))) OR (((required_stream_type)::text = ANY (ARRAY[('CAMERA'::character varying)::text, ('SCREEN'::character varying)::text])) AND (stream_type_permission IS NULL)))),
    CONSTRAINT chk_exams_required_stream_type_valid CHECK (((required_stream_type)::text = ANY (ARRAY[('CAMERA'::character varying)::text, ('SCREEN'::character varying)::text, ('CAMERA_AND_SCREEN'::character varying)::text]))),
    CONSTRAINT chk_exams_result_decision_method_valid CHECK (((result_decision_method)::text = ANY (ARRAY[('HIGHEST'::character varying)::text, ('LATEST'::character varying)::text, ('AVERAGE'::character varying)::text, ('FIRST'::character varying)::text, ('LOWEST'::character varying)::text]))),
    CONSTRAINT chk_exams_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('SCHEDULED'::character varying)::text, ('IN_PROGRESS'::character varying)::text, ('CLOSED'::character varying)::text, ('RESULTS_PUBLISHED'::character varying)::text, ('CANCELLED'::character varying)::text]))),
    CONSTRAINT chk_exams_stream_type_permission_valid CHECK (((stream_type_permission)::text = ANY (ARRAY[('ANY'::character varying)::text, ('ALL'::character varying)::text])))
);

CREATE TABLE exchange_rate_snapshot (
    id uuid DEFAULT uuidv7() NOT NULL,
    fetched_at timestamp(6) with time zone NOT NULL,
    usd_to_vnd_rate numeric(12,4) NOT NULL,
    source character varying(255) NOT NULL
);

CREATE TABLE financial_event (
    amount_signed numeric(15,0) NOT NULL,
    currency character varying(3) NOT NULL,
    occurred_at timestamp(6) with time zone NOT NULL,
    actor_id uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    subscription_id uuid,
    payment_method character varying(20) NOT NULL,
    event_type character varying(30) NOT NULL,
    payload text,
    CONSTRAINT chk_financial_event_event_type_valid CHECK (((event_type)::text = ANY (ARRAY[('SUB_PURCHASED'::character varying)::text, ('SUB_RENEWED'::character varying)::text, ('SUB_CANCELLED'::character varying)::text, ('SUB_UPGRADED'::character varying)::text, ('TOKEN_PURCHASED'::character varying)::text, ('TOKEN_CONSUMED'::character varying)::text, ('REFUND_ISSUED'::character varying)::text, ('SUB_SUSPENDED'::character varying)::text, ('SUB_UNSUSPENDED'::character varying)::text]))),
    CONSTRAINT chk_financial_event_payment_method_valid CHECK (((payment_method)::text = ANY (ARRAY[('PAYOS'::character varying)::text, ('SEPAY'::character varying)::text, ('MANUAL'::character varying)::text])))
);

CREATE TABLE framework_criteria (
    criteria_order integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    framework_version_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    code character varying(50) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL
);

CREATE TABLE framework_criterion_bands (
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    framework_criterion_id uuid NOT NULL,
    framework_result_band_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    descriptor text,
    negative_signals_json text,
    positive_signals_json text
);

CREATE TABLE framework_result_bands (
    result_band_order integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    framework_version_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    code character varying(50) NOT NULL,
    label character varying(100) NOT NULL,
    description character varying(2048)
);

CREATE TABLE framework_versions (
    version integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) with time zone NOT NULL,
    effective_to timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    framework_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL,
    CONSTRAINT chk_framework_versions_effective_range_valid CHECK (((effective_to IS NULL) OR (effective_from <= effective_to))),
    CONSTRAINT chk_framework_versions_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text, ('ARCHIVED'::character varying)::text]))),
    CONSTRAINT chk_framework_versions_version_positive CHECK ((version > 0))
);

CREATE TABLE frameworks (
    is_active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    code character varying(50) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL
);

CREATE TABLE grade_level_band_scopes (
    id uuid DEFAULT uuidv7() NOT NULL,
    grade_level_id uuid NOT NULL,
    framework_version_id uuid NOT NULL,
    default_target_band_id uuid NOT NULL,
    hard_max_band_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    updated_by uuid
);

CREATE TABLE grade_levels (
    id uuid DEFAULT uuidv7() NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(2048),
    grade_level_order integer NOT NULL,
    status character varying(20) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    updated_by uuid,
    CONSTRAINT chk_grade_levels_order_positive CHECK ((grade_level_order > 0)),
    CONSTRAINT chk_grade_levels_status_valid CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text])))
);

CREATE TABLE import_rows (
    row_number bigint NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    session_id uuid NOT NULL,
    status character varying(30) NOT NULL,
    errors_json text,
    mapped_data_json text,
    raw_data_json text NOT NULL,
    CONSTRAINT chk_import_rows_row_number_positive CHECK ((row_number > 0)),
    CONSTRAINT chk_import_rows_status_valid CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('VALID'::character varying)::text, ('INVALID'::character varying)::text, ('IMPORTED'::character varying)::text, ('SKIPPED'::character varying)::text, ('FAILED'::character varying)::text])))
);

CREATE TABLE import_sessions (
    attempts integer NOT NULL,
    claimed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    imported_rows bigint NOT NULL,
    invalid_rows bigint NOT NULL,
    lease_expires_at timestamp(6) with time zone,
    skipped_rows bigint NOT NULL,
    total_rows bigint NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    valid_rows bigint NOT NULL,
    claimed_by uuid,
    created_by uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    imported_entity_id uuid,
    school_id uuid,
    updated_by uuid NOT NULL,
    status character varying(30) NOT NULL,
    type character varying(30) NOT NULL,
    confirmed_mapping_json text,
    failure_reason text,
    file_name character varying(255) NOT NULL,
    original_headers_json text NOT NULL,
    suggested_mapping_json text,
    CONSTRAINT chk_import_sessions_row_counts_non_negative CHECK (((valid_rows >= 0) AND (invalid_rows >= 0) AND (imported_rows >= 0) AND (skipped_rows >= 0) AND (total_rows >= 0))),
    CONSTRAINT chk_import_sessions_status_valid CHECK (((status)::text = ANY (ARRAY[('PREVIEWED'::character varying)::text, ('VALIDATING'::character varying)::text, ('IMPORTING'::character varying)::text, ('QUEUED'::character varying)::text, ('COMPLETED'::character varying)::text, ('FAILED'::character varying)::text, ('EXPIRED'::character varying)::text, ('CANCELLED'::character varying)::text]))),
    CONSTRAINT chk_import_sessions_type_valid CHECK (((type)::text = ANY (ARRAY[('USER'::character varying)::text, ('SCHOOL_CLASS'::character varying)::text, ('SCHOOL_CLASS_USER'::character varying)::text, ('QUESTION'::character varying)::text, ('SCHOOL_DIRECTORY'::character varying)::text, ('SCHOOL_GRADE'::character varying)::text, ('SCHOOL_ROOM'::character varying)::text, ('RUBRIC_VERSION'::character varying)::text, ('RUBRIC_CRITERION'::character varying)::text, ('RUBRIC_RESULT_BAND'::character varying)::text, ('ASSESSMENT_POLICY'::character varying)::text])))
);

CREATE TABLE interest_dimension (
    code character varying(32) NOT NULL,
    label character varying(128) NOT NULL,
    description character varying(512),
    active boolean DEFAULT true NOT NULL,
    quiz_eligible boolean DEFAULT true NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE interest_quiz_item (
    id uuid DEFAULT uuidv7() NOT NULL,
    dimensions_json text NOT NULL,
    statements_json text NOT NULL,
    desirability_note text,
    active boolean DEFAULT true NOT NULL,
    student_id uuid,
    created_at timestamp with time zone NOT NULL
);

CREATE TABLE invoice (
    amount numeric(15,0) NOT NULL,
    issue_date date NOT NULL,
    paid_at timestamp(6) with time zone,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    source_id uuid NOT NULL,
    subscription_id uuid,
    source_type character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    checkout_url character varying(2048),
    invoice_number character varying(255) NOT NULL,
    payment_link_id character varying(255),
    resolved_plan_id uuid,
    payment_provider character varying(20) NOT NULL,
    provider_order_ref character varying(100),
    CONSTRAINT chk_invoice_payment_provider_valid CHECK (((payment_provider)::text = ANY (ARRAY[('PAYOS'::character varying)::text, ('SEPAY'::character varying)::text, ('MANUAL'::character varying)::text]))),
    CONSTRAINT chk_invoice_source_type_valid CHECK (((source_type)::text = ANY (ARRAY[('SUBSCRIPTION'::character varying)::text, ('SUBSCRIPTION_REQUEST'::character varying)::text, ('TOKEN_PURCHASE'::character varying)::text]))),
    CONSTRAINT chk_invoice_status_valid CHECK (((status)::text = ANY (ARRAY[('PAID'::character varying)::text, ('PENDING'::character varying)::text, ('FAILED'::character varying)::text, ('CANCELLED'::character varying)::text])))
);

CREATE TABLE learner_profile (
    id uuid DEFAULT uuidv7() NOT NULL,
    student_id uuid NOT NULL,
    goal_type character varying(24),
    auto_update_interest boolean DEFAULT true NOT NULL,
    quiz_completed_at timestamp with time zone,
    recorded_at timestamp with time zone NOT NULL
);

CREATE TABLE notification_devices (
    id uuid DEFAULT uuidv7() NOT NULL,
    user_id uuid NOT NULL,
    device_id character varying(255) NOT NULL,
    platform character varying(20) NOT NULL,
    installation_id character varying(50) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    last_seen_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT chk_notification_devices_platform_valid CHECK (((platform)::text = ANY (ARRAY[('WEB'::character varying)::text, ('ANDROID'::character varying)::text, ('IOS'::character varying)::text])))
);

CREATE TABLE notification_preferences (
    id uuid DEFAULT uuidv7() NOT NULL,
    user_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    push_enabled boolean NOT NULL,
    email_enabled boolean NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT chk_notification_preferences_category_valid CHECK (((category)::text = ANY (ARRAY[('EXAM_RESULT'::character varying)::text, ('EXAM_APPEAL'::character varying)::text, ('GRADING'::character varying)::text, ('EXAM_SCHEDULE'::character varying)::text, ('EXAM_BLUEPRINT'::character varying)::text, ('BILLING'::character varying)::text, ('SYSTEM'::character varying)::text])))
);

CREATE TABLE notifications (
    id uuid DEFAULT uuidv7() NOT NULL,
    user_id uuid NOT NULL,
    event_id uuid,
    event_type character varying(150) NOT NULL,
    title character varying(255) NOT NULL,
    body text,
    payload jsonb,
    read_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL
);

CREATE TABLE outboxes (
    retry_count integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    next_retry_at timestamp(6) with time zone,
    published_at timestamp(6) with time zone,
    aggregate_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    status character varying(20) NOT NULL,
    aggregate_type character varying(100) NOT NULL,
    event_type character varying(150) NOT NULL,
    last_error text,
    payload jsonb,
    CONSTRAINT chk_outboxs_status_valid CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('PROCESSING'::character varying)::text, ('PUBLISHED'::character varying)::text, ('FAILED'::character varying)::text])))
);

CREATE TABLE password_set_up_tokens (
    created_at timestamp(6) with time zone NOT NULL,
    expired_at timestamp(6) with time zone NOT NULL,
    used_at timestamp(6) with time zone,
    id uuid DEFAULT uuidv7() NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(255) NOT NULL
);

CREATE TABLE plan_quota (
    included_quantity numeric(18,6) NOT NULL,
    token_unit_price numeric(15,0) NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    plan_id uuid NOT NULL,
    quota_type character varying(20) NOT NULL,
    CONSTRAINT chk_plan_quota_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[('GRADING'::character varying)::text, ('CLASS_TEST'::character varying)::text, ('PRACTICE'::character varying)::text])))
);

CREATE TABLE practice_criterion_score (
    id uuid NOT NULL,
    practice_evaluation_id uuid NOT NULL,
    criterion_code character varying(32) NOT NULL,
    final_score numeric(7,3)
);

CREATE TABLE practice_item_evaluation (
    id uuid NOT NULL,
    practice_response_id uuid NOT NULL,
    item_score numeric(5,2),
    marked_invalid boolean DEFAULT false NOT NULL,
    evaluated_at timestamp with time zone NOT NULL
);

CREATE TABLE practice_item_response (
    id uuid NOT NULL,
    practice_session_id uuid NOT NULL,
    practice_question_id uuid NOT NULL,
    audio_url text,
    transcript text,
    question_complete boolean DEFAULT false NOT NULL,
    grading_requested_at timestamp with time zone,
    grading_status character varying(16) DEFAULT 'PENDING'::character varying NOT NULL,
    grading_attempts integer DEFAULT 0 NOT NULL,
    CONSTRAINT chk_practice_item_response_grading_status CHECK (((grading_status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('GRADING'::character varying)::text, ('GRADED'::character varying)::text, ('GRADING_FAILED'::character varying)::text])))
);

CREATE TABLE practice_paper (
    id uuid NOT NULL,
    student_id uuid NOT NULL,
    practice_topic_id uuid NOT NULL,
    target_framework_band_id uuid,
    origin character varying(24) NOT NULL,
    goal_type character varying(24) NOT NULL,
    offered_topic_ids_json text NOT NULL,
    previous_offered_topic_ids_json text NOT NULL,
    planned_seconds integer NOT NULL,
    reserved_quota_seconds integer NOT NULL,
    reservation_expires_at timestamp with time zone NOT NULL,
    status character varying(24) NOT NULL,
    created_at timestamp with time zone NOT NULL
);

CREATE TABLE practice_paper_item (
    id uuid NOT NULL,
    practice_paper_id uuid NOT NULL,
    practice_question_id uuid NOT NULL,
    slot_order integer NOT NULL,
    target_criterion_code character varying(32) NOT NULL,
    target_sub_attribute character varying(64),
    target_difficulty_rank integer NOT NULL
);

CREATE TABLE practice_question (
    id uuid DEFAULT uuidv7() NOT NULL,
    practice_topic_id uuid NOT NULL,
    question_text text NOT NULL,
    target_criterion_code character varying(32) NOT NULL,
    target_sub_attribute character varying(64),
    target_tense character varying(16),
    difficulty_rank integer NOT NULL,
    difficulty_features_json text NOT NULL,
    evaluation_guide_json text NOT NULL,
    suggested_ideas_json text,
    question_type character varying(24) NOT NULL,
    min_response_seconds integer NOT NULL,
    max_response_seconds integer NOT NULL,
    vstep_part integer,
    source character varying(24) DEFAULT 'AI_GENERATED'::character varying NOT NULL,
    usage_count integer DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT chk_practice_question_difficulty_rank CHECK (((difficulty_rank >= 1) AND (difficulty_rank <= 20))),
    CONSTRAINT chk_practice_question_time_budgets CHECK (((min_response_seconds > 0) AND (max_response_seconds > min_response_seconds))),
    CONSTRAINT chk_practice_question_type CHECK (((question_type)::text = ANY (ARRAY[('SHORT_ANSWER'::character varying)::text, ('LONG_ANSWER'::character varying)::text, ('DESCRIPTION'::character varying)::text, ('OPINION'::character varying)::text])))
);

CREATE TABLE practice_response_turn (
    id uuid NOT NULL,
    practice_response_id uuid NOT NULL,
    turn_order integer NOT NULL,
    turn_type character varying(24) NOT NULL,
    prompt_text text,
    audio_url text,
    transcript text,
    duration_seconds integer NOT NULL,
    word_feedback_json text,
    turn_score numeric(5,2)
);

CREATE TABLE practice_session (
    id uuid NOT NULL,
    student_id uuid NOT NULL,
    practice_paper_id uuid NOT NULL,
    target_framework_band_id uuid NOT NULL,
    chosen_practice_topic_id uuid NOT NULL,
    target_sub_attributes_json text,
    origin character varying(24) NOT NULL,
    offered_topic_ids_json text,
    overall_score numeric(5,2),
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    last_heartbeat_at timestamp with time zone,
    graded_seconds integer DEFAULT 0 NOT NULL,
    status character varying(16) NOT NULL,
    abandon_diagnosis character varying(16),
    help_request_count integer DEFAULT 0 NOT NULL,
    long_pause_count integer DEFAULT 0 NOT NULL
);

CREATE TABLE practice_topic (
    id uuid DEFAULT uuidv7() NOT NULL,
    name character varying(200) NOT NULL,
    normalized_name character varying(200) NOT NULL,
    description text,
    source character varying(24) NOT NULL,
    interest_dimension character varying(32) NOT NULL,
    curriculum_group character varying(24) NOT NULL,
    source_question_topic_id uuid,
    temporal_affordance character varying(8),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone NOT NULL
);

CREATE TABLE processed_events (
    processed_at timestamp(6) with time zone NOT NULL,
    event_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    consumer_group character varying(255) NOT NULL
);

CREATE TABLE question_assets (
    duration_seconds integer,
    question_asset_order integer NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    question_id uuid NOT NULL,
    type character varying(100) NOT NULL,
    description character varying(2048),
    url character varying(4096),
    alt_text character varying(255),
    title character varying(255),
    transcript text,
    CONSTRAINT chk_question_assets_type_valid CHECK (((type)::text = ANY (ARRAY[('AUDIO'::character varying)::text, ('IMAGE'::character varying)::text, ('VIDEO'::character varying)::text, ('TEXT_PASSAGE'::character varying)::text])))
);

CREATE TABLE question_bank_grades (
    attached_at timestamp(6) with time zone NOT NULL,
    attached_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    question_bank_id uuid NOT NULL,
    school_grade_id uuid NOT NULL
);

CREATE TABLE question_banks (
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    language_id uuid NOT NULL,
    school_id uuid,
    updated_by uuid,
    owner_type character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT chk_owner_type_and_school_id_valid CHECK (((((owner_type)::text = 'SYSTEM'::text) AND (school_id IS NULL)) OR (((owner_type)::text = 'SCHOOL'::text) AND (school_id IS NOT NULL)))),
    CONSTRAINT chk_quesion_banks_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text, ('ARCHIVED'::character varying)::text]))),
    CONSTRAINT chk_question_banks_owner_type_valid CHECK (((owner_type)::text = ANY (ARRAY[('SYSTEM'::character varying)::text, ('SCHOOL'::character varying)::text])))
);

CREATE TABLE question_collaborators (
    assigned_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    question_id uuid NOT NULL,
    user_id uuid NOT NULL,
    permission character varying(20) NOT NULL,
    CONSTRAINT chk_question_collaborators_permission_valid CHECK (((permission)::text = ANY (ARRAY[('READ_ONLY'::character varying)::text, ('CAN_USE'::character varying)::text, ('CAN_EDIT'::character varying)::text])))
);

CREATE TABLE question_evaluation_guides (
    id uuid DEFAULT uuidv7() NOT NULL,
    question_id uuid NOT NULL,
    acceptable_responses text,
    common_mistakes text,
    expected_content text,
    key_points text,
    off_topic_examples text,
    scoring_hints text
);

CREATE TABLE question_topics (
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    question_bank_id uuid NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL,
    CONSTRAINT chk_question_topics_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text, ('ARCHIVED'::character varying)::text])))
);

CREATE TABLE questions (
    locked boolean NOT NULL,
    max_response_seconds integer NOT NULL,
    min_response_seconds integer NOT NULL,
    preparation_time_seconds integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    question_bank_id uuid NOT NULL,
    question_topic_id uuid,
    secure_pool_id uuid,
    source_question_id uuid,
    updated_by uuid,
    confidentiality character varying(20) NOT NULL,
    type character varying(50) NOT NULL,
    code character varying(100) NOT NULL,
    sharing character varying(100),
    instruction_text text,
    preparation_text text,
    prompt_text text,
    question_text text NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT chk_questions_confidentiality_valid CHECK (((confidentiality)::text = ANY (ARRAY[('OPEN'::character varying)::text, ('EXAM_RESTRICTED'::character varying)::text, ('RELEASED'::character varying)::text]))),
    CONSTRAINT chk_questions_max_response_seconds_valid CHECK ((max_response_seconds >= 0)),
    CONSTRAINT chk_questions_min_response_seconds_and_max_response_seconds_val CHECK ((min_response_seconds <= max_response_seconds)),
    CONSTRAINT chk_questions_min_response_seconds_valid CHECK ((min_response_seconds >= 0)),
    CONSTRAINT chk_questions_preparation_time_seconds_valid CHECK ((preparation_time_seconds >= 0)),
    CONSTRAINT chk_questions_sharing_valid CHECK (((sharing)::text = ANY (ARRAY[('PRIVATE'::character varying)::text, ('SCHOOL_SHARED'::character varying)::text]))),
    CONSTRAINT chk_questions_type_valid CHECK (((type)::text = ANY (ARRAY[('READ_ALOUD'::character varying)::text, ('SHORT_ANSWER'::character varying)::text, ('LONG_ANSWER'::character varying)::text, ('OPINION'::character varying)::text, ('DESCRIPTION'::character varying)::text]))),
    CONSTRAINT chk_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('SUBMITTED_FOR_REVIEW'::character varying)::text, ('REVISION_REQUESTED'::character varying)::text, ('APPROVED'::character varying)::text, ('REJECTED'::character varying)::text, ('PUBLISHED'::character varying)::text, ('ARCHIVED'::character varying)::text])))
);

CREATE TABLE quota_pricing_calibration (
    id uuid DEFAULT uuidv7() NOT NULL,
    computed_at timestamp(6) with time zone NOT NULL,
    window_days integer NOT NULL,
    session_count integer NOT NULL,
    total_cost_usd numeric(12,6) NOT NULL,
    total_answered_seconds bigint NOT NULL,
    raw_rate_usd_per_second numeric(12,6) NOT NULL,
    applied_rate_usd_per_second numeric(12,6) NOT NULL,
    note text,
    pricing_source character varying(16) DEFAULT 'EXAM'::character varying NOT NULL
);

CREATE TABLE refresh_tokens (
    expired_at timestamp(6) with time zone NOT NULL,
    issued_at timestamp(6) with time zone NOT NULL,
    used_at timestamp(6) with time zone,
    id uuid DEFAULT uuidv7() NOT NULL,
    replaced_by uuid,
    session_id uuid NOT NULL,
    token_hash character varying(512) NOT NULL
);

CREATE TABLE register_form_documents (
    created_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    register_form_id uuid NOT NULL,
    url character varying(4096) NOT NULL
);

CREATE TABLE register_forms (
    date_of_birth date NOT NULL,
    student_count integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    verified_at timestamp(6) with time zone,
    postal_code character varying(10) NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    reviewed_by uuid,
    school_directory_id uuid,
    contact_phone character varying(20) NOT NULL,
    identity_number character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    verification_method character varying(20) NOT NULL,
    "position" character varying(50) NOT NULL,
    school_domain character varying(100),
    contact_address character varying(512) NOT NULL,
    school_address character varying(512),
    contact_email character varying(255) NOT NULL,
    contact_full_name character varying(255) NOT NULL,
    rejected_reason character varying(255),
    school_district character varying(255),
    school_name character varying(255),
    school_province character varying(255),
    CONSTRAINT chk_register_form_verification_method_valid CHECK (((verification_method)::text = ANY (ARRAY[('DOMAIN_OTP'::character varying)::text, ('DOCUMENT'::character varying)::text]))),
    CONSTRAINT chk_register_forms_status_valid CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('AUTO_APPROVED'::character varying)::text, ('APPROVED'::character varying)::text, ('REJECTED'::character varying)::text])))
);

CREATE TABLE roles (
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    code character varying(100) NOT NULL,
    name character varying(255) NOT NULL
);

CREATE TABLE rubric_criterions (
    criterion_order integer NOT NULL,
    is_required boolean NOT NULL,
    max_score numeric(6,2) NOT NULL,
    min_score numeric(6,2) NOT NULL,
    weight numeric(6,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    framework_criterion_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    rubric_version_id uuid NOT NULL,
    updated_by uuid,
    code character varying(50) NOT NULL,
    description character varying(2048),
    examples_json text,
    name character varying(255) NOT NULL,
    CONSTRAINT chk_rubric_criterions_order_positive CHECK ((criterion_order > 0)),
    CONSTRAINT chk_rubric_criterions_score_range_valid CHECK ((min_score <= max_score)),
    CONSTRAINT chk_rubric_criterions_weight_non_negative CHECK ((weight >= (0)::numeric))
);

CREATE TABLE rubric_result_bands (
    result_order integer NOT NULL,
    score_max numeric NOT NULL,
    score_min numeric NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    rubric_version_id uuid NOT NULL,
    updated_by uuid,
    description character varying(2048),
    code character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT chk_rubric_result_bands_order_valid CHECK ((result_order > 0)),
    CONSTRAINT chk_rubric_result_bands_score_min_max_valid CHECK ((score_min <= score_max))
);

CREATE TABLE rubric_versions (
    scoring_scale_max numeric(6,2) NOT NULL,
    scoring_scale_min numeric(6,2) NOT NULL,
    version integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    effective_from timestamp(6) with time zone NOT NULL,
    effective_to timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    rubric_id uuid NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    total_score_method character varying(30) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL,
    source_rubric_version_id uuid,
    CONSTRAINT chk_rubric_versions_effective_range_valid CHECK (((effective_to IS NULL) OR (effective_from <= effective_to))),
    CONSTRAINT chk_rubric_versions_scoring_scale_valid CHECK ((scoring_scale_min <= scoring_scale_max)),
    CONSTRAINT chk_rubric_versions_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text, ('ARCHIVED'::character varying)::text]))),
    CONSTRAINT chk_rubric_versions_total_score_method_valid CHECK (((total_score_method)::text = ANY (ARRAY[('WEIGHTED_AVERAGE'::character varying)::text, ('SUM'::character varying)::text]))),
    CONSTRAINT chk_rubric_versions_version_positive CHECK ((version > 0))
);

CREATE TABLE rubrics (
    framework_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    language_id uuid NOT NULL,
    school_id uuid,
    owner_type character varying(20) NOT NULL,
    code character varying(50) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL,
    CONSTRAINT chk_rubrics_owner_school_valid CHECK (((((owner_type)::text = 'SYSTEM'::text) AND (school_id IS NULL)) OR (((owner_type)::text = 'SCHOOL'::text) AND (school_id IS NOT NULL)))),
    CONSTRAINT chk_rubrics_owner_type_valid CHECK (((owner_type)::text = ANY (ARRAY[('SYSTEM'::character varying)::text, ('SCHOOL'::character varying)::text])))
);

CREATE TABLE saved_topic (
    id uuid NOT NULL,
    student_id uuid NOT NULL,
    practice_topic_id uuid NOT NULL,
    saved_at timestamp with time zone NOT NULL
);

CREATE TABLE school_class_users (
    is_active boolean NOT NULL,
    joined_at timestamp(6) with time zone NOT NULL,
    left_at timestamp(6) with time zone,
    assigned_by uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_class_id uuid NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE school_classes (
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    language_id uuid NOT NULL,
    school_grade_id uuid NOT NULL,
    school_id uuid NOT NULL,
    updated_by uuid NOT NULL,
    status character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL,
    CONSTRAINT chk_school_class_status_valid CHECK (((status)::text = ANY (ARRAY[('INACTIVE'::character varying)::text, ('ACTIVE'::character varying)::text, ('ARCHIVED'::character varying)::text])))
);

CREATE TABLE school_debt_event (
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    subscription_id uuid NOT NULL,
    event_type character varying(20) NOT NULL,
    quota_type character varying(20) NOT NULL,
    trigger_exam_session_id uuid,
    trigger_amount_usd numeric(18,6),
    total_allocated_usd numeric(18,6) NOT NULL,
    used_quantity_usd numeric(18,6) NOT NULL,
    overage_usd numeric(18,6) NOT NULL,
    occurred_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT chk_school_debt_event_event_type_valid CHECK (((event_type)::text = ANY (ARRAY[('LOCKED'::character varying)::text, ('CAP_EXCEEDED'::character varying)::text, ('CLEARED'::character varying)::text]))),
    CONSTRAINT chk_school_debt_event_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[('GRADING'::character varying)::text, ('CLASS_TEST'::character varying)::text, ('PRACTICE'::character varying)::text])))
);

CREATE TABLE school_directories (
    verified boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    source character varying(30) NOT NULL,
    code character varying(100) NOT NULL,
    domain character varying(100),
    province_code character varying(100) NOT NULL,
    address character varying(512) NOT NULL,
    district_name character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    province_name character varying(255) NOT NULL,
    CONSTRAINT chk_school_directories_source_valid CHECK (((source)::text = ANY (ARRAY[('OFFICIAL_IMPORT'::character varying)::text, ('ADMIN_CREATED'::character varying)::text, ('USER_SUBMITTED'::character varying)::text])))
);

CREATE TABLE school_grades (
    end_date date NOT NULL,
    start_date date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    status character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL,
    school_id uuid NOT NULL,
    grade_level_id uuid NOT NULL,
    CONSTRAINT chk_school_grades_start_end_date_valid CHECK ((start_date < end_date)),
    CONSTRAINT chk_status_valid CHECK (((status)::text = ANY (ARRAY[('INACTIVE'::character varying)::text, ('ACTIVE'::character varying)::text, ('ARCHIVED'::character varying)::text])))
);

CREATE TABLE school_rooms (
    is_active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    updated_by uuid,
    code character varying(50) NOT NULL,
    description character varying(2048),
    name character varying(255) NOT NULL
);

CREATE TABLE school_subscription (
    end_date date NOT NULL,
    price_paid_snapshot numeric(15,0) NOT NULL,
    start_date date NOT NULL,
    cancelled_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    plan_id uuid NOT NULL,
    school_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    suspended_at timestamp with time zone,
    suspended_reason text,
    suspended_by uuid,
    CONSTRAINT chk_school_subscription_status_valid CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('EXPIRED'::character varying)::text, ('CANCELLED'::character varying)::text, ('SUSPENDED'::character varying)::text])))
);

CREATE TABLE school_users (
    end_date timestamp(6) with time zone,
    start_date timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    school_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT chk_school_users_start_date_and_end_date_valid CHECK ((start_date < end_date))
);

CREATE TABLE schools (
    is_active boolean NOT NULL,
    student_count integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid NOT NULL,
    contact_phone character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    domain character varying(100),
    address character varying(512) NOT NULL,
    description character varying(2048),
    contact_email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT chk_student_count_positive CHECK ((student_count > 0))
);

CREATE TABLE scoring_rules (
    is_active boolean NOT NULL,
    priority integer NOT NULL,
    stop_processing boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    policy_id uuid NOT NULL,
    updated_by uuid,
    severity character varying(20) NOT NULL,
    action_type character varying(50) NOT NULL,
    code character varying(50) NOT NULL,
    condition_type character varying(50) NOT NULL,
    description character varying(2048),
    action_params_json text NOT NULL,
    condition_params_json text NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT chk_scoring_rules_action_type_valid CHECK (((action_type)::text = ANY (ARRAY[('CAP_FINAL_SCORE'::character varying)::text, ('CAP_CRITERION_SCORE'::character varying)::text, ('ADD_FINAL_SCORE_DELTA'::character varying)::text, ('ADD_CRITERION_SCORE_DELTA'::character varying)::text, ('CAP_FRAMEWORK_RESULT_BAND'::character varying)::text, ('SET_FRAMEWORK_RESULT_BAND'::character varying)::text, ('REQUIRE_HUMAN_REVIEW'::character varying)::text, ('MARK_RESPONSE_INVALID'::character varying)::text, ('REQUIRE_RETAKE'::character varying)::text, ('ADD_FEEDBACK_TAG'::character varying)::text, ('ADD_REVIEW_REASON'::character varying)::text]))),
    CONSTRAINT chk_scoring_rules_condition_type_valid CHECK (((condition_type)::text = ANY (ARRAY[('DURATION_LESS_THAN'::character varying)::text, ('DURATION_GREATER_THAN'::character varying)::text, ('WORD_COUNT_LESS_THAN'::character varying)::text, ('WORD_COUNT_GREATER_THAN'::character varying)::text, ('CRITERION_SCORE_LESS_THAN'::character varying)::text, ('CRITERION_SCORE_GREATER_THAN'::character varying)::text, ('FINAL_SCORE_LESS_THAN'::character varying)::text, ('FINAL_SCORE_GREATER_THAN'::character varying)::text, ('TASK_RELEVANCE_LESS_THAN'::character varying)::text, ('OFF_TOPIC_RATIO_GREATER_THAN'::character varying)::text, ('CODE_SWITCHING_RATIO_GREATER_THAN'::character varying)::text, ('ASR_CONFIDENCE_LESS_THAN'::character varying)::text, ('AI_CONFIDENCE_LESS_THAN'::character varying)::text, ('AUDIO_QUALITY_LESS_THAN'::character varying)::text, ('SILENCE_RATIO_GREATER_THAN'::character varying)::text, ('SPEECH_RATE_LESS_THAN'::character varying)::text, ('SPEECH_RATE_GREATER_THAN'::character varying)::text]))),
    CONSTRAINT chk_scoring_rules_priority_positive CHECK ((priority > 0)),
    CONSTRAINT chk_scoring_rules_severity_valid CHECK (((severity)::text = ANY (ARRAY[('INFO'::character varying)::text, ('WARNING'::character varying)::text, ('BLOCKING'::character varying)::text])))
);

CREATE TABLE student_question_exposure (
    id uuid NOT NULL,
    student_id uuid NOT NULL,
    practice_question_id uuid NOT NULL,
    seen_at timestamp with time zone NOT NULL
);

CREATE TABLE subscription_plan (
    max_time_per_attempt_min integer NOT NULL,
    price_per_year numeric(15,0) NOT NULL,
    validity_days integer NOT NULL,
    version integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    status character varying(20) NOT NULL,
    name character varying(255) NOT NULL,
    tagline character varying(255),
    replaced_by_plan_id uuid,
    service_fee_ratio numeric(5,4) DEFAULT 0.20 NOT NULL,
    CONSTRAINT chk_subscription_plan_status_valid CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('ACTIVE'::character varying)::text, ('ARCHIVED'::character varying)::text])))
);

CREATE TABLE subscription_quota (
    total_allocated numeric(18,6) NOT NULL,
    used_quantity numeric(18,6) NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    subscription_id uuid NOT NULL,
    quota_type character varying(20) NOT NULL,
    CONSTRAINT chk_subscription_quota_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[('GRADING'::character varying)::text, ('CLASS_TEST'::character varying)::text, ('PRACTICE'::character varying)::text])))
);

CREATE TABLE subscription_quota_user_allocations (
    id uuid DEFAULT uuidv7() NOT NULL,
    allocated_quantity numeric(18,6) NOT NULL,
    quota_type character varying(20) NOT NULL,
    subscription_id uuid NOT NULL,
    used_quantity numeric(18,6) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT chk_subscription_quota_user_allocations_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[('CLASS_TEST'::character varying)::text, ('PRACTICE'::character varying)::text])))
);

CREATE TABLE subscription_request (
    amount numeric(15,0) NOT NULL,
    reviewed_at timestamp(6) with time zone,
    submitted_at timestamp(6) with time zone NOT NULL,
    current_plan_id uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    requested_plan_id uuid NOT NULL,
    reviewed_by uuid,
    school_id uuid NOT NULL,
    request_type character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    CONSTRAINT chk_subscription_request_request_type_valid CHECK (((request_type)::text = ANY (ARRAY[('REGISTRATION'::character varying)::text, ('UPGRADE'::character varying)::text]))),
    CONSTRAINT chk_subscription_request_status_valid CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('APPROVED'::character varying)::text, ('REJECTED'::character varying)::text])))
);

CREATE TABLE supported_languages (
    is_active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    code character varying(10) NOT NULL,
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    name character varying(100) NOT NULL,
    description character varying(2048)
);

CREATE TABLE token_purchase (
    total_amount numeric(15,0) NOT NULL,
    purchased_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    subscription_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    CONSTRAINT chk_token_purchase_status_valid CHECK (((status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('PAID'::character varying)::text, ('FAILED'::character varying)::text])))
);

CREATE TABLE token_purchase_item (
    quantity numeric(18,6) NOT NULL,
    subtotal numeric(15,0) NOT NULL,
    unit_price_snapshot numeric(15,0) NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    purchase_id uuid NOT NULL,
    quota_type character varying(20) NOT NULL,
    CONSTRAINT chk_token_purchase_item_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[('GRADING'::character varying)::text, ('CLASS_TEST'::character varying)::text, ('PRACTICE'::character varying)::text])))
);

CREATE TABLE token_usage_event (
    tokens_consumed numeric(18,6) NOT NULL,
    occurred_at timestamp(6) with time zone NOT NULL,
    exam_session_id uuid NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    subscription_id uuid NOT NULL,
    quota_type character varying(20) NOT NULL,
    CONSTRAINT chk_token_usage_event_quota_type_valid CHECK (((quota_type)::text = ANY (ARRAY[('GRADING'::character varying)::text, ('CLASS_TEST'::character varying)::text, ('PRACTICE'::character varying)::text])))
);

CREATE TABLE topic_interest_event (
    id uuid NOT NULL,
    student_id uuid NOT NULL,
    practice_topic_id uuid NOT NULL,
    practice_session_id uuid,
    event_type character varying(32) NOT NULL,
    signal numeric(4,3) NOT NULL,
    occurred_at timestamp with time zone NOT NULL
);

CREATE TABLE topic_interest_score (
    id uuid NOT NULL,
    student_id uuid NOT NULL,
    practice_topic_id uuid NOT NULL,
    score numeric(5,4) NOT NULL,
    sessions_mentioned integer DEFAULT 0 NOT NULL,
    last_mentioned_at timestamp with time zone,
    computed_at timestamp with time zone NOT NULL
);

CREATE TABLE topic_suggestion (
    id uuid NOT NULL,
    student_id uuid NOT NULL,
    suggested_topic_name character varying(200) NOT NULL,
    keyword character varying(200),
    interest_dimension character varying(32) NOT NULL,
    curriculum_group character varying(24),
    confidence numeric(4,3) NOT NULL,
    reason_text text NOT NULL,
    evidence_json text,
    status character varying(16) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    responded_at timestamp with time zone
);

CREATE TABLE turn_correction (
    id uuid NOT NULL,
    turn_id uuid NOT NULL,
    category character varying(64) NOT NULL,
    original_text text NOT NULL,
    corrected_text text NOT NULL,
    explanation text NOT NULL,
    correct_audio_url text
);

CREATE TABLE user_roles (
    created_at timestamp(6) with time zone NOT NULL,
    id uuid DEFAULT uuidv7() NOT NULL,
    role_id uuid NOT NULL,
    user_id uuid NOT NULL
);

CREATE TABLE users (
    date_of_birth date NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    gender character varying(15),
    created_by uuid,
    id uuid DEFAULT uuidv7() NOT NULL,
    updated_by uuid,
    phone character varying(20),
    status character varying(20) NOT NULL,
    avatar_url character varying(4096),
    address character varying(255),
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    CONSTRAINT chk_users_status_valid CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('INACTIVE'::character varying)::text, ('LOCKED'::character varying)::text, ('DISABLED'::character varying)::text])))
);

ALTER TABLE ONLY ai_usage_record
    ADD CONSTRAINT ai_usage_record_pkey PRIMARY KEY (id);

ALTER TABLE ONLY assessment_policies
    ADD CONSTRAINT assessment_policies_pkey PRIMARY KEY (id);

ALTER TABLE ONLY device_sessions
    ADD CONSTRAINT device_sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY dimension_interest_score
    ADD CONSTRAINT dimension_interest_score_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_blueprint_sections
    ADD CONSTRAINT exam_blueprint_sections_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_blueprint_slots
    ADD CONSTRAINT exam_blueprint_slots_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_blueprint_versions
    ADD CONSTRAINT exam_blueprint_versions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_blueprints
    ADD CONSTRAINT exam_blueprints_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_candidate_results
    ADD CONSTRAINT exam_candidate_results_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_candidates
    ADD CONSTRAINT exam_candidates_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_grading_assignments
    ADD CONSTRAINT exam_grading_assignments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_item_criterion_scores
    ADD CONSTRAINT exam_item_criterion_scores_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_item_evaluation_turns
    ADD CONSTRAINT exam_item_evaluation_turns_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_item_evaluations
    ADD CONSTRAINT exam_item_evaluations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_item_response_turns
    ADD CONSTRAINT exam_item_response_turns_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_item_responses
    ADD CONSTRAINT exam_item_responses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_item_rule_hits
    ADD CONSTRAINT exam_item_rule_hits_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_members
    ADD CONSTRAINT exam_members_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_paper_items
    ADD CONSTRAINT exam_paper_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_paper_sections
    ADD CONSTRAINT exam_paper_sections_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_papers
    ADD CONSTRAINT exam_papers_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_proctoring_alerts
    ADD CONSTRAINT exam_proctoring_alerts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_recordings
    ADD CONSTRAINT exam_recordings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_result_appeal_items
    ADD CONSTRAINT exam_result_appeal_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_result_appeals
    ADD CONSTRAINT exam_result_appeals_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_result_status_histories
    ADD CONSTRAINT exam_result_status_histories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_schedule_proctors
    ADD CONSTRAINT exam_schedule_proctors_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_schedule_proctors
    ADD CONSTRAINT exam_schedule_proctors_schedule_teacher UNIQUE (schedule_id, teacher_id);

ALTER TABLE ONLY exam_schedules
    ADD CONSTRAINT exam_schedules_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_secure_pools
    ADD CONSTRAINT exam_secure_pools_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exam_sessions
    ADD CONSTRAINT exam_sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exams
    ADD CONSTRAINT exams_pkey PRIMARY KEY (id);

ALTER TABLE ONLY exchange_rate_snapshot
    ADD CONSTRAINT exchange_rate_snapshot_pkey PRIMARY KEY (id);

ALTER TABLE ONLY financial_event
    ADD CONSTRAINT financial_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY framework_criteria
    ADD CONSTRAINT framework_criteria_pkey PRIMARY KEY (id);

ALTER TABLE ONLY framework_criterion_bands
    ADD CONSTRAINT framework_criterion_bands_pkey PRIMARY KEY (id);

ALTER TABLE ONLY framework_result_bands
    ADD CONSTRAINT framework_result_bands_pkey PRIMARY KEY (id);

ALTER TABLE ONLY framework_versions
    ADD CONSTRAINT framework_versions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY frameworks
    ADD CONSTRAINT frameworks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY grade_level_band_scopes
    ADD CONSTRAINT grade_level_band_scopes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY grade_levels
    ADD CONSTRAINT grade_levels_pkey PRIMARY KEY (id);

ALTER TABLE ONLY assessment_policies
    ADD CONSTRAINT idx_assessment_policies_scope_version UNIQUE (school_id, grade_level_id, school_grade_id, school_class_id, language_id, framework_version_id, version);

ALTER TABLE ONLY framework_criteria
    ADD CONSTRAINT idx_framework_criteria_version_code UNIQUE (framework_version_id, code);

ALTER TABLE ONLY framework_criterion_bands
    ADD CONSTRAINT idx_framework_criterion_bands_criterion_result UNIQUE (framework_criterion_id, framework_result_band_id);

ALTER TABLE ONLY framework_result_bands
    ADD CONSTRAINT idx_framework_result_bands_version_code UNIQUE (framework_version_id, code);

ALTER TABLE ONLY framework_result_bands
    ADD CONSTRAINT idx_framework_result_bands_version_label UNIQUE (framework_version_id, label);

ALTER TABLE ONLY framework_result_bands
    ADD CONSTRAINT idx_framework_result_bands_version_order UNIQUE (framework_version_id, result_band_order);

ALTER TABLE ONLY framework_versions
    ADD CONSTRAINT idx_framework_versions_framework_code UNIQUE (framework_id, code);

ALTER TABLE ONLY framework_versions
    ADD CONSTRAINT idx_framework_versions_framework_version UNIQUE (framework_id, version);

ALTER TABLE ONLY frameworks
    ADD CONSTRAINT idx_frameworks_code UNIQUE (code);

ALTER TABLE ONLY grade_level_band_scopes
    ADD CONSTRAINT idx_grade_level_band_scopes_level_framework UNIQUE (grade_level_id, framework_version_id);

ALTER TABLE ONLY grade_levels
    ADD CONSTRAINT idx_grade_levels_code UNIQUE (code);

ALTER TABLE ONLY grade_levels
    ADD CONSTRAINT idx_grade_levels_order UNIQUE (grade_level_order);

ALTER TABLE ONLY import_rows
    ADD CONSTRAINT idx_import_rows_session_row_number UNIQUE (session_id, row_number);

ALTER TABLE ONLY supported_languages
    ADD CONSTRAINT idx_language_code UNIQUE (code);

ALTER TABLE ONLY processed_events
    ADD CONSTRAINT idx_processed_events_event_consumer_group UNIQUE (event_id, consumer_group);

ALTER TABLE ONLY question_assets
    ADD CONSTRAINT idx_question_assets_question_question_asset_order UNIQUE (question_id, question_asset_order);

ALTER TABLE ONLY question_bank_grades
    ADD CONSTRAINT idx_question_bank_grades_bank_grade UNIQUE (question_bank_id, school_grade_id);

ALTER TABLE ONLY question_banks
    ADD CONSTRAINT idx_question_banks_language_school_code UNIQUE (owner_type, language_id, school_id, code);

ALTER TABLE ONLY question_collaborators
    ADD CONSTRAINT idx_question_collaborators_question_user UNIQUE (question_id, user_id);

ALTER TABLE ONLY question_evaluation_guides
    ADD CONSTRAINT idx_question_evaluation_guides_question UNIQUE (question_id);

ALTER TABLE ONLY question_topics
    ADD CONSTRAINT idx_question_topics_question_bank_code UNIQUE (question_bank_id, code);

ALTER TABLE ONLY questions
    ADD CONSTRAINT idx_questions_question_topic_code UNIQUE (question_topic_id, code);

ALTER TABLE ONLY refresh_tokens
    ADD CONSTRAINT idx_refresh_token_hash UNIQUE (token_hash);

ALTER TABLE ONLY roles
    ADD CONSTRAINT idx_role_code UNIQUE (code);

ALTER TABLE ONLY rubric_criterions
    ADD CONSTRAINT idx_rubric_criterions_version_code UNIQUE (rubric_version_id, code);

ALTER TABLE ONLY rubric_criterions
    ADD CONSTRAINT idx_rubric_criterions_version_framework_criterion UNIQUE (rubric_version_id, framework_criterion_id);

ALTER TABLE ONLY rubric_criterions
    ADD CONSTRAINT idx_rubric_criterions_version_order UNIQUE (rubric_version_id, criterion_order);

ALTER TABLE ONLY rubric_result_bands
    ADD CONSTRAINT idx_rubric_result_bands_version_id_code UNIQUE (rubric_version_id, code);

ALTER TABLE ONLY rubric_result_bands
    ADD CONSTRAINT idx_rubric_result_bands_version_order UNIQUE (rubric_version_id, result_order);

ALTER TABLE ONLY rubric_versions
    ADD CONSTRAINT idx_rubric_versions_rubric_id_code UNIQUE (rubric_id, code);

ALTER TABLE ONLY rubric_versions
    ADD CONSTRAINT idx_rubric_versions_rubric_id_version UNIQUE (rubric_id, version);

ALTER TABLE ONLY rubrics
    ADD CONSTRAINT idx_rubrics_owner_scope_code UNIQUE (owner_type, school_id, language_id, framework_id, code);

ALTER TABLE ONLY school_classes
    ADD CONSTRAINT idx_school_class_class_school_language UNIQUE (id, school_id, language_id);

ALTER TABLE ONLY school_classes
    ADD CONSTRAINT idx_school_class_code UNIQUE (school_id, code);

ALTER TABLE ONLY school_class_users
    ADD CONSTRAINT idx_school_class_users UNIQUE (user_id, school_class_id);

ALTER TABLE ONLY school_directories
    ADD CONSTRAINT idx_school_directories_code UNIQUE (code);

ALTER TABLE ONLY school_grades
    ADD CONSTRAINT idx_school_grades_school_level_code UNIQUE (school_id, grade_level_id, code);

ALTER TABLE ONLY school_users
    ADD CONSTRAINT idx_school_users_school_user UNIQUE (school_id, user_id);

ALTER TABLE ONLY school_users
    ADD CONSTRAINT idx_school_users_user UNIQUE (user_id);

ALTER TABLE ONLY schools
    ADD CONSTRAINT idx_schools_code UNIQUE (code);

ALTER TABLE ONLY scoring_rules
    ADD CONSTRAINT idx_scoring_rules_policy_code UNIQUE (policy_id, code);

ALTER TABLE ONLY users
    ADD CONSTRAINT idx_user_email UNIQUE (email);

ALTER TABLE ONLY users
    ADD CONSTRAINT idx_user_phone UNIQUE (phone);

ALTER TABLE ONLY user_roles
    ADD CONSTRAINT idx_user_roles UNIQUE (user_id, role_id);

ALTER TABLE ONLY import_rows
    ADD CONSTRAINT import_rows_pkey PRIMARY KEY (id);

ALTER TABLE ONLY import_sessions
    ADD CONSTRAINT import_sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY interest_dimension
    ADD CONSTRAINT interest_dimension_pkey PRIMARY KEY (code);

ALTER TABLE ONLY interest_quiz_item
    ADD CONSTRAINT interest_quiz_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY invoice
    ADD CONSTRAINT invoice_invoice_number_key UNIQUE (invoice_number);

ALTER TABLE ONLY invoice
    ADD CONSTRAINT invoice_pkey PRIMARY KEY (id);

ALTER TABLE ONLY learner_profile
    ADD CONSTRAINT learner_profile_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notification_devices
    ADD CONSTRAINT notification_devices_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notification_preferences
    ADD CONSTRAINT notification_preferences_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY outboxes
    ADD CONSTRAINT outboxes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY password_set_up_tokens
    ADD CONSTRAINT password_set_up_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY plan_quota
    ADD CONSTRAINT plan_quota_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_criterion_score
    ADD CONSTRAINT practice_criterion_score_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_item_evaluation
    ADD CONSTRAINT practice_item_evaluation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_item_response
    ADD CONSTRAINT practice_item_response_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_paper_item
    ADD CONSTRAINT practice_paper_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_paper
    ADD CONSTRAINT practice_paper_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_question
    ADD CONSTRAINT practice_question_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_response_turn
    ADD CONSTRAINT practice_response_turn_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_session
    ADD CONSTRAINT practice_session_pkey PRIMARY KEY (id);

ALTER TABLE ONLY practice_topic
    ADD CONSTRAINT practice_topic_pkey PRIMARY KEY (id);

ALTER TABLE ONLY processed_events
    ADD CONSTRAINT processed_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY question_assets
    ADD CONSTRAINT question_assets_pkey PRIMARY KEY (id);

ALTER TABLE ONLY question_bank_grades
    ADD CONSTRAINT question_bank_grades_pkey PRIMARY KEY (id);

ALTER TABLE ONLY question_banks
    ADD CONSTRAINT question_banks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY question_collaborators
    ADD CONSTRAINT question_collaborators_pkey PRIMARY KEY (id);

ALTER TABLE ONLY question_evaluation_guides
    ADD CONSTRAINT question_evaluation_guides_pkey PRIMARY KEY (id);

ALTER TABLE ONLY question_topics
    ADD CONSTRAINT question_topics_pkey PRIMARY KEY (id);

ALTER TABLE ONLY questions
    ADD CONSTRAINT questions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY quota_pricing_calibration
    ADD CONSTRAINT quota_pricing_calibration_pkey PRIMARY KEY (id);

ALTER TABLE ONLY refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY register_form_documents
    ADD CONSTRAINT register_form_documents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY register_forms
    ADD CONSTRAINT register_forms_pkey PRIMARY KEY (id);

ALTER TABLE ONLY roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rubric_criterions
    ADD CONSTRAINT rubric_criterions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rubric_result_bands
    ADD CONSTRAINT rubric_result_bands_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rubric_versions
    ADD CONSTRAINT rubric_versions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rubrics
    ADD CONSTRAINT rubrics_pkey PRIMARY KEY (id);

ALTER TABLE ONLY saved_topic
    ADD CONSTRAINT saved_topic_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_class_users
    ADD CONSTRAINT school_class_users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_classes
    ADD CONSTRAINT school_classes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_debt_event
    ADD CONSTRAINT school_debt_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_directories
    ADD CONSTRAINT school_directories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_grades
    ADD CONSTRAINT school_grades_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_rooms
    ADD CONSTRAINT school_rooms_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_rooms
    ADD CONSTRAINT school_rooms_school_code UNIQUE (school_id, code);

ALTER TABLE ONLY school_subscription
    ADD CONSTRAINT school_subscription_pkey PRIMARY KEY (id);

ALTER TABLE ONLY school_users
    ADD CONSTRAINT school_users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY schools
    ADD CONSTRAINT schools_pkey PRIMARY KEY (id);

ALTER TABLE ONLY scoring_rules
    ADD CONSTRAINT scoring_rules_pkey PRIMARY KEY (id);

ALTER TABLE ONLY student_question_exposure
    ADD CONSTRAINT student_question_exposure_pkey PRIMARY KEY (id);

ALTER TABLE ONLY subscription_plan
    ADD CONSTRAINT subscription_plan_pkey PRIMARY KEY (id);

ALTER TABLE ONLY subscription_quota
    ADD CONSTRAINT subscription_quota_pkey PRIMARY KEY (id);

ALTER TABLE ONLY subscription_quota_user_allocations
    ADD CONSTRAINT subscription_quota_user_allocations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY subscription_request
    ADD CONSTRAINT subscription_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY supported_languages
    ADD CONSTRAINT supported_languages_pkey PRIMARY KEY (id);

ALTER TABLE ONLY token_purchase_item
    ADD CONSTRAINT token_purchase_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY token_purchase
    ADD CONSTRAINT token_purchase_pkey PRIMARY KEY (id);

ALTER TABLE ONLY token_usage_event
    ADD CONSTRAINT token_usage_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY topic_interest_event
    ADD CONSTRAINT topic_interest_event_pkey PRIMARY KEY (id);

ALTER TABLE ONLY topic_interest_score
    ADD CONSTRAINT topic_interest_score_pkey PRIMARY KEY (id);

ALTER TABLE ONLY topic_suggestion
    ADD CONSTRAINT topic_suggestion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY turn_correction
    ADD CONSTRAINT turn_correction_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ai_usage_record
    ADD CONSTRAINT uk_ai_usage_record_usage_event_id UNIQUE (usage_event_id);

ALTER TABLE ONLY exam_item_response_turns
    ADD CONSTRAINT uk_exam_item_response_turns_response_order UNIQUE (exam_item_response_id, turn_order);

ALTER TABLE ONLY exam_proctoring_alerts
    ADD CONSTRAINT uk_exam_proctoring_alert_event_id UNIQUE (event_id);

ALTER TABLE ONLY notification_devices
    ADD CONSTRAINT uk_notification_devices_installation_id UNIQUE (installation_id);

ALTER TABLE ONLY notification_preferences
    ADD CONSTRAINT uk_notification_preferences_user_category UNIQUE (user_id, category);

ALTER TABLE ONLY notifications
    ADD CONSTRAINT uk_notifications_user_event UNIQUE (user_id, event_id);

ALTER TABLE ONLY question_assets
    ADD CONSTRAINT uk_question_assets_question_id UNIQUE (question_id);

ALTER TABLE ONLY subscription_quota_user_allocations
    ADD CONSTRAINT uk_subscription_quota_user_allocations_subscription_quota_user UNIQUE (subscription_id, quota_type, user_id);

ALTER TABLE ONLY exam_result_appeal_items
    ADD CONSTRAINT uq_appeal_item UNIQUE (appeal_id, paper_item_id);

ALTER TABLE ONLY exam_candidates
    ADD CONSTRAINT uq_exam_candidates_exam_student UNIQUE (exam_id, student_id);

ALTER TABLE ONLY exam_recordings
    ADD CONSTRAINT uq_exam_recordings_session_stream_source UNIQUE (exam_session_id, stream_type, source);

ALTER TABLE ONLY exam_grading_assignments
    ADD CONSTRAINT uq_grading_assignment_active_result UNIQUE (active_result_id);

ALTER TABLE ONLY practice_criterion_score
    ADD CONSTRAINT uq_practice_criterion_score_evaluation_code UNIQUE (practice_evaluation_id, criterion_code);

ALTER TABLE ONLY practice_item_evaluation
    ADD CONSTRAINT uq_practice_evaluation_response UNIQUE (practice_response_id);

ALTER TABLE ONLY practice_paper_item
    ADD CONSTRAINT uq_practice_paper_item_slot UNIQUE (practice_paper_id, slot_order);

ALTER TABLE ONLY practice_item_response
    ADD CONSTRAINT uq_practice_response_session_question UNIQUE (practice_session_id, practice_question_id);

ALTER TABLE ONLY practice_response_turn
    ADD CONSTRAINT uq_practice_response_turn_order UNIQUE (practice_response_id, turn_order);

ALTER TABLE ONLY practice_topic
    ADD CONSTRAINT uq_practice_topic_source_question_topic UNIQUE (source_question_topic_id);

ALTER TABLE ONLY saved_topic
    ADD CONSTRAINT uq_saved_topic_student_topic UNIQUE (student_id, practice_topic_id);

ALTER TABLE ONLY student_question_exposure
    ADD CONSTRAINT uq_student_question_exposure_student_question UNIQUE (student_id, practice_question_id);

ALTER TABLE ONLY topic_interest_score
    ADD CONSTRAINT uq_topic_interest_score_student_topic UNIQUE (student_id, practice_topic_id);

ALTER TABLE ONLY user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX idx_ai_usage_record_exam_session ON ai_usage_record USING btree (exam_session_id);

CREATE INDEX idx_ai_usage_record_turn ON ai_usage_record USING btree (turn_id);

CREATE INDEX idx_assessment_policies_grade_status ON assessment_policies USING btree (school_id, grade_level_id, school_grade_id, language_id, framework_version_id, status);

CREATE INDEX idx_assessment_policies_rubric_version ON assessment_policies USING btree (rubric_version_id);

CREATE INDEX idx_assessment_policies_target_band ON assessment_policies USING btree (target_framework_band_id);

CREATE INDEX idx_device_sessions_users ON device_sessions USING btree (user_id);

CREATE UNIQUE INDEX idx_dimension_interest_profile_dimension ON dimension_interest_score USING btree (learner_profile_id, dimension);

CREATE INDEX idx_exam_candidates_schedule_id ON exam_candidates USING btree (schedule_id) WHERE (schedule_id IS NOT NULL);

CREATE INDEX idx_exam_candidates_student_id ON exam_candidates USING btree (student_id) WHERE (schedule_id IS NOT NULL);

CREATE INDEX idx_exam_item_response_turns_response ON exam_item_response_turns USING btree (exam_item_response_id);

CREATE INDEX idx_exam_item_response_turns_response_order ON exam_item_response_turns USING btree (exam_item_response_id, turn_order);

CREATE INDEX idx_exam_proctoring_alert_candidate ON exam_proctoring_alerts USING btree (candidate_id, captured_at);

CREATE INDEX idx_exam_proctoring_alert_session ON exam_proctoring_alerts USING btree (exam_session_id, captured_at);

CREATE INDEX idx_exam_result_appeal_items_appeal ON exam_result_appeal_items USING btree (appeal_id);

CREATE INDEX idx_exam_result_appeals_candidate_result ON exam_result_appeals USING btree (candidate_result_id);

CREATE INDEX idx_exam_result_appeals_status ON exam_result_appeals USING btree (status);

CREATE INDEX idx_exam_schedule_proctors_teacher_id ON exam_schedule_proctors USING btree (teacher_id);

CREATE INDEX idx_exam_schedules_active_window ON exam_schedules USING btree (start_date, end_date) WHERE ((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('PUBLISHED'::character varying)::text]));

CREATE INDEX idx_exchange_rate_snapshot_fetched_at ON exchange_rate_snapshot USING btree (fetched_at DESC);

CREATE INDEX idx_framework_criterion_bands_result ON framework_criterion_bands USING btree (framework_result_band_id);

CREATE INDEX idx_framework_versions_status ON framework_versions USING btree (status);

CREATE INDEX idx_frameworks_active ON frameworks USING btree (is_active);

CREATE INDEX idx_grade_level_band_scopes_default_band ON grade_level_band_scopes USING btree (default_target_band_id);

CREATE INDEX idx_grade_level_band_scopes_hard_max_band ON grade_level_band_scopes USING btree (hard_max_band_id);

CREATE INDEX idx_grading_assignments_appeal ON exam_grading_assignments USING btree (appeal_id);

CREATE INDEX idx_grading_assignments_result ON exam_grading_assignments USING btree (candidate_result_id);

CREATE INDEX idx_grading_assignments_status_deadline ON exam_grading_assignments USING btree (status, deadline_at);

CREATE INDEX idx_grading_assignments_teacher_status ON exam_grading_assignments USING btree (teacher_id, status);

CREATE INDEX idx_import_rows_session_status ON import_rows USING btree (session_id, status);

CREATE INDEX idx_import_sessions_expires_at ON import_sessions USING btree (expires_at);

CREATE INDEX idx_import_sessions_school_created_at ON import_sessions USING btree (school_id, created_at);

CREATE INDEX idx_import_sessions_school_type_status ON import_sessions USING btree (school_id, type, status);

CREATE INDEX idx_interest_dimension_active_quiz ON interest_dimension USING btree (active, quiz_eligible);

CREATE INDEX idx_interest_quiz_item_student ON interest_quiz_item USING btree (student_id) WHERE (student_id IS NOT NULL);

CREATE UNIQUE INDEX idx_invoice_provider_order_ref ON invoice USING btree (payment_provider, provider_order_ref);

CREATE INDEX idx_invoice_school ON invoice USING btree (school_id);

CREATE INDEX idx_language_name ON supported_languages USING btree (name);

CREATE UNIQUE INDEX idx_learner_profile_student ON learner_profile USING btree (student_id);

CREATE INDEX idx_notification_devices_user ON notification_devices USING btree (user_id);

CREATE INDEX idx_notifications_user_created_at ON notifications USING btree (user_id, created_at);

CREATE INDEX idx_notifications_user_id_desc ON notifications USING btree (user_id, id DESC);

CREATE INDEX idx_notifications_user_unread ON notifications USING btree (user_id) WHERE (read_at IS NULL);

CREATE INDEX idx_outboxes_pending ON outboxes USING btree (status, next_retry_at, created_at);

CREATE INDEX idx_password_user_token ON password_set_up_tokens USING btree (user_id, token_hash);

CREATE INDEX idx_practice_criterion_score_evaluation ON practice_criterion_score USING btree (practice_evaluation_id);

CREATE INDEX idx_practice_evaluation_time ON practice_item_evaluation USING btree (evaluated_at);

CREATE INDEX idx_practice_item_response_grading_requested ON practice_item_response USING btree (practice_session_id, grading_status, grading_requested_at);

CREATE INDEX idx_practice_paper_item_paper ON practice_paper_item USING btree (practice_paper_id);

CREATE INDEX idx_practice_paper_student_created ON practice_paper USING btree (student_id, created_at);

CREATE INDEX idx_practice_paper_target_band ON practice_paper USING btree (target_framework_band_id);

CREATE INDEX idx_practice_question_lookup ON practice_question USING btree (practice_topic_id, target_criterion_code, difficulty_rank, active);

CREATE INDEX idx_practice_question_sub_attribute ON practice_question USING btree (target_sub_attribute);

CREATE INDEX idx_practice_question_type ON practice_question USING btree (practice_topic_id, question_type, difficulty_rank);

CREATE INDEX idx_practice_response_session ON practice_item_response USING btree (practice_session_id);

CREATE INDEX idx_practice_response_session_complete ON practice_item_response USING btree (practice_session_id, question_complete);

CREATE INDEX idx_practice_session_heartbeat ON practice_session USING btree (status, last_heartbeat_at);

CREATE INDEX idx_practice_session_student_started ON practice_session USING btree (student_id, started_at);

CREATE INDEX idx_practice_topic_dimension_active ON practice_topic USING btree (interest_dimension, active);

CREATE UNIQUE INDEX idx_practice_topic_normalized_name ON practice_topic USING btree (normalized_name);

CREATE INDEX idx_practice_turn_response ON practice_response_turn USING btree (practice_response_id);

CREATE INDEX idx_question_assets_question ON question_assets USING btree (question_id);

CREATE INDEX idx_question_bank_grades_school_grade ON question_bank_grades USING btree (school_grade_id);

CREATE INDEX idx_question_collaborators_user ON question_collaborators USING btree (user_id);

CREATE INDEX idx_questions_sharing ON questions USING btree (sharing);

CREATE INDEX idx_questions_source_question ON questions USING btree (source_question_id);

CREATE INDEX idx_questions_type ON questions USING btree (type);

CREATE INDEX idx_quota_pricing_calibration_source_computed_at ON quota_pricing_calibration USING btree (pricing_source, computed_at DESC);

CREATE INDEX idx_refresh_token_sessions ON refresh_tokens USING btree (session_id);

CREATE INDEX idx_register_email ON register_forms USING btree (contact_email);

CREATE INDEX idx_register_form_documents_register_form ON register_form_documents USING btree (register_form_id);

CREATE INDEX idx_register_identity ON register_forms USING btree (identity_number);

CREATE INDEX idx_register_phone ON register_forms USING btree (contact_phone);

CREATE INDEX idx_result_status_histories_result_time ON exam_result_status_histories USING btree (candidate_result_id, created_at);

CREATE INDEX idx_role_name ON roles USING btree (name);

CREATE INDEX idx_rubric_criterions_framework_criterion ON rubric_criterions USING btree (framework_criterion_id);

CREATE INDEX idx_rubric_versions_source ON rubric_versions USING btree (source_rubric_version_id) WHERE (source_rubric_version_id IS NOT NULL);

CREATE INDEX idx_rubric_versions_status ON rubric_versions USING btree (status);

CREATE INDEX idx_rubrics_language_framework ON rubrics USING btree (language_id, framework_id);

CREATE INDEX idx_saved_topic_student ON saved_topic USING btree (student_id);

CREATE INDEX idx_school_class_name ON school_classes USING btree (school_id, name);

CREATE INDEX idx_school_class_users_user_id ON school_class_users USING btree (user_id);

CREATE INDEX idx_school_debt_event_school ON school_debt_event USING btree (school_id, occurred_at);

CREATE INDEX idx_school_debt_event_subscription ON school_debt_event USING btree (subscription_id, occurred_at);

CREATE INDEX idx_school_grades_level ON school_grades USING btree (grade_level_id);

CREATE INDEX idx_school_grades_school ON school_grades USING btree (school_id);

CREATE INDEX idx_schools_name ON schools USING btree (name);

CREATE INDEX idx_scoring_rules_policy_active ON scoring_rules USING btree (policy_id, is_active);

CREATE INDEX idx_scoring_rules_policy_priority ON scoring_rules USING btree (policy_id, priority);

CREATE INDEX idx_student_question_exposure_student_seen ON student_question_exposure USING btree (student_id, seen_at);

CREATE INDEX idx_topic_interest_event_student_time ON topic_interest_event USING btree (student_id, occurred_at);

CREATE INDEX idx_topic_interest_score_student ON topic_interest_score USING btree (student_id);

CREATE INDEX idx_topic_suggestion_student_name ON topic_suggestion USING btree (student_id, suggested_topic_name);

CREATE INDEX idx_topic_suggestion_student_status ON topic_suggestion USING btree (student_id, status);

CREATE INDEX idx_turn_correction_turn ON turn_correction USING btree (turn_id);

CREATE INDEX idx_user_roles_role_id ON user_roles USING btree (role_id);

ALTER TABLE ONLY grade_level_band_scopes
    ADD CONSTRAINT fk_grade_level_band_scopes_default_band FOREIGN KEY (default_target_band_id) REFERENCES framework_result_bands(id);

ALTER TABLE ONLY grade_level_band_scopes
    ADD CONSTRAINT fk_grade_level_band_scopes_grade_level FOREIGN KEY (grade_level_id) REFERENCES grade_levels(id);

ALTER TABLE ONLY grade_level_band_scopes
    ADD CONSTRAINT fk_grade_level_band_scopes_hard_max_band FOREIGN KEY (hard_max_band_id) REFERENCES framework_result_bands(id);

ALTER TABLE ONLY invoice
    ADD CONSTRAINT fk_invoice_resolved_plan FOREIGN KEY (resolved_plan_id) REFERENCES subscription_plan(id);

ALTER TABLE ONLY practice_topic
    ADD CONSTRAINT fk_practice_topic_source_question_topic FOREIGN KEY (source_question_topic_id) REFERENCES question_topics(id);

ALTER TABLE ONLY subscription_plan
    ADD CONSTRAINT fk_subscription_plan_replaced_by FOREIGN KEY (replaced_by_plan_id) REFERENCES subscription_plan(id);

