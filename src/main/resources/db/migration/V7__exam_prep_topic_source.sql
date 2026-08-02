-- EXAM_PREP lấy topic từ ngân hàng câu hỏi (question_banks/question_topics, đã có sẵn từ
-- baseline, được gắn khối qua question_bank_grades) theo đúng trường + khối của học sinh, thay
-- cho class_topic_scope (chưa từng có UI nào gọi tới, xoá hẳn). Mỗi topic được vật chất hoá
-- lazy thành 1 dòng practice_topic khi lần đầu xuất hiện trong danh sách gợi ý, liên kết ngược
-- qua source_question_topic_id để pipeline câu hỏi / điểm yếu / vector sở thích chạy nguyên vẹn.

ALTER TABLE practice_topic
    ADD COLUMN source_question_topic_id UUID NULL;

ALTER TABLE practice_topic
    ADD CONSTRAINT uq_practice_topic_source_question_topic UNIQUE (source_question_topic_id);

ALTER TABLE practice_topic
    ADD CONSTRAINT fk_practice_topic_source_question_topic
        FOREIGN KEY (source_question_topic_id) REFERENCES question_topics (id);

DROP TABLE IF EXISTS class_topic_scope;
