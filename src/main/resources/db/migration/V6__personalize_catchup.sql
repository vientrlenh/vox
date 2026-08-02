-- V1/V2 giữ khớp byte-for-byte với nhánh default -- toàn bộ schema personalize (chưa từng có
-- migration nào, kể cả V1 baseline) dồn vào đây. Đối chiếu trực tiếp từng cột với JPA entity
-- tương ứng dưới infrastructure/persistence/entity/, không dùng ddl-auto: update sinh hộ.
-- Tên bảng KHÔNG có tiền tố "new_" (đã đổi khỏi cả entity lẫn native SQL cùng đợt).

-- ---------------------------------------------------------------------------
-- 1. practice_topic
-- ---------------------------------------------------------------------------
CREATE TABLE practice_topic (
    id UUID DEFAULT uuidv7() NOT NULL,
    name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    description TEXT,
    source VARCHAR(24) NOT NULL,
    interest_dimension VARCHAR(32) NOT NULL,
    curriculum_group VARCHAR(24) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_practice_topic_normalized_name ON practice_topic (normalized_name);
CREATE INDEX idx_practice_topic_dimension_active ON practice_topic (interest_dimension, active);

-- ---------------------------------------------------------------------------
-- 2. practice_question
-- ---------------------------------------------------------------------------
CREATE TABLE practice_question (
    id UUID DEFAULT uuidv7() NOT NULL,
    practice_topic_id UUID NOT NULL,
    question_text TEXT NOT NULL,
    target_criterion_code VARCHAR(32) NOT NULL,
    target_sub_attribute VARCHAR(64),
    difficulty_rank INTEGER NOT NULL,
    difficulty_features_json TEXT NOT NULL,
    evaluation_guide_json TEXT NOT NULL,
    suggested_ideas_json TEXT,
    preparation_time_seconds INTEGER NOT NULL,
    max_response_seconds INTEGER NOT NULL,
    max_followup_seconds INTEGER NOT NULL,
    vstep_part INTEGER,
    source VARCHAR(24) NOT NULL DEFAULT 'AI_GENERATED',
    usage_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_practice_question_difficulty_rank CHECK (difficulty_rank BETWEEN 1 AND 6),
    CONSTRAINT chk_practice_question_time_budgets
        CHECK (preparation_time_seconds >= 0 AND max_response_seconds > 0 AND max_followup_seconds >= 0)
);
CREATE INDEX idx_practice_question_lookup
    ON practice_question (practice_topic_id, target_criterion_code, difficulty_rank, active);
CREATE INDEX idx_practice_question_sub_attribute ON practice_question (target_sub_attribute);

-- ---------------------------------------------------------------------------
-- 3. student_question_exposure
-- ---------------------------------------------------------------------------
CREATE TABLE student_question_exposure (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    practice_question_id UUID NOT NULL,
    seen_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_student_question_exposure_student_question UNIQUE (student_id, practice_question_id)
);
CREATE INDEX idx_student_question_exposure_student_seen ON student_question_exposure (student_id, seen_at);

-- ---------------------------------------------------------------------------
-- 4. practice_paper
-- ---------------------------------------------------------------------------
CREATE TABLE practice_paper (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    practice_topic_id UUID NOT NULL,
    origin VARCHAR(24) NOT NULL,
    goal_type VARCHAR(24) NOT NULL,
    offered_topic_ids_json TEXT NOT NULL,
    previous_offered_topic_ids_json TEXT NOT NULL,
    planned_seconds INTEGER NOT NULL,
    reserved_quota_seconds INTEGER NOT NULL,
    reservation_expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_practice_paper_student_created ON practice_paper (student_id, created_at);

-- ---------------------------------------------------------------------------
-- 5. practice_paper_item
-- ---------------------------------------------------------------------------
CREATE TABLE practice_paper_item (
    id UUID NOT NULL,
    practice_paper_id UUID NOT NULL,
    practice_question_id UUID NOT NULL,
    slot_order INTEGER NOT NULL,
    target_criterion_code VARCHAR(32) NOT NULL,
    target_sub_attribute VARCHAR(64),
    target_difficulty_rank INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_practice_paper_item_slot UNIQUE (practice_paper_id, slot_order)
);
CREATE INDEX idx_practice_paper_item_paper ON practice_paper_item (practice_paper_id);

-- ---------------------------------------------------------------------------
-- 6. practice_session
-- ---------------------------------------------------------------------------
CREATE TABLE practice_session (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    practice_paper_id UUID NOT NULL,
    rubric_version_id UUID NOT NULL,
    target_framework_band_id UUID NOT NULL,
    chosen_practice_topic_id UUID NOT NULL,
    target_sub_attributes_json TEXT,
    origin VARCHAR(24) NOT NULL,
    offered_topic_ids_json TEXT,
    overall_score NUMERIC(5,2),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    last_heartbeat_at TIMESTAMPTZ,
    graded_seconds INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    abandon_diagnosis VARCHAR(16),
    help_request_count INTEGER NOT NULL DEFAULT 0,
    long_pause_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_practice_session_student_started ON practice_session (student_id, started_at);
CREATE INDEX idx_practice_session_heartbeat ON practice_session (status, last_heartbeat_at);

-- ---------------------------------------------------------------------------
-- 7. practice_item_response
-- ---------------------------------------------------------------------------
CREATE TABLE practice_item_response (
    id UUID NOT NULL,
    practice_session_id UUID NOT NULL,
    practice_question_id UUID NOT NULL,
    audio_url TEXT,
    transcript TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uq_practice_response_session_question UNIQUE (practice_session_id, practice_question_id)
);
CREATE INDEX idx_practice_response_session ON practice_item_response (practice_session_id);

-- ---------------------------------------------------------------------------
-- 8. practice_response_turn
-- ---------------------------------------------------------------------------
CREATE TABLE practice_response_turn (
    id UUID NOT NULL,
    practice_response_id UUID NOT NULL,
    turn_order INTEGER NOT NULL,
    turn_type VARCHAR(24) NOT NULL,
    prompt_text TEXT,
    audio_url TEXT,
    transcript TEXT,
    duration_seconds INTEGER NOT NULL,
    word_feedback_json TEXT,
    turn_score NUMERIC(5,2),
    PRIMARY KEY (id),
    CONSTRAINT uq_practice_response_turn_order UNIQUE (practice_response_id, turn_order)
);
CREATE INDEX idx_practice_turn_response ON practice_response_turn (practice_response_id);

-- ---------------------------------------------------------------------------
-- 9. practice_item_evaluation
-- ---------------------------------------------------------------------------
CREATE TABLE practice_item_evaluation (
    id UUID NOT NULL,
    practice_response_id UUID NOT NULL,
    item_score NUMERIC(5,2),
    marked_invalid BOOLEAN NOT NULL DEFAULT FALSE,
    evaluated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_practice_evaluation_response UNIQUE (practice_response_id)
);
CREATE INDEX idx_practice_evaluation_time ON practice_item_evaluation (evaluated_at);

-- ---------------------------------------------------------------------------
-- 10. practice_criterion_score
-- ---------------------------------------------------------------------------
CREATE TABLE practice_criterion_score (
    id UUID NOT NULL,
    practice_evaluation_id UUID NOT NULL,
    rubric_criterion_id UUID NOT NULL,
    final_score NUMERIC(7,3),
    matched_band_code VARCHAR(64),
    PRIMARY KEY (id),
    CONSTRAINT uq_practice_criterion_score_evaluation_criterion UNIQUE (practice_evaluation_id, rubric_criterion_id)
);
CREATE INDEX idx_practice_criterion_score_evaluation ON practice_criterion_score (practice_evaluation_id);

-- ---------------------------------------------------------------------------
-- 11. turn_correction
-- ---------------------------------------------------------------------------
CREATE TABLE turn_correction (
    id UUID NOT NULL,
    turn_id UUID NOT NULL,
    weakness_observation_id UUID,
    category VARCHAR(64) NOT NULL,
    original_text TEXT NOT NULL,
    corrected_text TEXT NOT NULL,
    explanation TEXT NOT NULL,
    correct_audio_url TEXT,
    PRIMARY KEY (id)
);
CREATE INDEX idx_turn_correction_turn ON turn_correction (turn_id);

-- ---------------------------------------------------------------------------
-- 12. learner_profile
-- ---------------------------------------------------------------------------
CREATE TABLE learner_profile (
    id UUID DEFAULT uuidv7() NOT NULL,
    student_id UUID NOT NULL,
    version INTEGER NOT NULL,
    goal_type VARCHAR(24),
    target_exam VARCHAR(24),
    target_date DATE,
    flsa_score NUMERIC(5,2),
    flsa_raw_answers_json TEXT,
    auto_update_interest BOOLEAN NOT NULL DEFAULT TRUE,
    quiz_completed_at TIMESTAMPTZ,
    recorded_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_learner_profile_student_version ON learner_profile (student_id, version);

-- ---------------------------------------------------------------------------
-- 13. learner_weakness_snapshot
-- ---------------------------------------------------------------------------
CREATE TABLE learner_weakness_snapshot (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    framework_criterion_id UUID NOT NULL,
    rel_estimate NUMERIC(6,4) NOT NULL,
    weakness NUMERIC(6,4) NOT NULL,
    observation_count INTEGER NOT NULL,
    reliable BOOLEAN NOT NULL DEFAULT FALSE,
    computed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_learner_weakness_snapshot_student_criterion UNIQUE (student_id, framework_criterion_id)
);
CREATE INDEX idx_learner_weakness_snapshot_student ON learner_weakness_snapshot (student_id);

-- ---------------------------------------------------------------------------
-- 14. sub_attribute_priority
-- ---------------------------------------------------------------------------
CREATE TABLE sub_attribute_priority (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    framework_criterion_id UUID NOT NULL,
    sub_attribute VARCHAR(64) NOT NULL,
    freq INTEGER NOT NULL DEFAULT 0,
    recent_freq INTEGER NOT NULL DEFAULT 0,
    priority NUMERIC(6,4) NOT NULL,
    practiceable BOOLEAN NOT NULL DEFAULT FALSE,
    computed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_sub_attribute_priority_student_criterion_attribute
        UNIQUE (student_id, framework_criterion_id, sub_attribute)
);
CREATE INDEX idx_sub_attribute_priority_student ON sub_attribute_priority (student_id);

-- ---------------------------------------------------------------------------
-- 15. topic_interest_event
-- ---------------------------------------------------------------------------
CREATE TABLE topic_interest_event (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    practice_topic_id UUID NOT NULL,
    practice_session_id UUID,
    event_type VARCHAR(32) NOT NULL,
    signal NUMERIC(4,3) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_topic_interest_event_student_time ON topic_interest_event (student_id, occurred_at);

-- ---------------------------------------------------------------------------
-- 16. topic_interest_score
-- ---------------------------------------------------------------------------
CREATE TABLE topic_interest_score (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    practice_topic_id UUID NOT NULL,
    score NUMERIC(5,4) NOT NULL,
    sessions_mentioned INTEGER NOT NULL DEFAULT 0,
    last_mentioned_at TIMESTAMPTZ,
    computed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_topic_interest_score_student_topic UNIQUE (student_id, practice_topic_id)
);
CREATE INDEX idx_topic_interest_score_student ON topic_interest_score (student_id);

-- ---------------------------------------------------------------------------
-- 17. dimension_interest_score
-- ---------------------------------------------------------------------------
CREATE TABLE dimension_interest_score (
    id UUID DEFAULT uuidv7() NOT NULL,
    learner_profile_id UUID NOT NULL,
    dimension VARCHAR(32) NOT NULL,
    score NUMERIC(5,4) NOT NULL,
    baseline_score NUMERIC(5,4),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_dimension_interest_profile_dimension ON dimension_interest_score (learner_profile_id, dimension);

-- ---------------------------------------------------------------------------
-- 18. topic_suggestion
-- ---------------------------------------------------------------------------
CREATE TABLE topic_suggestion (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    suggested_topic_name VARCHAR(200) NOT NULL,
    keyword VARCHAR(200),
    interest_dimension VARCHAR(32) NOT NULL,
    curriculum_group VARCHAR(24),
    confidence NUMERIC(4,3) NOT NULL,
    reason_text TEXT NOT NULL,
    evidence_json TEXT,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    PRIMARY KEY (id)
);
CREATE INDEX idx_topic_suggestion_student_status ON topic_suggestion (student_id, status);
CREATE INDEX idx_topic_suggestion_student_name ON topic_suggestion (student_id, suggested_topic_name);

-- ---------------------------------------------------------------------------
-- 19. class_topic_scope
-- ---------------------------------------------------------------------------
CREATE TABLE class_topic_scope (
    id UUID NOT NULL,
    school_class_id UUID NOT NULL,
    practice_topic_id UUID NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_class_topic_scope_class_topic UNIQUE (school_class_id, practice_topic_id)
);
CREATE INDEX idx_class_topic_scope_class ON class_topic_scope (school_class_id);

-- ---------------------------------------------------------------------------
-- 20. saved_topic
-- ---------------------------------------------------------------------------
CREATE TABLE saved_topic (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    practice_topic_id UUID NOT NULL,
    saved_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_saved_topic_student_topic UNIQUE (student_id, practice_topic_id)
);
CREATE INDEX idx_saved_topic_student ON saved_topic (student_id);

-- ---------------------------------------------------------------------------
-- 21. weakness_observation (gói 1-2, cũng chưa từng có migration)
-- ---------------------------------------------------------------------------
CREATE TABLE weakness_observation (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    source_evaluation_id UUID NOT NULL,
    framework_criterion_id UUID NOT NULL,
    criterion_code VARCHAR(32) NOT NULL,
    sub_attribute VARCHAR(64) NOT NULL,
    evidence_span VARCHAR(200) NOT NULL DEFAULT '',
    observed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_weakness_observation_source
        UNIQUE (source_evaluation_id, framework_criterion_id, sub_attribute, evidence_span)
);
CREATE INDEX idx_weakness_observation_student_criterion_time
    ON weakness_observation (student_id, framework_criterion_id, observed_at);

-- ---------------------------------------------------------------------------
-- 22. interest_quiz_item -- bảng gốc bị thiếu khỏi baseline dù entity/seed
--     (InterestQuizSeedInitializer)/submitQuiz đã dùng từ trước. student_id: NULL = bộ tĩnh
--     gốc dùng chung (fallback); có giá trị = sinh riêng cho đúng học sinh này (gói 13, xem
--     task/implement/13-quiz-so-thich-sinh-theo-tinh-huong.md).
-- ---------------------------------------------------------------------------
CREATE TABLE interest_quiz_item (
    id UUID DEFAULT uuidv7() NOT NULL,
    dimensions_json TEXT NOT NULL,
    statements_json TEXT NOT NULL,
    desirability_note VARCHAR(512),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    student_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_interest_quiz_item_student ON interest_quiz_item (student_id) WHERE student_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 23. V1 thiếu SCHOOL_ROOM/RUBRIC_RESULT_BAND trong danh sách import_sessions.type cho phép
--     (dù ImportType.java + các use case import phòng học/rubric result band đã dùng từ
--     trước, không liên quan personalize) -- và rubric_result_bands.score_min/score_max đang
--     numeric(6,2) cần nới thành numeric cho personalize. Độc lập xác nhận bởi migration
--     "grading_rework_and_baseline_catchup" của nhánh khác (chưa merge vào đây) cũng tự vá
--     đúng 2 chỗ này.
-- ---------------------------------------------------------------------------
ALTER TABLE import_sessions
    DROP CONSTRAINT IF EXISTS chk_import_sessions_type_valid;

ALTER TABLE import_sessions
    ADD CONSTRAINT chk_import_sessions_type_valid
    CHECK (
        type IN (
            'USER',
            'SCHOOL_CLASS',
            'SCHOOL_CLASS_USER',
            'QUESTION',
            'SCHOOL_DIRECTORY',
            'SCHOOL_GRADE_LEVEL',
            'SCHOOL_GRADE',
            'RUBRIC_VERSION',
            'RUBRIC_CRITERION',
            'ASSESSMENT_POLICY',
            'SCHOOL_ROOM',
            'RUBRIC_RESULT_BAND'
        )
    );

ALTER TABLE rubric_result_bands
    ALTER COLUMN score_min TYPE numeric,
    ALTER COLUMN score_max TYPE numeric;
