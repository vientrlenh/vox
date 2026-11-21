ALTER TABLE assessments
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_schedules
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_secure_pools
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_candidate_results
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;
