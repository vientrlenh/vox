package com.sep.vox.infrastructure.initializer;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;

/**
 * Declares the target band ceilings for the three high-school grades on the KNLNNVN version currently in force.
 *
 * <p>Without this table {@code GradeLevelBandScopeGuardService} runs in OPEN mode -- when it finds no
 * configuration row it lets every band through. Meaning the ceiling only really takes effect from the time
 * this initializer runs; before that a school could still set Band 6 for Grade 10 as before.
 *
 * <h2>Two kinds of assessment, two different ceilings</h2>
 *
 * <p>{@code BAND_CODE_DEFAULT_TARGET} is the ceiling for **CENTRALIZE** assessments (centralized testing -- the policy
 * applies to a whole Grade or a whole School Year, not anchored to one specific Class): Grades 10/11/12 may only choose Band 3
 * or Band 4. {@code BAND_CODE_HARD_MAX} is the ceiling for **CLASS_TEST** (a specialized class with its own
 * test -- the policy is anchored to one specific {@code schoolClassId}): relaxed up to Band 5. See
 * {@link GradeLevelBandScopeGuardService.Batch#effectiveCeilingBand} for how the ceiling is chosen per
 * scope at guard time.
 *
 * <p>All three grades share one pair of ceilings because the 2018 General Education Curriculum for English (Circular
 * 32/2018/TT-BGDĐT) fixes the output standard by SCHOOL LEVEL and not per grade (Band 3 by the end of high school),
 * and no document distinguishes CENTRALIZE/CLASS_TEST by grade -- this is a business decision
 * of the project, easy to extend when the grade catalog gains lower-secondary levels.
 *
 * <p>What must be blocked absolutely is Band 6 (in both kinds of assessment). In general education there is no
 * document that sets C2 as a target for STUDENTS; C1/C2 is officially the standard for high-school English
 * TEACHERS under the National Foreign Languages Project. Confusing those two standards is quite likely the origin
 * of schools setting target bands too high for the students.
 */
@Component
@Order(8)
@RequiredArgsConstructor
@Slf4j
public class GradeLevelBandScopeInitializer implements ApplicationRunner {

    private static final String FRAMEWORK_CODE = "KNLNNVN";

    // Must match exactly the band codes that FrameworkInitializer builds. With one wrong character this initializer
    // cannot find the band and skips everything -- with a warning log, not silently.
    // BAND_CODE_DEFAULT_TARGET = the CENTRALIZE ceiling, BAND_CODE_HARD_MAX = the CLASS_TEST ceiling -- see the
    // class Javadoc.
    private static final String BAND_CODE_DEFAULT_TARGET = "BAC_4";
    private static final String BAND_CODE_HARD_MAX = "BAC_5";

    private static final List<String> GRADE_LEVEL_CODES = List.of(
        GradeLevelInitializer.GRADE_10,
        GradeLevelInitializer.GRADE_11,
        GradeLevelInitializer.GRADE_12
    );

    private final GradeLevelRepository gradeLevelRepository;
    private final GradeLevelBandScopeRepository gradeLevelBandScopeRepository;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    /**
     * Idempotent PER ROW, unlike {@link GradeLevelInitializer} which only seeds when the table is empty.
     * The unique key of the table is {@code (grade_level_id, framework_version_id)}, so adding a missing row is an additive operation and does not overwrite
     * a configuration someone has edited. This is needed because every newly published framework version needs its own set of ceilings,
     * and the old set must stay untouched.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Framework framework = frameworkRepository.findByCode(FRAMEWORK_CODE).orElse(null);
        if (framework == null || !framework.isActive()) {
            log.warn("No active framework {}. Skipping grade band ceiling initialization", FRAMEWORK_CODE);
            return;
        }

        FrameworkVersion frameworkVersion = latestPublishedVersion(framework.getId());
        if (frameworkVersion == null) {
            log.warn("Framework {} has no published version. Skipping grade band ceiling initialization",
                FRAMEWORK_CODE);
            return;
        }

        seedScopes(frameworkVersion);
    }

    private void seedScopes(FrameworkVersion frameworkVersion) {
        FrameworkResultBand defaultTargetBand = frameworkResultBandRepository
            .findByVersionIdAndCode(frameworkVersion.getId(), BAND_CODE_DEFAULT_TARGET).orElse(null);
        FrameworkResultBand hardMaxBand = frameworkResultBandRepository
            .findByVersionIdAndCode(frameworkVersion.getId(), BAND_CODE_HARD_MAX).orElse(null);
        if (defaultTargetBand == null || hardMaxBand == null) {
            log.warn("Framework version {} lacks band {} or {}. Skipping grade band ceiling initialization",
                frameworkVersion.getCode(), BAND_CODE_DEFAULT_TARGET, BAND_CODE_HARD_MAX);
            return;
        }

        requireDefaultBandNotAboveHardMax(defaultTargetBand, hardMaxBand);

        int created = createMissingScopes(frameworkVersion, defaultTargetBand, hardMaxBand);

        if (created == 0) {
            log.info("Grade band ceilings on framework version {} are already complete. Skipping", frameworkVersion.getCode());
            return;
        }
        log.info("Initialized {} grade band ceilings on framework version {} (CENTRALIZE {}, CLASS_TEST {})",
            created, frameworkVersion.getCode(), BAND_CODE_DEFAULT_TARGET, BAND_CODE_HARD_MAX);
    }

    /**
     * An invariant the DB cannot express (it needs a join to framework_result_bands) and that
     * GradeLevelBandScopeGuardService guards at runtime. Also checked here so that a wrong constant above
     * breaks at startup, instead of producing an upside-down configuration that only shows up when a
     * school creates a policy.
     */
    private void requireDefaultBandNotAboveHardMax(FrameworkResultBand defaultTargetBand,
            FrameworkResultBand hardMaxBand) {
        if (defaultTargetBand.getOrder() > hardMaxBand.getOrder()) {
            throw new IllegalStateException("Default band " + BAND_CODE_DEFAULT_TARGET
                + " is higher than the ceiling band " + BAND_CODE_HARD_MAX + " in framework " + FRAMEWORK_CODE + ".");
        }
    }

    private int createMissingScopes(FrameworkVersion frameworkVersion, FrameworkResultBand defaultTargetBand,
            FrameworkResultBand hardMaxBand) {
        int created = 0;
        for (String gradeLevelCode : GRADE_LEVEL_CODES) {
            GradeLevel gradeLevel = gradeLevelRepository.findByCode(gradeLevelCode).orElse(null);
            if (gradeLevel == null) {
                log.warn("Grade {} not found. Skipping the band ceiling for this grade", gradeLevelCode);
                continue;
            }
            if (gradeLevelBandScopeRepository
                    .findByGradeLevelIdAndFrameworkVersionId(gradeLevel.getId(), frameworkVersion.getId())
                    .isPresent()) {
                continue;
            }

            gradeLevelBandScopeRepository.save(GradeLevelBandScope.builder()
                .gradeLevelId(gradeLevel.getId())
                .frameworkVersionId(frameworkVersion.getId())
                .defaultTargetBandId(defaultTargetBand.getId())
                .hardMaxBandId(hardMaxBand.getId())
                .build());
            created++;
        }
        return created;
    }

    private FrameworkVersion latestPublishedVersion(UUID frameworkId) {
        return frameworkVersionRepository
            .findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED).stream()
            .max(Comparator.comparingInt(version -> version.getVersion()))
            .orElse(null);
    }
}
