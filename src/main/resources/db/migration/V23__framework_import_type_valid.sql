-- chk_import_sessions_type_valid thiếu 4 giá trị FRAMEWORK_* mới thêm ở ImportType.
alter table if exists import_sessions drop constraint if exists chk_import_sessions_type_valid;
alter table if exists import_sessions
    add constraint chk_import_sessions_type_valid
    check (type IN ('USER', 'SCHOOL_CLASS', 'SCHOOL_CLASS_USER', 'QUESTION', 'SCHOOL_DIRECTORY',
                    'SCHOOL_GRADE_LEVEL', 'SCHOOL_GRADE', 'SCHOOL_ROOM', 'RUBRIC_VERSION',
                    'RUBRIC_CRITERION', 'RUBRIC_RESULT_BAND', 'ASSESSMENT_POLICY',
                    'FRAMEWORK_VERSION', 'FRAMEWORK_CRITERION', 'FRAMEWORK_RESULT_BAND',
                    'FRAMEWORK_CRITERION_BAND'));
