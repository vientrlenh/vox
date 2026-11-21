package com.sep.vox.infrastructure.initializer;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.service.rubric.RubricScoringConsistencyValidator;

/**
 * Builds the system's three SAMPLE criteria sets (grades 10, 11, 12) up front so schools have something to copy from the
 * very first startup.
 *
 * <p>Different from {@link FrameworkInitializer} in that this is NOT mandatory data: without it the system still
 * runs and schools can still author a criteria set from scratch. It exists so the copy feature has content, and
 * so a new school has a reasonable starting point instead of a blank page.
 *
 * <h2>Three templates, not three templates x two scoring methods</h2>
 *
 * <p>All three are declared in the DISTRIBUTED form ({@code SUM}). A parallel Average version is not built, because Average
 * can be derived from Distributed at copy time -- {@code RubricCloneService} flattens every weight to 1 when the
 * school chooses that method -- while the opposite direction cannot: from the state where every criterion is equal
 * there is no way to recreate the ratios the template defined. The Distributed version carries more information, so it
 * is the form worth storing.
 *
 * <h2>The accompanying assessment policy lives in another initializer</h2>
 *
 * <p>{@link SystemAssessmentPolicyInitializer} runs later ({@code @Order(9)}) and builds for each grade one
 * sample policy pointing at the template of the same grade here. The two are tied together by the VERSION CODE
 * ({@code SYS-ENG-K10-V1}...), i.e. the convention {@code code + "-V1"} below is something that leaves this
 * file -- change it and the lookup table over there has to change too.
 *
 * <p>Historical note: this place used to explain that it deliberately did NOT build system policies, because
 * {@code findCandidatePolicies} filters {@code WHERE p.schoolId = :schoolId} so a policy belonging to
 * no school would never be chosen. That reason no longer describes the running system: those two scope
 * resolution functions are now dead code (the real grading path reads {@code assessments.assessment_policy_id}), and
 * more importantly, the system's sample policy does not exist for grading -- it exists for schools to COPY
 * together with the criteria set through {@code CloneSystemRubricToSchoolUseCase}. The copy is the one that does the grading.
 *
 * <h2>Written straight through the repository</h2>
 *
 * <p>Goes around {@code CreateSystemRubricUseCase} and {@code ChangeSystemRubricVersionStatusUseCase}
 * because at startup no user is logged in for those use cases to take an identity from. In return, the
 * invariants that the publish path requires have to be kept by hand here: the root framework is still active, the version
 * has criteria, and the weights/scoring scale are consistent. The last condition is checked with the very
 * {@link RubricScoringConsistencyValidator} of the real publish path, so wrong numbers in this file
 * break startup instead of silently producing a template that can never be republished.
 */
@Component
@Order(6)
public class SystemRubricTemplateInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemRubricTemplateInitializer.class);

    /** Bản mẫu bám vào ngôn ngữ và khung do hai initializer chạy trước dựng ra. */
    private static final String LANGUAGE_CODE = "ENG";
    private static final String FRAMEWORK_CODE = "KNLNNVN";

    /**
     * Thang điểm 10 của phổ thông Việt Nam.
     *
     * <p>Mọi tiêu chí phải phủ ĐÚNG thang này -- chênh lệch tầm quan trọng giữa các tiêu chí thể hiện
     * bằng trọng số, không phải bằng khoảng điểm hẹp hơn. Xem {@link RubricScoringConsistencyValidator}.
     */
    private static final BigDecimal SCORING_SCALE_MIN = new BigDecimal("0.00");
    private static final BigDecimal SCORING_SCALE_MAX = new BigDecimal("10.00");

    // Mã tiêu chí của khung KNLNNVN. Dùng lại nguyên mã đó làm mã tiêu chí rubric: hai bên là ánh xạ
    // một-một (bảng rubric_criterions có unique (rubric_version_id, framework_criterion_id)), nên đặt
    // mã khác chỉ tạo thêm một từ điển phải tra.
    private static final String CRITERION_PRONUNCIATION = "PRONUNCIATION";
    private static final String CRITERION_FLUENCY = "FLUENCY";
    private static final String CRITERION_GRAMMAR = "GRAMMAR";
    private static final String CRITERION_VOCABULARY = "VOCABULARY";
    private static final String CRITERION_COHERENCE = "COHERENCE";

    /**
     * Năm tiêu chí dùng chung cho cả ba khối: cùng một năng lực nói, chỉ khác mức nhấn.
     *
     * <p>Khác biệt giữa các khối nằm hoàn toàn ở TRỌNG SỐ, không ở mô tả tiêu chí. Viết ba bộ mô tả
     * gần giống nhau sẽ tạo ra ba bản dễ trôi lệch mà không mang thêm thông tin nào.
     */
    private static final List<CriterionSeed> CRITERIA = List.of(
        new CriterionSeed(CRITERION_PRONUNCIATION, "Phát âm",
            "Độ chính xác của âm, trọng âm và ngữ điệu; mức công sức người nghe phải bỏ ra để hiểu.", 1),
        new CriterionSeed(CRITERION_FLUENCY, "Độ trôi chảy",
            "Tốc độ, nhịp nói và cách xử lý khi ngập ngừng hoặc phải sửa lời.", 2),
        new CriterionSeed(CRITERION_GRAMMAR, "Ngữ pháp",
            "Độ chính xác và độ đa dạng của cấu trúc câu được dùng.", 3),
        new CriterionSeed(CRITERION_VOCABULARY, "Từ vựng",
            "Độ rộng của vốn từ và độ chuẩn xác khi chọn từ theo ngữ cảnh.", 4),
        new CriterionSeed(CRITERION_COHERENCE, "Tính mạch lạc",
            "Cách tổ chức ý và dùng liên từ để người nghe theo được mạch lập luận.", 5)
    );

    /**
     * Xếp loại theo Thông tư 22/2021/TT-BGDĐT quy đổi sang thang 10.
     *
     * <p>Ranh giới lấy tới hai chữ số thập phân (4.99 / 6.49 / 7.99) để các dải không chồng lấn mà
     * cũng không hở: điểm tổng lưu ở {@code numeric(5,2)} nên không tồn tại giá trị nào rơi vào khe
     * giữa hai dải.
     */
    private static final List<ResultBandSeed> RESULT_BANDS = List.of(
        new ResultBandSeed("CHUA_DAT", "Chưa đạt",
            "Chưa đáp ứng yêu cầu cần đạt của chương trình; cần được hỗ trợ và đánh giá lại.",
            new BigDecimal("0.00"), new BigDecimal("4.99"), 1),
        new ResultBandSeed("DAT", "Đạt",
            "Đáp ứng được yêu cầu cần đạt, tuy còn hạn chế ở một số tiêu chí.",
            new BigDecimal("5.00"), new BigDecimal("6.49"), 2),
        new ResultBandSeed("KHA", "Khá",
            "Đáp ứng tốt yêu cầu cần đạt ở phần lớn tiêu chí, giao tiếp trôi chảy trong tình huống quen thuộc.",
            new BigDecimal("6.50"), new BigDecimal("7.99"), 3),
        new ResultBandSeed("TOT", "Tốt",
            "Đáp ứng vượt yêu cầu cần đạt: diễn đạt tự tin, chính xác và có tổ chức ở cả chủ đề ngoài phạm vi quen thuộc.",
            new BigDecimal("8.00"), new BigDecimal("10.00"), 4)
    );

    /**
     * Ba bản mẫu, khác nhau ở cách phân bổ trọng số theo trọng tâm dạy học của từng khối.
     *
     * <p>Mỗi bộ trọng số cộng lại đúng 1.00 -- điều kiện của cách tính Phân bổ. Con số ở đây là một
     * đề xuất sư phạm chứ không phải quy định: trường sao về rồi chỉnh lại tuỳ ý, vì bản sao là tài
     * sản riêng của trường.
     */
    private static final List<TemplateSeed> TEMPLATES = List.of(
        new TemplateSeed("SYS-ENG-K10", "Bộ tiêu chí nói Tiếng Anh - Khối 10",
            "Bản mẫu của hệ thống cho khối 10, nhấn vào nền tảng phát âm và vốn từ. "
                + "Trường sao về rồi tự điều chỉnh trọng số, tiêu chí và thang xếp loại theo thực tế.",
            Map.of(
                CRITERION_PRONUNCIATION, new BigDecimal("0.20"),
                CRITERION_FLUENCY, new BigDecimal("0.20"),
                CRITERION_GRAMMAR, new BigDecimal("0.20"),
                CRITERION_VOCABULARY, new BigDecimal("0.20"),
                CRITERION_COHERENCE, new BigDecimal("0.20"))),
        new TemplateSeed("SYS-ENG-K11", "Bộ tiêu chí nói Tiếng Anh - Khối 11",
            "Bản mẫu của hệ thống cho khối 11, chuyển trọng tâm từ nền tảng sang khả năng diễn đạt liên tục. "
                + "Trường sao về rồi tự điều chỉnh trọng số, tiêu chí và thang xếp loại theo thực tế.",
            Map.of(
                CRITERION_PRONUNCIATION, new BigDecimal("0.20"),
                CRITERION_FLUENCY, new BigDecimal("0.20"),
                CRITERION_GRAMMAR, new BigDecimal("0.20"),
                CRITERION_VOCABULARY, new BigDecimal("0.20"),
                CRITERION_COHERENCE, new BigDecimal("0.20"))),
        new TemplateSeed("SYS-ENG-K12", "Bộ tiêu chí nói Tiếng Anh - Khối 12",
            "Bản mẫu của hệ thống cho khối 12, nhấn vào độ trôi chảy và khả năng tổ chức lập luận. "
                + "Trường sao về rồi tự điều chỉnh trọng số, tiêu chí và thang xếp loại theo thực tế.",
            Map.of(
                CRITERION_PRONUNCIATION, new BigDecimal("0.20"),
                CRITERION_FLUENCY, new BigDecimal("0.20"),
                CRITERION_GRAMMAR, new BigDecimal("0.20"),
                CRITERION_VOCABULARY, new BigDecimal("0.20"),
                CRITERION_COHERENCE, new BigDecimal("0.20")))
    );

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;

    public SystemRubricTemplateInitializer(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricResultBandRepository rubricResultBandRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
    }

    /**
     * {@code @Transactional} because this is a graph with foreign keys (rubric -> version -> criteria + result
     * bands). Dying midway without rolling back means the "is there a template yet" check on the next startup
     * sees one and skips, leaving a half-built version that is never fixed.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        SupportedLanguage language = supportedLanguageRepository.findByCode(LANGUAGE_CODE).orElse(null);
        if (language == null) {
            LOGGER.warn("Chưa có ngôn ngữ {}. Bỏ qua khởi tạo bộ tiêu chí mẫu của hệ thống", LANGUAGE_CODE);
            return;
        }

        // A single check for all three templates: if the system already has any rubric for this language, do
        // not insert opinionated templates on top of it. That means if the system admin has already created a rubric
        // for English by hand, this seed will no longer run -- to get the templates, create them by hand, like
        // FrameworkInitializer.
        if (rubricRepository.existsByOwnerTypeAndLanguageId(RubricOwnerType.SYSTEM.name(), language.getId())) {
            LOGGER.info("Hệ thống đã có bộ tiêu chí cho ngôn ngữ {}. Bỏ qua khởi tạo bộ tiêu chí mẫu",
                LANGUAGE_CODE);
            return;
        }

        Framework framework = frameworkRepository.findByCode(FRAMEWORK_CODE).orElse(null);
        if (framework == null || !framework.isActive()) {
            LOGGER.warn("Chưa có khung {} đang hoạt động. Bỏ qua khởi tạo bộ tiêu chí mẫu của hệ thống",
                FRAMEWORK_CODE);
            return;
        }

        FrameworkVersion frameworkVersion = latestPublishedVersion(framework.getId());
        if (frameworkVersion == null) {
            LOGGER.warn("Khung {} chưa có phiên bản nào được ban hành. Bỏ qua khởi tạo bộ tiêu chí mẫu",
                FRAMEWORK_CODE);
            return;
        }

        // A rubric criterion must point to a real framework_criterion_id, so look it up by the framework's code instead
        // of guessing. Any missing code means the framework has been changed -- stopping is better than building a template
        // that lacks criteria and then cannot be published.
        Map<String, FrameworkCriterion> criterionByCode = frameworkCriterionRepository
            .findByFrameworkVersionId(frameworkVersion.getId()).stream()
            .collect(Collectors.toMap(criterion -> criterion.getCode(), Function.identity(), (first, second) -> first));

        List<String> missingCodes = CRITERIA.stream()
            .map(seed -> seed.frameworkCriterionCode())
            .filter(code -> !criterionByCode.containsKey(code))
            .toList();
        if (!missingCodes.isEmpty()) {
            LOGGER.warn("Khung {} thiếu các tiêu chí {}. Bỏ qua khởi tạo bộ tiêu chí mẫu của hệ thống",
                FRAMEWORK_CODE, missingCodes);
            return;
        }

        Instant now = Instant.now();
        for (TemplateSeed template : TEMPLATES) {
            createTemplate(template, language.getId(), framework.getId(), criterionByCode, now);
        }

        LOGGER.info("Đã khởi tạo {} bộ tiêu chí mẫu của hệ thống cho ngôn ngữ {}", TEMPLATES.size(), LANGUAGE_CODE);
    }

    private FrameworkVersion latestPublishedVersion(UUID frameworkId) {
        return frameworkVersionRepository
            .findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED).stream()
            .max(Comparator.comparingInt(v -> v.getVersion()))
            .orElse(null);
    }

    private void createTemplate(
            TemplateSeed template,
            UUID languageId,
            UUID frameworkId,
            Map<String, FrameworkCriterion> criterionByCode,
            Instant now) {

        Rubric rubric = rubricRepository.save(Rubric.builder()
                .languageId(languageId)
                .frameworkId(frameworkId)
                .code(template.code())
                .name(template.name())
                .description(template.description())
                .ownerType(RubricOwnerType.SYSTEM)
                // SYSTEM requires school_id NULL -- chk_rubrics_owner_school_valid.
                .schoolId(null)
                .build());

        RubricVersion version = rubricVersionRepository.save(RubricVersion.builder()
                .rubricId(rubric.getId())
                .version(1)
                .code(template.code() + "-V1")
                .name(template.name())
                .description(template.description())
                // PUBLISHED right away: a template is only for reading and copying, and CloneSystemRubricToSchoolUseCase rejects
                // every unpublished version. A template in draft state is one nobody can use.
                .status(RubricStatus.PUBLISHED)
                .effectiveFrom(now)
                .effectiveTo(null)
                .scoringScaleMin(SCORING_SCALE_MIN)
                .scoringScaleMax(SCORING_SCALE_MAX)
                .totalScoreMethod(RubricTotalScoreMethod.SUM)
                .build());

        List<RubricCriterion> criteria = CRITERIA.stream()
            .map(seed -> RubricCriterion.builder()
                    .rubricVersionId(version.getId())
                    .frameworkCriterionId(criterionByCode.get(seed.frameworkCriterionCode()).getId())
                    .code(seed.frameworkCriterionCode())
                    .name(seed.name())
                    .description(seed.description())
                    // No sample examples: an example is tied to each school's own assessment and grading style, and putting one in here
                    // would make every copy carry an example that is not its own.
                    .examples(null)
                    .weight(template.weights().get(seed.frameworkCriterionCode()))
                    .minScore(SCORING_SCALE_MIN)
                    .maxScore(SCORING_SCALE_MAX)
                    .order(seed.order())
                    .required(true)
                    .build())
            .toList();

        // Checked with exactly the rules of the real publish path, before writing. Wrong numbers in this file will break
        // at startup -- loudly and early -- instead of silently producing a template that a school copies over and
        // can never publish.
        RubricScoringConsistencyValidator.assertPublishable(
            RubricTotalScoreMethod.SUM, SCORING_SCALE_MIN, SCORING_SCALE_MAX, criteria);

        rubricCriterionRepository.saveAll(criteria);

        rubricResultBandRepository.saveAll(RESULT_BANDS.stream()
            .map(seed -> RubricResultBand.builder()
                    .rubricVersionId(version.getId())
                    .code(seed.code())
                    .name(seed.name())
                    .description(seed.description())
                    .scoreMin(seed.scoreMin())
                    .scoreMax(seed.scoreMax())
                    .order(seed.order())
                    .build())
            .toList());
    }

    private record CriterionSeed(String frameworkCriterionCode, String name, String description, int order) {}

    private record ResultBandSeed(
        String code, String name, String description, BigDecimal scoreMin, BigDecimal scoreMax, int order) {}

    /** @param weights trọng số theo mã tiêu chí của khung; tổng phải bằng 1.00. */
    private record TemplateSeed(String code, String name, String description, Map<String, BigDecimal> weights) {}
}
