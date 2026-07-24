package com.sep.vox.infrastructure.initializer;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExamCandidateStatusConstraintInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ExamCandidateStatusConstraintInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE exam_candidates DROP CONSTRAINT IF EXISTS chk_exam_candidates_status_valid");
        jdbcTemplate.execute("""
            ALTER TABLE exam_candidates
            ADD CONSTRAINT chk_exam_candidates_status_valid
            CHECK (status IN ('ASSIGNED', 'ATTENDED', 'ABSENT', 'COMPLETED', 'EXEMPTED', 'CANCELLED'))
        """);
    }
}
