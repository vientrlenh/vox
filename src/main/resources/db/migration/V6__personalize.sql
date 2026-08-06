-- ===========================================================================
-- Toàn bộ schema personalize, gộp từ 12 migration rời (V6..V18) thành một file.
--
-- Đây là TRẠNG THÁI CUỐI, không phải lịch sử phát lại: những cột từng được thêm rồi bỏ
-- (preparation_time_seconds, max_followup_seconds, recent_freq), những bảng từng tạo rồi drop
-- (class_topic_scope, learner_weakness_snapshot, weakness_observation, sub_attribute_priority),
-- và cột turn_correction.weakness_observation_id -- đều KHÔNG xuất hiện ở đây. Tạo rồi xoá
-- trong cùng một file chỉ làm dài thêm mà kết quả y hệt.
--
-- Đối chiếu từng cột với JPA entity dưới infrastructure/persistence/entity/, không dùng
-- ddl-auto: update sinh hộ (ddl-auto đang là `validate`, xem application.yaml).
--
-- Lý do gộp và cái giá của nó: xem chú thích ở cuối file.
-- ===========================================================================


-- ---------------------------------------------------------------------------
-- 1. practice_topic
--
-- source_question_topic_id: EXAM_PREP lấy topic từ ngân hàng câu hỏi của trường
-- (question_topics, gắn khối qua question_bank_grades). Mỗi topic được vật chất hoá lazy
-- thành 1 dòng ở đây khi lần đầu xuất hiện trong danh sách gợi ý, liên kết ngược qua cột này
-- để pipeline chọn câu / vector sở thích chạy nguyên vẹn.
--
-- temporal_affordance: chủ đề này tự nhiên gọi ra khung thời gian nào (PAST / FUTURE / MIXED),
-- do topicGenerationGraph gắn lúc soạn. Java đọc để quyết thì đích cho từng câu -- xem
-- TensePolicy.forSlot. NULL coi như MIXED.
-- ---------------------------------------------------------------------------
CREATE TABLE practice_topic (
    id UUID DEFAULT uuidv7() NOT NULL,
    name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    description TEXT,
    source VARCHAR(24) NOT NULL,
    interest_dimension VARCHAR(32) NOT NULL,
    curriculum_group VARCHAR(24) NOT NULL,
    source_question_topic_id UUID,
    temporal_affordance VARCHAR(8),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_practice_topic_source_question_topic UNIQUE (source_question_topic_id),
    CONSTRAINT fk_practice_topic_source_question_topic
        FOREIGN KEY (source_question_topic_id) REFERENCES question_topics (id)
);
CREATE UNIQUE INDEX idx_practice_topic_normalized_name ON practice_topic (normalized_name);
CREATE INDEX idx_practice_topic_dimension_active ON practice_topic (interest_dimension, active);


-- ---------------------------------------------------------------------------
-- 2. practice_question
--
-- difficulty_rank BETWEEN 1 AND 20, KHÔNG phải 1..6: bảng dùng chung cho mọi trường mà mỗi
-- trường áp framework khác nhau (VSTEP/CEFR 6 bậc, IELTS 9...). Nới chứ không bỏ hẳn -- vẫn
-- chặn rác (0, âm, 999 do lỗi ánh xạ). Trần đúng theo từng học sinh do tầng ứng dụng lo, xem
-- PracticeTopicOfferEnrichmentService.frameworkBandCount.
--
-- Thời lượng là một SÀN và một TRẦN, đúng hình dạng của đề thi. min_response_seconds là mốc
-- "nói bao nhiêu là đủ" -- thiếu nó thì bên Python lấy TRẦN làm mốc, khiến một câu trả lời
-- trọn vẹn 18 giây trên trần 45 giây bị đọc thành "mới đạt 0.4".
--
-- target_tense: thì mà câu này ép học sinh dùng (PRESENT/PAST/FUTURE/PERFECT/CONDITIONAL).
-- NULL = CHƯA BIẾT (câu soạn trước khi có cột này), không phải "không ép thì nào" -- thang leo
-- chọn câu cố ý coi NULL là dùng được.
-- ---------------------------------------------------------------------------
CREATE TABLE practice_question (
    id UUID DEFAULT uuidv7() NOT NULL,
    practice_topic_id UUID NOT NULL,
    question_text TEXT NOT NULL,
    target_criterion_code VARCHAR(32) NOT NULL,
    target_sub_attribute VARCHAR(64),
    target_tense VARCHAR(16),
    difficulty_rank INTEGER NOT NULL,
    difficulty_features_json TEXT NOT NULL,
    evaluation_guide_json TEXT NOT NULL,
    suggested_ideas_json TEXT,
    question_type VARCHAR(24) NOT NULL,
    min_response_seconds INTEGER NOT NULL,
    max_response_seconds INTEGER NOT NULL,
    vstep_part INTEGER,
    source VARCHAR(24) NOT NULL DEFAULT 'AI_GENERATED',
    usage_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_practice_question_difficulty_rank CHECK (difficulty_rank BETWEEN 1 AND 20),
    CONSTRAINT chk_practice_question_time_budgets
        CHECK (min_response_seconds > 0 AND max_response_seconds > min_response_seconds),
    -- Bốn dạng, KHÔNG có READ_ALOUD: dạng đó cần văn bản mẫu để đọc theo, mà luyện nói tự do
    -- thì không có văn bản nào cả.
    CONSTRAINT chk_practice_question_type
        CHECK (question_type IN ('SHORT_ANSWER', 'LONG_ANSWER', 'DESCRIPTION', 'OPINION'))
);
CREATE INDEX idx_practice_question_lookup
    ON practice_question (practice_topic_id, target_criterion_code, difficulty_rank, active);
CREATE INDEX idx_practice_question_sub_attribute ON practice_question (target_sub_attribute);
CREATE INDEX idx_practice_question_type
    ON practice_question (practice_topic_id, question_type, difficulty_rank);


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
--
-- target_framework_band_id CỐ Ý để NULLABLE: đề dựng trước lần đổi "học sinh tự chọn bậc"
-- không mang lựa chọn nào, và bịa ra một bậc cho chúng là ghi dữ liệu sai.
-- StartPracticeSessionPersistenceService gặp NULL thì lùi về bậc mục tiêu của chính sách chấm.
-- ---------------------------------------------------------------------------
CREATE TABLE practice_paper (
    id UUID NOT NULL,
    student_id UUID NOT NULL,
    practice_topic_id UUID NOT NULL,
    target_framework_band_id UUID,
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
CREATE INDEX idx_practice_paper_target_band ON practice_paper (target_framework_band_id);


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
--
-- question_complete: "câu này đã trả lời xong chưa" là sự thật hệ thống biết ngay lúc nộp lượt
-- cuối. Không lưu thì hỏng hai chỗ: không phân biệt được "đang chờ chấm" với "sẽ không bao giờ
-- được chấm" (màn tổng kết không biết nên đợi hay thôi), và điểm phiên không loại được câu dở
-- dang nên học sinh rớt mạng giữa câu bị chấm như đã trả lời đầy đủ.
-- ---------------------------------------------------------------------------
CREATE TABLE practice_item_response (
    id UUID NOT NULL,
    practice_session_id UUID NOT NULL,
    practice_question_id UUID NOT NULL,
    audio_url TEXT,
    transcript TEXT,
    question_complete BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uq_practice_response_session_question UNIQUE (practice_session_id, practice_question_id)
);
CREATE INDEX idx_practice_response_session ON practice_item_response (practice_session_id);
CREATE INDEX idx_practice_response_session_complete
    ON practice_item_response (practice_session_id, question_complete);


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
--
-- Nguồn của thẻ "sửa ngay" lúc đang nói và của mục "lỗi lặp lại" ở màn tổng kết buổi luyện.
-- ---------------------------------------------------------------------------
CREATE TABLE turn_correction (
    id UUID NOT NULL,
    turn_id UUID NOT NULL,
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
-- 13. topic_interest_event
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
-- 14. topic_interest_score
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
-- 15. dimension_interest_score
-- ---------------------------------------------------------------------------
CREATE TABLE dimension_interest_score (
    id UUID DEFAULT uuidv7() NOT NULL,
    learner_profile_id UUID NOT NULL,
    dimension VARCHAR(32) NOT NULL,
    score NUMERIC(5,4) NOT NULL,
    baseline_score NUMERIC(5,4),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_dimension_interest_profile_dimension
    ON dimension_interest_score (learner_profile_id, dimension);


-- ---------------------------------------------------------------------------
-- 16. topic_suggestion
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
-- 17. saved_topic
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
-- 18. interest_quiz_item
--
-- student_id: NULL = bộ tĩnh gốc dùng chung (fallback); có giá trị = sinh riêng cho đúng học
-- sinh đó.
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
-- 19. interest_dimension -- danh mục "chiều sở thích", là DỮ LIỆU chứ không phải hằng số code
--
-- Trước đây danh sách bị lặp cứng ở 5 chỗ (Java + 3 Literal bên Python + prompt sinh quiz).
-- Thiếu đồng bộ một chỗ là dimension mới âm thầm vô hiệu chứ không báo lỗi -- đúng thứ đã xảy
-- ra với ACADEMIC_EXAM.
-- ---------------------------------------------------------------------------
CREATE TABLE interest_dimension (
    code            VARCHAR(32)  PRIMARY KEY,
    label           VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    -- Tắt mềm: giữ nguyên dữ liệu lịch sử (điểm số, topic đã gán) thay vì xoá cứng.
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Tách riêng với `active` vì có chiều KHÔNG phải sở thích: ACADEMIC_EXAM do hệ thống tự
    -- gán cho topic từ ngân hàng đề, không được đem ra hỏi học sinh "cái nào giống em nhất".
    quiz_eligible   BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order   INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interest_dimension_active_quiz
    ON interest_dimension (active, quiz_eligible);

INSERT INTO interest_dimension (code, label, description, quiz_eligible, display_order) VALUES
    ('ENTERTAINMENT_MEDIA', 'Giải trí & Truyền thông',
     'Phim ảnh, âm nhạc, nội dung số, sáng tạo hình ảnh/âm thanh.', TRUE, 1),
    ('TECH_GAMING', 'Công nghệ & Trò chơi',
     'Thiết bị, phần mềm, trò chơi điện tử, thiết kế giao diện/luật chơi.', TRUE, 2),
    ('SPORTS_HEALTH', 'Thể thao & Sức khoẻ',
     'Vận động, rèn luyện thể chất, dinh dưỡng, thói quen lành mạnh.', TRUE, 3),
    ('PEOPLE_SOCIETY', 'Con người & Xã hội',
     'Giao tiếp, làm việc nhóm, quan sát hành vi, hoạt động cộng đồng.', TRUE, 4),
    ('TRAVEL_PLACES', 'Du lịch & Địa điểm',
     'Khám phá nơi chốn, bản đồ, lộ trình, văn hoá vùng miền.', TRUE, 5),
    ('FUTURE_SCIENCE', 'Khoa học & Tương lai',
     'Hiện tượng tự nhiên, phát minh, môi trường, xu hướng tương lai.', TRUE, 6),
    -- quiz_eligible = FALSE: xem giải thích ở định nghĩa cột.
    ('ACADEMIC_EXAM', 'Ôn thi theo chương trình',
     'Chủ đề lấy từ ngân hàng câu hỏi của trường; hệ thống tự gán, không hỏi qua quiz.',
     FALSE, 99);


-- ===========================================================================
-- Vá các bảng NGOÀI personalize mà V1 baseline còn thiếu
-- ===========================================================================

-- exam_item_criterion_scores thiếu matched_band_code (entity đã có, dùng bởi luồng AI chấm bài
-- và truy vấn ước lượng band trong JpaLearnerProfileQueryRepository.findEstimatedBandCode).
ALTER TABLE exam_item_criterion_scores
    ADD COLUMN matched_band_code VARCHAR(64);

-- V1 thiếu SCHOOL_ROOM/RUBRIC_RESULT_BAND trong danh sách import_sessions.type cho phép, dù
-- ImportType.java và các use case import tương ứng đã dùng từ trước.
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

-- rubric_result_bands.score_min/score_max đang numeric(6,2), cần nới thành numeric cho
-- personalize.
ALTER TABLE rubric_result_bands
    ALTER COLUMN score_min TYPE numeric,
    ALTER COLUMN score_max TYPE numeric;


-- ===========================================================================
-- GHI CHÚ VỀ VIỆC GỘP -- đọc trước khi chạy trên một DB đã có dữ liệu
--
-- File này thay cho 12 migration cũ (V6..V18), đã xoá khỏi thư mục. Flyway ghi lịch sử theo
-- SỐ HIỆU vào bảng `flyway_schema_history`, nên:
--
--   * DB TRỐNG (hoặc mới chỉ chạy tới V5): chạy thẳng, không cần làm gì.
--
--   * DB ĐÃ TỪNG chạy V6..V16: Flyway sẽ TỪ CHỐI khởi động với
--     "Detected applied migration not resolved locally: 7, 8, 9, ..." -- vì lịch sử có những
--     số hiệu mà thư mục không còn file tương ứng. Cách xử lý là tạo lại DB từ đầu, hoặc xoá
--     các dòng >= 6 khỏi flyway_schema_history rồi drop tay các bảng personalize.
--
-- Đây là cái giá của việc gộp và không có cách nào tránh: Flyway không có khái niệm "viết lại
-- lịch sử". Chấp nhận được ở đây vì phần personalize chưa từng chạy ở đâu ngoài máy phát triển.
-- ===========================================================================
