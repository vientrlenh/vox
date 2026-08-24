package com.sep.vox.infrastructure.initializer;

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
 * Dựng sẵn ba bộ tiêu chí MẪU của hệ thống (khối 10, 11, 12) để trường có cái mà sao về ngay từ lần
 * khởi động đầu.
 *
 * <p>Khác {@link FrameworkInitializer} ở chỗ đây KHÔNG phải dữ liệu bắt buộc: thiếu nó hệ thống vẫn
 * chạy, trường vẫn tự soạn bộ tiêu chí từ đầu được. Nó tồn tại để chức năng sao chép có nội dung, và
 * để trường mới có một điểm khởi đầu hợp lý thay vì một trang trắng.
 *
 * <h2>Ba bản mẫu, không phải ba bản × hai cách tính</h2>
 *
 * <p>Cả ba đều khai ở dạng PHÂN BỔ ({@code SUM}). Không dựng thêm bản Trung bình song song, vì Trung
 * bình suy được từ Phân bổ lúc sao chép -- {@code RubricCloneService} san mọi trọng số về 1 khi
 * trường chọn cách tính đó -- còn chiều ngược lại thì không: từ trạng thái mọi tiêu chí bằng nhau
 * không có cách nào tái tạo tỉ lệ mà bản mẫu định ra. Bản Phân bổ mang nhiều thông tin hơn, nên nó
 * là dạng đáng lưu.
 *
 * <h2>Chính sách đánh giá đi kèm nằm ở initializer khác</h2>
 *
 * <p>{@link SystemAssessmentPolicyInitializer} chạy sau ({@code @Order(9)}) và dựng cho mỗi khối một
 * chính sách mẫu trỏ vào đúng bản mẫu cùng khối ở đây. Hai bên nối với nhau bằng MÃ PHIÊN BẢN
 * ({@code SYS-ENG-K10-V1}...), tức là quy ước {@code code + "-V1"} bên dưới là thứ đi ra ngoài file
 * này -- đổi nó thì phải đổi cả bảng tra bên đó.
 *
 * <p>Ghi chú lịch sử: trước đây chỗ này giải thích là cố ý KHÔNG dựng chính sách hệ thống, vì
 * {@code findCandidatePolicies} lọc {@code WHERE p.schoolId = :schoolId} nên chính sách không thuộc
 * trường nào sẽ không bao giờ được chọn. Lý do đó không còn mô tả đúng hệ thống đang chạy: hai hàm
 * phân giải phạm vi ấy nay là mã chết (đường chấm thật đọc {@code exams.assessment_policy_id}), và
 * quan trọng hơn, chính sách mẫu của hệ thống không tồn tại để chấm -- nó tồn tại để trường SAO VỀ
 * kèm bộ tiêu chí qua {@code CloneSystemRubricToSchoolUseCase}. Bản sao mới là bản đi chấm.
 *
 * <h2>Ghi thẳng qua repository</h2>
 *
 * <p>Đi vòng qua {@code CreateSystemRubricUseCase} và {@code ChangeSystemRubricVersionStatusUseCase}
 * vì lúc khởi động chưa có người dùng nào đăng nhập để các use case đó lấy danh tính. Đổi lại, các
 * bất biến mà đường publish bắt buộc phải được giữ bằng tay ở đây: khung gốc còn hoạt động, phiên
 * bản có tiêu chí, và trọng số/thang điểm nhất quán. Điều kiện cuối được kiểm bằng chính
 * {@link RubricScoringConsistencyValidator} của đường publish thật, nên sai số liệu trong file này
 * làm hỏng lúc khởi động chứ không âm thầm sinh ra bản mẫu không ban hành lại được.
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
     * {@code @Transactional} vì đây là một đồ thị có khoá ngoại (rubric -> version -> tiêu chí + dải
     * kết quả). Chết giữa chừng mà không cuộn lại thì phép kiểm "đã có bản mẫu chưa" ở lần khởi động
     * sau sẽ thấy có rồi và bỏ qua, để lại một bản dựng dở không bao giờ được sửa.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        var language = supportedLanguageRepository.findByCode(LANGUAGE_CODE).orElse(null);
        if (language == null) {
            LOGGER.warn("Chưa có ngôn ngữ {}. Bỏ qua khởi tạo bộ tiêu chí mẫu của hệ thống", LANGUAGE_CODE);
            return;
        }

        // Một phép kiểm duy nhất cho cả ba bản: hệ thống đã có bộ tiêu chí nào cho ngôn ngữ này thì
        // không chèn thêm bản mẫu có sẵn ý kiến lên trên. Nghĩa là nếu system admin đã tự tạo rubric
        // cho tiếng Anh, seed này sẽ không chạy nữa -- muốn có bản mẫu thì tạo tay, giống
        // FrameworkInitializer.
        if (rubricRepository.existsByOwnerTypeAndLanguageId(RubricOwnerType.SYSTEM.name(), language.getId())) {
            LOGGER.info("Hệ thống đã có bộ tiêu chí cho ngôn ngữ {}. Bỏ qua khởi tạo bộ tiêu chí mẫu",
                LANGUAGE_CODE);
            return;
        }

        var framework = frameworkRepository.findByCode(FRAMEWORK_CODE).orElse(null);
        if (framework == null || !framework.isActive()) {
            LOGGER.warn("Chưa có khung {} đang hoạt động. Bỏ qua khởi tạo bộ tiêu chí mẫu của hệ thống",
                FRAMEWORK_CODE);
            return;
        }

        var frameworkVersion = latestPublishedVersion(framework.getId());
        if (frameworkVersion == null) {
            LOGGER.warn("Khung {} chưa có phiên bản nào được ban hành. Bỏ qua khởi tạo bộ tiêu chí mẫu",
                FRAMEWORK_CODE);
            return;
        }

        // Tiêu chí rubric phải trỏ tới framework_criterion_id có thật, nên tra theo mã của khung thay
        // vì đoán. Thiếu bất kỳ mã nào là khung đã bị sửa khác đi -- dừng lại còn hơn dựng ra bản mẫu
        // khuyết tiêu chí rồi không ban hành được.
        Map<String, FrameworkCriterion> criterionByCode = frameworkCriterionRepository
            .findByFrameworkVersionId(frameworkVersion.getId()).stream()
            .collect(Collectors.toMap(criterion -> criterion.getCode(), Function.identity(), (first, second) -> first));

        var missingCodes = CRITERIA.stream()
            .map(seed -> seed.frameworkCriterionCode())
            .filter(code -> !criterionByCode.containsKey(code))
            .toList();
        if (!missingCodes.isEmpty()) {
            LOGGER.warn("Khung {} thiếu các tiêu chí {}. Bỏ qua khởi tạo bộ tiêu chí mẫu của hệ thống",
                FRAMEWORK_CODE, missingCodes);
            return;
        }

        var now = Instant.now();
        for (var template : TEMPLATES) {
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

        var rubric = rubricRepository.save(new Rubric(
            languageId,
            frameworkId,
            template.code(),
            template.name(),
            template.description(),
            RubricOwnerType.SYSTEM,
            // SYSTEM bắt buộc school_id NULL -- chk_rubrics_owner_school_valid.
            null
        ));

        var version = rubricVersionRepository.save(new RubricVersion(
            rubric.getId(),
            1,
            template.code() + "-V1",
            template.name(),
            template.description(),
            // PUBLISHED ngay: bản mẫu chỉ để đọc và sao, và CloneSystemRubricToSchoolUseCase từ chối
            // mọi phiên bản chưa ban hành. Bản mẫu ở trạng thái nháp là bản không ai dùng được.
            RubricStatus.PUBLISHED,
            now,
            null,
            SCORING_SCALE_MIN,
            SCORING_SCALE_MAX,
            RubricTotalScoreMethod.SUM,
            now,
            now,
            // Lúc khởi động không có phiên đăng nhập nào, nên không có danh tính để ghi vào
            // created_by/updated_by. Hai cột này nullable, và null ở đây đọc đúng nghĩa: bản này do
            // hệ thống dựng, không phải do người nào tạo.
            null,
            null
        ));

        var criteria = CRITERIA.stream()
            .map(seed -> new RubricCriterion(
                version.getId(),
                criterionByCode.get(seed.frameworkCriterionCode()).getId(),
                seed.frameworkCriterionCode(),
                seed.name(),
                seed.description(),
                // Không kèm ví dụ mẫu: ví dụ là thứ gắn với đề và cách chấm của từng trường, đặt sẵn
                // ở đây thì bản sao nào cũng mang theo một ví dụ không phải của mình.
                null,
                template.weights().get(seed.frameworkCriterionCode()),
                SCORING_SCALE_MIN,
                SCORING_SCALE_MAX,
                seed.order(),
                true,
                now,
                now,
                null,
                null))
            .toList();

        // Kiểm bằng đúng luật của đường publish thật, trước khi ghi. Sai số liệu trong file này sẽ làm
        // hỏng lúc khởi động -- to và sớm -- thay vì âm thầm sinh ra bản mẫu mà trường sao về rồi
        // không bao giờ ban hành được.
        RubricScoringConsistencyValidator.assertPublishable(
            RubricTotalScoreMethod.SUM, SCORING_SCALE_MIN, SCORING_SCALE_MAX, criteria);

        rubricCriterionRepository.saveAll(criteria);

        rubricResultBandRepository.saveAll(RESULT_BANDS.stream()
            .map(seed -> new RubricResultBand(
                version.getId(),
                seed.code(),
                seed.name(),
                seed.description(),
                seed.scoreMin(),
                seed.scoreMax(),
                seed.order(),
                now,
                now,
                null,
                null))
            .toList());
    }

    private record CriterionSeed(String frameworkCriterionCode, String name, String description, int order) {}

    private record ResultBandSeed(
        String code, String name, String description, BigDecimal scoreMin, BigDecimal scoreMax, int order) {}

    /** @param weights trọng số theo mã tiêu chí của khung; tổng phải bằng 1.00. */
    private record TemplateSeed(String code, String name, String description, Map<String, BigDecimal> weights) {}
}
