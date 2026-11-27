package com.sep.vox.infrastructure.initializer;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

/**
 * Builds the system's six SAMPLE grading policies up front: for each sample high-school criteria set created by
 * {@link SystemRubricTemplateInitializer} there is a standard version (Level 3, declared by Grade) and a version
 * for specialized classes (Level 4, no scope declared).
 *
 * <p>This is what gives {@code CloneSystemRubricToSchoolUseCase} a policy to copy along with the criteria
 * set. Before this file existed, a school that wanted to use a template had to copy the rubric and then retype all the policy
 * parameters itself -- exactly the manual step that the copy path was created to remove.
 *
 * <h2>PUBLISHED right away, and why it goes around the use case</h2>
 *
 * <p>{@code CloneSystemRubricToSchoolUseCase} rejects every template that has not been published, so a sample policy
 * in the draft state is a policy nobody can copy.
 *
 * <p>{@code CreateSystemAssessmentPolicyUseCase} cannot be called, for two reasons. One, at startup there is no
 * login session yet from which that use case takes the system admin identity. Two, that use case requires the rubric version
 * to still be DRAFT ("A Policy may only be attached while the Rubric Version is DRAFT") because the real flow is to
 * create a draft policy on a draft rubric and publish both at the same time; while the sample criteria set here is built by
 * {@link SystemRubricTemplateInitializer} directly in the PUBLISHED state. The final state this file
 * writes -- a PUBLISHED policy pointing at a PUBLISHED rubric -- equals the result of the real flow, it only
 * skips the intermediate step.
 *
 * <p>In return, the invariants of the real creation path have to be kept by hand here: the framework is still PUBLISHED, the
 * target level belongs to the right framework version, the rubric is owned by SYSTEM and has the same framework as the policy.
 *
 * <h2>The standard version declares a Grade, the specialized version leaves the scope empty</h2>
 *
 * <p>Grade levels are a global catalog and are not tied to any school year, so a sample policy declared by Grade
 * stays correct when the school opens next year's school year -- it does not have to be declared again every year. That is also why
 * the use case that copies resolves scope ({@code resolveScope}) requires the copy to KEEP the grade of the template: changing the grade is
 * keeping the parameters written for Grade 10 and pasting a Grade 12 label on them.
 *
 * <p>That is exactly why the SPECIALIZED version must not declare a Grade: the copy would be pinned to a whole grade, while
 * what the school needs is to apply it only to the specialized class. In addition each scope may only have one policy in
 * effect, so the standard version and the specialized version cannot coexist at Grade scope. Leaving the scope empty is the
 * only way for a school to copy the standard version for a Grade/School Year and the specialized version for exactly the specialized class.
 *
 * <h2>Two target levels, not three grades x three levels</h2>
 *
 * <p>The standard version of all three grades uses the same {@code BAC_3}. According to Circular 32/2018/TT-BGDĐT the output standard is
 * Level 3 by the end of high school, not split by grade. See {@link GradeLevelBandScopeInitializer} for why
 * the original 6-level framework cannot express the 3.1/3.2/3.3 difference between the three grades.
 *
 * <p>The specialized version targets {@code BAC_4} (CEFR B2) -- exactly the HARD CEILING that the ceiling table sets for all three grades,
 * so the copy always passes {@code GradeLevelBandScopeGuardService}. B2 is the practice of specialized schools, not
 * a regulation (Circular 05/2023/TT-BGDĐT sets no foreign-language standard for specialized classes), so
 * it is here as an optional template: a school without specialized classes does not copy this one.
 *
 * <p>The target level decides which descriptor set is sent to the AI for grading
 * ({@code SubmitAssessmentSessionUseCase.buildCriteriaFrameworks}), so two levels mean two different yardsticks --
 * the raw scores of a specialized class and a regular class cannot be compared directly. That is intended: these are two
 * groups of students with different targets to reach.
 *
 * <h2>No passing score is set</h2>
 *
 * <p>{@code passingScore} is left null (meaning the pass score is not checked). The official documents specify the LEVEL to be
 * reached, not a score threshold; setting a number here would be the only part of the seed with no
 * basis behind it. The school sets it itself after copying.
 */
@Component
@Order(9)
@RequiredArgsConstructor
@Slf4j
public class SystemAssessmentPolicyInitializer implements ApplicationRunner {

    private static final String LANGUAGE_CODE = "ENG";
    private static final String FRAMEWORK_CODE = "KNLNNVN";

    /** Level 3 = CEFR B1 = the high-school output standard. Must match the band code of {@link FrameworkInitializer}. */
    private static final String TARGET_BAND_CODE = "BAC_3";

    /**
     * Level 4 = CEFR B2 = the usual practice for specialized English classes.
     *
     * <p>Exactly equal to the HARD CEILING that {@link GradeLevelBandScopeInitializer} sets for all three high-school grades, so
     * a copy of this template always passes {@code GradeLevelBandScopeGuardService}. Note that B2 is NOT part of
     * the specialized-school regulation (Circular 05/2023/TT-BGDĐT sets no foreign-language output standard
     * for specialized classes) -- this is a template a school may opt into, not a regulation.
     */
    private static final String SPECIALISED_TARGET_BAND_CODE = "BAC_4";

    /**
     * Each version of a sample criteria set has TWO policies: a standard one (Level 3) and one for specialized
     * classes (Level 4). Same criteria set, only the target level differs -- the difference between regular and specialized
     * classes lies in the goal to reach, not in the grading criteria.
     *
     * <p><b>Why the specialized version does NOT declare a Grade:</b> once a template declares a Grade, the copy MUST keep exactly
     * that grade ({@code CloneSystemRubricToSchoolUseCase#resolveScope}), which means the school cannot apply it
     * to the specialized class alone. And "each scope has only one effective policy" means the standard and
     * specialized versions can NOT coexist at the Grade scope. Leaving the scope empty is the only way for a school to
     * copy the standard version for Grade/School year and the specialized version for the specialized class only, in a single pass.
     *
     * <p>The version code follows the convention of {@link SystemRubricTemplateInitializer}: template code + "-V1".
     * If the code there is changed and this one is forgotten, this initializer cannot find the rubric and skips the matching
     * version -- with a warning log, not silently.
     */
    private static final List<PolicySeed> POLICIES = List.of(
        new PolicySeed(GradeLevelInitializer.GRADE_10, "SYS-ENG-K10-V1", TARGET_BAND_CODE),
        new PolicySeed(GradeLevelInitializer.GRADE_11, "SYS-ENG-K11-V1", TARGET_BAND_CODE),
        new PolicySeed(GradeLevelInitializer.GRADE_12, "SYS-ENG-K12-V1", TARGET_BAND_CODE),
        new PolicySeed(null, "SYS-ENG-K10-V1", SPECIALISED_TARGET_BAND_CODE),
        new PolicySeed(null, "SYS-ENG-K11-V1", SPECIALISED_TARGET_BAND_CODE),
        new PolicySeed(null, "SYS-ENG-K12-V1", SPECIALISED_TARGET_BAND_CODE)
    );

    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;

    /**
     * Idempotent per TEMPLATE, keyed by the pair (rubric version, target band) -- see
     * {@link #createPolicy}. That means a template that already exists is skipped, the remaining ones are still built, and
     * running it many times produces nothing extra.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        SupportedLanguage language = supportedLanguageRepository.findByCode(LANGUAGE_CODE).orElse(null);
        if (language == null) {
            log.warn("Chưa có ngôn ngữ {}. Bỏ qua khởi tạo chính sách chấm mẫu của hệ thống", LANGUAGE_CODE);
            return;
        }

        Framework framework = frameworkRepository.findByCode(FRAMEWORK_CODE).orElse(null);
        if (framework == null || !framework.isActive()) {
            log.warn("Chưa có khung {} đang hoạt động. Bỏ qua khởi tạo chính sách chấm mẫu", FRAMEWORK_CODE);
            return;
        }

        FrameworkVersion frameworkVersion = latestPublishedVersion(framework.getId());
        if (frameworkVersion == null) {
            log.warn("Khung {} chưa có phiên bản nào được ban hành. Bỏ qua khởi tạo chính sách chấm mẫu",
                FRAMEWORK_CODE);
            return;
        }

        seedPolicies(language, framework, frameworkVersion);
    }

    private void seedPolicies(SupportedLanguage language, Framework framework, FrameworkVersion frameworkVersion) {
        Instant now = Instant.now();
        int created = 0;
        for (PolicySeed seed : POLICIES) {
            if (createPolicy(seed, language.getId(), framework.getId(), frameworkVersion, now)) {
                created++;
            }
        }

        if (created == 0) {
            log.info("Chính sách chấm mẫu của hệ thống đã đầy đủ. Bỏ qua khởi tạo");
            return;
        }
        log.info("Đã khởi tạo {} chính sách chấm mẫu của hệ thống (ngôn ngữ {}, khung {})",
            created, LANGUAGE_CODE, frameworkVersion.getCode());
    }

    /** @return true if a new policy was written. */
    private boolean createPolicy(
            PolicySeed seed,
            UUID languageId,
            UUID frameworkId,
            FrameworkVersion frameworkVersion,
            Instant now) {

        Optional<PolicyInputs> resolved = resolveInputs(seed, frameworkId, frameworkVersion);
        if (resolved.isEmpty() || isAlreadySeeded(resolved.get())) {
            return false;
        }

        PolicyInputs inputs = resolved.get();
        int nextVersion = assessmentPolicyRepository.findMaxVersionForScope(
            null, languageId, frameworkVersion.getId(), inputs.gradeLevelId(), null, null) + 1;

        assessmentPolicyRepository.save(buildSystemPolicy(inputs, languageId, frameworkVersion, nextVersion, now));
        return true;
    }

    /** @return empty, after logging why, when a template cannot be built from the current data. */
    private Optional<PolicyInputs> resolveInputs(PolicySeed seed, UUID frameworkId, FrameworkVersion frameworkVersion) {
        FrameworkResultBand targetBand = findTargetBand(seed, frameworkVersion);
        if (targetBand == null) {
            return Optional.empty();
        }

        // The specialized template declares no Grade: with an empty scope the school chooses when copying it over.
        UUID gradeLevelId = null;
        if (seed.gradeLevelCode() != null) {
            GradeLevel gradeLevel = findGradeLevel(seed.gradeLevelCode());
            if (gradeLevel == null) {
                return Optional.empty();
            }
            gradeLevelId = gradeLevel.getId();
        }

        RubricVersion rubricVersion = findSystemRubricVersion(seed, frameworkId);
        if (rubricVersion == null) {
            return Optional.empty();
        }
        return Optional.of(new PolicyInputs(targetBand, gradeLevelId, rubricVersion));
    }

    private @Nullable FrameworkResultBand findTargetBand(PolicySeed seed, FrameworkVersion frameworkVersion) {
        FrameworkResultBand targetBand = frameworkResultBandRepository
            .findByVersionIdAndCode(frameworkVersion.getId(), seed.targetBandCode()).orElse(null);
        if (targetBand == null) {
            log.warn("Phiên bản khung {} thiếu bậc {}. Bỏ qua chính sách chấm mẫu tương ứng",
                frameworkVersion.getCode(), seed.targetBandCode());
        }
        return targetBand;
    }

    private @Nullable GradeLevel findGradeLevel(String gradeLevelCode) {
        GradeLevel gradeLevel = gradeLevelRepository.findByCode(gradeLevelCode).orElse(null);
        if (gradeLevel == null) {
            log.warn("Không tìm thấy khối {}. Bỏ qua chính sách chấm mẫu cho khối này", gradeLevelCode);
        }
        return gradeLevel;
    }

    private @Nullable RubricVersion findSystemRubricVersion(PolicySeed seed, UUID frameworkId) {
        RubricVersion rubricVersion = rubricVersionRepository.findByCode(seed.rubricVersionCode()).orElse(null);
        if (rubricVersion == null) {
            log.warn("Không tìm thấy phiên bản bộ tiêu chí mẫu {}. Bỏ qua chính sách chấm mẫu bậc {}",
                seed.rubricVersionCode(), seed.targetBandCode());
            return null;
        }
        Rubric rubric = rubricRepository.findById(rubricVersion.getRubricId()).orElse(null);
        if (rubric == null || rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            log.warn("Bộ tiêu chí của phiên bản {} không thuộc sở hữu SYSTEM. Bỏ qua chính sách chấm mẫu"
                + " bậc {}", seed.rubricVersionCode(), seed.targetBandCode());
            return null;
        }
        // The same check CreateSystemAssessmentPolicyUseCase applies: the rubric is tied to the FRAMEWORK (not
        // the framework version), so compare against getFrameworkId of the version in use.
        if (!rubric.getFrameworkId().equals(frameworkId)) {
            log.warn("Bộ tiêu chí {} không cùng khung với chính sách. Bỏ qua chính sách chấm mẫu bậc {}",
                seed.rubricVersionCode(), seed.targetBandCode());
            return null;
        }
        return rubricVersion;
    }

    private boolean isAlreadySeeded(PolicyInputs inputs) {
        // Idempotent by EXACTLY the identity of a template: (rubric version, target band).
        //
        // This place used to use existsActiveForScopeAnyRubricVersion -- the "one policy per scope"
        // check of the real creation path. That rule is an invariant of SCHOOL policies (each class may only be
        // graded by one policy), not of the template directory: two templates of the same grade with different bands
        // are exactly what we want to build. Keeping it would silently skip the specialized template, and the three
        // specialized templates (none of which declares a grade) would also overwrite each other because they share the same empty scope.
        //
        // Dropping that rule here affects NOTHING: a system policy does not grade attempts (the grading path reads
        // assessments.assessment_policy_id), findActivePolicy has no remaining callers, and idx_assessment_policies
        // _scope_version does not catch any row that has NULL in its key. The manual creation path of the system admin
        // (CreateSystemAssessmentPolicyUseCase) keeps the old rule.
        return assessmentPolicyRepository
            .findPublishedSystemWideByRubricVersionId(inputs.rubricVersion().getId()).stream()
            .anyMatch(existing -> inputs.targetBand().getId().equals(existing.getTargetFrameworkBandId()));
    }

    private AssessmentPolicy buildSystemPolicy(PolicyInputs inputs, UUID languageId, FrameworkVersion frameworkVersion,
            int nextVersion, Instant now) {
        return AssessmentPolicy.builder()
            // schoolId null = a system policy, belonging to no school.
            .schoolId(null)
            .gradeLevelId(inputs.gradeLevelId())
            // School year and Class belong to the scope of a specific school, so a system policy always leaves them empty.
            .schoolGradeId(null)
            .schoolClassId(null)
            .languageId(languageId)
            .frameworkVersionId(frameworkVersion.getId())
            .rubricVersionId(inputs.rubricVersion().getId())
            .targetFrameworkBandId(inputs.targetBand().getId())
            // See the class javadoc: the regulation dictates the band, not the score threshold.
            .passingScore(null)
            .strictness(AssessmentPolicyStrictness.STANDARD)
            .version(nextVersion)
            .status(AssessmentPolicyStatus.PUBLISHED)
            // effectiveTo null = unlimited. A template is only for reading and copying so there is no reason for it to expire;
            // the real validity range is what the school sets itself when copying it over.
            .effectiveFrom(now)
            .effectiveTo(null)
            .build();
    }

    private FrameworkVersion latestPublishedVersion(UUID frameworkId) {
        return frameworkVersionRepository
            .findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED).stream()
            .max(Comparator.comparingInt(version -> version.getVersion()))
            .orElse(null);
    }

    /**
     * @param gradeLevelCode     mã khối trong catalog toàn cục; null = bản mẫu không khai phạm vi,
     *                           trường tự chọn lúc sao về (dùng cho bản lớp chuyên).
     * @param rubricVersionCode  mã phiên bản bộ tiêu chí mẫu tương ứng của hệ thống.
     * @param targetBandCode     mã bậc mục tiêu; đây là thứ phân biệt bản chuẩn với bản chuyên.
     */
    private record PolicyInputs(
        FrameworkResultBand targetBand, @Nullable UUID gradeLevelId, RubricVersion rubricVersion) {}

    private record PolicySeed(String gradeLevelCode, String rubricVersionCode, String targetBandCode) {}
}
