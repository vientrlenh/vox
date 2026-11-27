package com.sep.vox.infrastructure.initializer;

import com.sep.vox.domain.common.PageResult;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;

/**
 * Builds the catalog of high-school grades (Grade 10, 11, 12) shared across the whole system.
 *
 * <p>This seed used to be in step 8 of {@code V41__grade_level_global_catalog.sql}. It moved
 * here for three reasons:
 *
 * <ul>
 *   <li>Migrations do NOT run in tests: {@code src/test/resources/application-test.yaml} sets
 *       {@code ddl-auto: create-drop}, so Flyway is skipped entirely when running {@code ./gradlew
 *       test}. Seed data living in a .sql file is data no test ever sees.</li>
 *   <li>A migration runs exactly once. Changing the seed content after V41 has been applied to some
 *       environment changes the checksum -- Flyway will refuse to start. An initializer runs again on every boot
 *       so it can be edited.</li>
 *   <li>These three grades are business data and not table structure. Next to
 *       {@link FrameworkInitializer} and {@link SystemRubricTemplateInitializer} they can be found by
 *       reading Java code, instead of having to remember that they sit inside a migration file.</li>
 * </ul>
 *
 * <h2>Only seeds when the catalog is COMPLETELY empty</h2>
 *
 * <p>Keeps the semantics of the {@code where not exists (select 1 from grade_levels)} statement in V41, rather than
 * checking per code. Two reasons:
 *
 * <ul>
 *   <li>{@code grade_level_order} is globally unique ({@code idx_grade_levels_order}). Inserting GRADE_10 at
 *       order 1 into a catalog where another grade already holds order 1 would blow up the constraint right at
 *       startup.</li>
 *   <li>A system already running for real has a catalog that V41 merged up from the {@code school_grade_levels} of the
 *       schools. Stacking the default high-school set on top of that adds grades with duplicate meaning that nobody
 *       declared.</li>
 * </ul>
 *
 * <p>Meaning: delete all grades and restart and the default set comes back; delete exactly one grade and it
 * does not. Intentional -- deleting one grade is a decision, not an incident for the initializer to heal.
 */
@Component
@Order(7)
@RequiredArgsConstructor
@Slf4j
public class GradeLevelInitializer implements ApplicationRunner {

    /**
     * A grade code is a matching key that leaves the system: {@link GradeLevelBandScopeInitializer} and
     * {@link SystemAssessmentPolicyInitializer} both look up exactly these three strings, and the Excel
     * import flow normalizes with {@code toUpperCase} before comparing. Changing a code here means changing it in all three places.
     */
    static final String GRADE_10 = "GRADE_10";
    static final String GRADE_11 = "GRADE_11";
    static final String GRADE_12 = "GRADE_12";

    private static final List<GradeLevelSeed> GRADE_LEVELS = List.of(
        new GradeLevelSeed(GRADE_10, "Khối 10", "Khối lớp 10 bậc trung học phổ thông", 1),
        new GradeLevelSeed(GRADE_11, "Khối 11", "Khối lớp 11 bậc trung học phổ thông", 2),
        new GradeLevelSeed(GRADE_12, "Khối 12", "Khối lớp 12 bậc trung học phổ thông", 3)
    );

    private final GradeLevelRepository gradeLevelRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // findAll takes the page 1-based (GradeLevelRepositoryImpl calls PageRequest.of(page - 1,
        // size)), so the first page is 1, not 0. Size 1 is used because only totalElements is needed.
        PageResult<GradeLevel> existing = gradeLevelRepository.findAll(null, null, 1, 1);
        if (existing.totalElements() > 0) {
            log.info("The grade catalog already has {} records. Skipping default grade initialization",
                existing.totalElements());
            return;
        }

        for (GradeLevelSeed seed : GRADE_LEVELS) {
            gradeLevelRepository.save(GradeLevel.builder()
                .code(seed.code())
                .name(seed.name())
                .description(seed.description())
                .order(seed.order())
                .status(GradeLevelStatus.ACTIVE)
                .build());
        }

        log.info("Initialized {} default high-school grades", GRADE_LEVELS.size());
    }

    private record GradeLevelSeed(String code, String name, String description, int order) {}
}
