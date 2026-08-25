-- ImportType thêm QUESTION_BANK và QUESTION_TOPIC (nhập ngân hàng câu hỏi và chủ đề từ Excel) nhưng
-- check constraint chưa được nới theo, nên mọi phiên import hai loại đó bị Postgres từ chối ngay ở
-- câu insert đầu tiên. ImportSessionRepositoryTests quét đủ mọi giá trị của enum nên bắt được.
--
-- Danh sách phải khớp @CheckConstraint trên ImportSessionJpaEntity.type -- lệch thì Hibernate validate
-- schema sẽ báo, và lần vá tiếp theo lại phải mò cả hai chỗ như V2/V4 đã từng.
alter table if exists import_sessions drop constraint if exists chk_import_sessions_type_valid;
alter table if exists import_sessions
    add constraint chk_import_sessions_type_valid
    check (type IN ('USER', 'SCHOOL_CLASS', 'SCHOOL_CLASS_USER', 'QUESTION', 'QUESTION_BANK',
                    'QUESTION_TOPIC', 'SCHOOL_DIRECTORY', 'SCHOOL_GRADE_LEVEL', 'SCHOOL_GRADE',
                    'SCHOOL_ROOM', 'RUBRIC_VERSION', 'RUBRIC_CRITERION', 'RUBRIC_RESULT_BAND',
                    'ASSESSMENT_POLICY'));
