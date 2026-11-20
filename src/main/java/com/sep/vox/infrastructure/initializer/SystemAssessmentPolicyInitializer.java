package com.sep.vox.infrastructure.initializer;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SystemAssessmentPolicyInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemAssessmentPolicyInitializer.class);

    private static final String LANGUAGE_CODE = "ENG";
    private static final String FRAMEWORK_CODE = "KNLNNVN";

    /** Bậc 3 = CEFR B1 = chuẩn đầu ra THPT. Phải trùng mã bậc của {@link FrameworkInitializer}. */
    private static final String TARGET_BAND_CODE = "BAC_3";

    /**
     * Bậc 4 = CEFR B2 = thông lệ của lớp chuyên Anh.
     *
     * <p>Đúng bằng TRẦN CỨNG mà {@link GradeLevelBandScopeInitializer} đặt cho cả ba khối THPT, nên
     * bản sao của bản mẫu này luôn lọt {@code GradeLevelBandScopeGuardService}. Lưu ý B2 KHÔNG nằm
     * trong quy chế trường chuyên (Thông tư 05/2023/TT-BGDĐT không quy định chuẩn đầu ra ngoại ngữ
     * nào cho lớp chuyên) -- đây là bản mẫu để trường tuỳ chọn, không phải quy định.
     */
    private static final String SPECIALISED_TARGET_BAND_CODE = "BAC_4";

    /**
     * Mỗi phiên bản bộ tiêu chí mẫu có HAI chính sách: một bản chuẩn (Bậc 3) và một bản cho lớp
     * chuyên (Bậc 4). Cùng bộ tiêu chí, chỉ khác bậc mục tiêu -- khác biệt giữa lớp thường và lớp
     * chuyên nằm ở đích cần đạt, không ở tiêu chí chấm.
     *
     * <p><b>Vì sao bản chuyên KHÔNG khai Khối:</b> bản mẫu đã khai Khối thì bản sao BẮT BUỘC giữ đúng
     * khối đó ({@code CloneSystemRubricToSchoolUseCase#resolveScope}), tức là trường không thể áp nó
     * cho riêng lớp chuyên. Mà "mỗi phạm vi chỉ một chính sách còn hiệu lực" nghĩa là bản chuẩn và
     * bản chuyên KHÔNG cùng tồn tại ở phạm vi Khối được. Để trống phạm vi là cách duy nhất để trường
     * sao bản chuẩn cho Khối/Niên khóa và bản chuyên cho đúng lớp chuyên trong cùng một lần.
     *
     * <p>Mã phiên bản theo quy ước của {@link SystemRubricTemplateInitializer}: mã bản mẫu + "-V1".
     * Đổi mã bên đó mà quên đổi ở đây thì initializer này không tìm thấy rubric và bỏ qua bản tương
     * ứng -- có log cảnh báo chứ không âm thầm.
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

    public SystemAssessmentPolicyInitializer(
            AssessmentPolicyRepository assessmentPolicyRepository,
            GradeLevelRepository gradeLevelRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository) {
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
    }

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
            LOGGER.warn("Chưa có ngôn ngữ {}. Bỏ qua khởi tạo chính sách chấm mẫu của hệ thống", LANGUAGE_CODE);
            return;
        }

        Framework framework = frameworkRepository.findByCode(FRAMEWORK_CODE).orElse(null);
        if (framework == null || !framework.isActive()) {
            LOGGER.warn("Chưa có khung {} đang hoạt động. Bỏ qua khởi tạo chính sách chấm mẫu", FRAMEWORK_CODE);
            return;
        }

        FrameworkVersion frameworkVersion = latestPublishedVersion(framework.getId());
        if (frameworkVersion == null) {
            LOGGER.warn("Khung {} chưa có phiên bản nào được ban hành. Bỏ qua khởi tạo chính sách chấm mẫu",
                FRAMEWORK_CODE);
            return;
        }

        Instant now = Instant.now();
        int created = 0;
        for (PolicySeed seed : POLICIES) {
            if (createPolicy(seed, language.getId(), framework.getId(), frameworkVersion, now)) {
                created++;
            }
        }

        if (created == 0) {
            LOGGER.info("Chính sách chấm mẫu của hệ thống đã đầy đủ. Bỏ qua khởi tạo");
            return;
        }
        LOGGER.info("Đã khởi tạo {} chính sách chấm mẫu của hệ thống (ngôn ngữ {}, khung {})",
            created, LANGUAGE_CODE, frameworkVersion.getCode());
    }

    /** @return true if a new policy was written. */
    private boolean createPolicy(
            PolicySeed seed,
            UUID languageId,
            UUID frameworkId,
            FrameworkVersion frameworkVersion,
            Instant now) {

        FrameworkResultBand targetBand = frameworkResultBandRepository
            .findByVersionIdAndCode(frameworkVersion.getId(), seed.targetBandCode()).orElse(null);
        if (targetBand == null) {
            LOGGER.warn("Phiên bản khung {} thiếu bậc {}. Bỏ qua chính sách chấm mẫu tương ứng",
                frameworkVersion.getCode(), seed.targetBandCode());
            return false;
        }

        // The specialized template declares no Grade: with an empty scope the school chooses when copying it over.
        GradeLevel gradeLevel = null;
        if (seed.gradeLevelCode() != null) {
            gradeLevel = gradeLevelRepository.findByCode(seed.gradeLevelCode()).orElse(null);
            if (gradeLevel == null) {
                LOGGER.warn("Không tìm thấy khối {}. Bỏ qua chính sách chấm mẫu cho khối này",
                    seed.gradeLevelCode());
                return false;
            }
        }
        UUID gradeLevelId = gradeLevel == null ? null : gradeLevel.getId();

        RubricVersion rubricVersion = rubricVersionRepository.findByCode(seed.rubricVersionCode()).orElse(null);
        if (rubricVersion == null) {
            LOGGER.warn("Không tìm thấy phiên bản bộ tiêu chí mẫu {}. Bỏ qua chính sách chấm mẫu bậc {}",
                seed.rubricVersionCode(), seed.targetBandCode());
            return false;
        }
        Rubric rubric = rubricRepository.findById(rubricVersion.getRubricId()).orElse(null);
        if (rubric == null || rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            LOGGER.warn("Bộ tiêu chí của phiên bản {} không thuộc sở hữu SYSTEM. Bỏ qua chính sách chấm mẫu"
                + " bậc {}", seed.rubricVersionCode(), seed.targetBandCode());
            return false;
        }
        // The same check CreateSystemAssessmentPolicyUseCase applies: the rubric is tied to the FRAMEWORK (not
        // the framework version), so compare against getFrameworkId of the version in use.
        if (!rubric.getFrameworkId().equals(frameworkId)) {
            LOGGER.warn("Bộ tiêu chí {} không cùng khung với chính sách. Bỏ qua chính sách chấm mẫu bậc {}",
                seed.rubricVersionCode(), seed.targetBandCode());
            return false;
        }

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
        boolean alreadySeeded = assessmentPolicyRepository
            .findPublishedSystemWideByRubricVersionId(rubricVersion.getId()).stream()
            .anyMatch(existing -> targetBand.getId().equals(existing.getTargetFrameworkBandId()));
        if (alreadySeeded) {
            return false;
        }

        int nextVersion = assessmentPolicyRepository.findMaxVersionForScope(
            null, languageId, frameworkVersion.getId(), gradeLevelId, null, null) + 1;

        assessmentPolicyRepository.save(new AssessmentPolicy(
            // schoolId null = a system policy, belonging to no school.
            null,
            gradeLevelId,
            // School year and Class belong to the scope of a specific school, so a system policy always leaves them empty.
            null,
            null,
            languageId,
            frameworkVersion.getId(),
            rubricVersion.getId(),
            targetBand.getId(),
            // See the class javadoc: the regulation dictates the band, not the score threshold.
            null,
            AssessmentPolicyStrictness.STANDARD,
            nextVersion,
            AssessmentPolicyStatus.PUBLISHED,
            // effectiveTo null = unlimited. A template is only for reading and copying so there is no reason for it to expire;
            // the real validity range is what the school sets itself when copying it over.
            now,
            null,
            now,
            now,
            null,
            null
        ));
        return true;
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
    private record PolicySeed(String gradeLevelCode, String rubricVersionCode, String targetBandCode) {}
}
