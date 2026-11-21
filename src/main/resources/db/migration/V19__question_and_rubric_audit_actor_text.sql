ALTER TABLE questions
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE question_banks
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE question_topics
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE rubric_criterions
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE rubric_result_bands
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE rubric_versions
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;
