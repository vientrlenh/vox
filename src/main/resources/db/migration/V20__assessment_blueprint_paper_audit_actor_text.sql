ALTER TABLE assessment_blueprints
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_blueprint_sections
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_blueprint_slots
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_blueprint_versions
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_papers
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE assessment_paper_sections
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;
