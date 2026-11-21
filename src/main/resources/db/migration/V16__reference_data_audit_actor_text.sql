ALTER TABLE frameworks
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE framework_versions
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE framework_criteria
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE framework_result_bands
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE framework_criterion_bands
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE grade_levels
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE grade_level_band_scopes
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_policies
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE scoring_rules
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;
