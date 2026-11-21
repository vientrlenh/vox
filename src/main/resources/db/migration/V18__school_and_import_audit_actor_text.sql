ALTER TABLE schools
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE school_classes
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE school_directories
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE school_grades
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE school_rooms
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;

ALTER TABLE import_sessions
    ALTER COLUMN created_by TYPE varchar(64) USING created_by::text,
    ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::text;
