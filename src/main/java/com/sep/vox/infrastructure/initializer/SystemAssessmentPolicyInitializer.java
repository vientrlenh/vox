package com.sep.vox.infrastructure.initializer;

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
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
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
 * Dựng sẵn ba chính sách chấm MẪU của hệ thống, mỗi khối THPT một bản, trỏ vào đúng bộ tiêu chí mẫu
 * cùng khối do {@link SystemRubricTemplateInitializer} tạo ra.
 *
 * <p>Đây là thứ làm cho {@code CloneSystemAssessmentPolicyToSchoolUseCase} có cái để sao. Trước khi
 * có file này, trường muốn dùng bản mẫu phải sao rubric rồi tự gõ lại toàn bộ thông số chính sách --
 * chính là bước thủ công mà use case sao chép sinh ra để bỏ đi.
 *
 * <h2>PUBLISHED ngay, và vì sao phải đi vòng qua use case</h2>
 *
 * <p>{@code CloneSystemAssessmentPolicyToSchoolUseCase} từ chối mọi bản mẫu chưa ban hành, nên chính
 * sách mẫu ở trạng thái nháp là chính sách không ai sao được.
 *
 * <p>Không gọi {@code CreateSystemAssessmentPolicyUseCase} được vì hai lẽ. Một, lúc khởi động chưa có
 * phiên đăng nhập nào để use case đó lấy danh tính system admin. Hai, use case đó bắt phiên bản rubric
 * phải còn DRAFT ("Chỉ được gán Policy khi Phiên bản Rubric còn ở trạng thái DRAFT") vì luồng thật là
 * tạo chính sách nháp trên rubric nháp rồi ban hành cả hai cùng lúc; còn bộ tiêu chí mẫu ở đây do
 * {@link SystemRubricTemplateInitializer} dựng thẳng ở trạng thái PUBLISHED. Trạng thái cuối mà file
 * này ghi ra -- chính sách PUBLISHED trỏ vào rubric PUBLISHED -- đúng bằng kết quả của luồng thật, chỉ
 * bỏ qua bước trung gian.
 *
 * <p>Đổi lại, các bất biến của đường tạo thật phải giữ bằng tay ở đây: khung còn PUBLISHED, bậc mục
 * tiêu thuộc đúng phiên bản khung, rubric thuộc sở hữu SYSTEM và cùng khung với chính sách, và mỗi
 * phạm vi chỉ một chính sách còn hiệu lực.
 *
 * <h2>Ba bản theo Khối, không phải một bản dùng chung</h2>
 *
 * <p>Khối lớp là catalog toàn cục và không gắn với niên khóa nào, nên chính sách mẫu khai theo Khối
 * vẫn đúng khi trường mở niên khóa năm sau -- không phải khai lại hằng năm. Đó cũng là lý do
 * {@code resolveScope} của use case sao chép bắt bản sao GIỮ NGUYÊN khối của bản mẫu: đổi khối là
 * giữ nguyên thông số soạn cho Khối 10 rồi dán nhãn Khối 12.
 *
 * <h2>Bậc mục tiêu giống nhau ở cả ba khối</h2>
 *
 * <p>Cả ba cùng {@code BAC_3}. Theo Thông tư 32/2018/TT-BGDĐT thì chuẩn đầu ra là hết THPT đạt Bậc 3,
 * không phân theo từng lớp. Xem {@link GradeLevelBandScopeInitializer} để biết vì sao khung 6 bậc
 * nguyên không diễn đạt được khác biệt 3.1/3.2/3.3 giữa ba khối.
 *
 * <p>Giữ bậc mục tiêu cố định còn có cái lợi riêng: bậc mục tiêu quyết định bộ mô tả nào được gửi sang
 * AI chấm ({@code SubmitExamSessionUseCase.buildCriteriaFrameworks}), nên để nguyên nghĩa là điểm thô
 * của Khối 10 và Khối 12 đo bằng cùng một thước và so sánh được qua các năm. Khác biệt giữa các khối
 * nằm ở thang xếp loại của từng bộ tiêu chí, tức là ở tầng BÁO CÁO, không phải ở tầng chấm.
 *
 * <h2>Không đặt điểm đạt</h2>
 *
 * <p>{@code passingScore} để null (nghĩa là không kiểm điểm đạt). Văn bản chính thức quy định BẬC cần
 * đạt chứ không quy định ngưỡng điểm; đặt sẵn một con số ở đây là phần duy nhất của bộ seed không có
 * căn cứ nào đứng sau. Trường tự đặt sau khi sao về.
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
     * Mỗi khối gắn với đúng phiên bản bộ tiêu chí mẫu cùng khối.
     *
     * <p>Mã phiên bản theo quy ước của {@link SystemRubricTemplateInitializer}: mã bản mẫu + "-V1".
     * Đổi mã bên đó mà quên đổi ở đây thì initializer này không tìm thấy rubric và bỏ qua khối tương
     * ứng -- có log cảnh báo chứ không âm thầm.
     */
    private static final List<PolicySeed> POLICIES = List.of(
        new PolicySeed(GradeLevelInitializer.GRADE_10, "SYS-ENG-K10-V1"),
        new PolicySeed(GradeLevelInitializer.GRADE_11, "SYS-ENG-K11-V1"),
        new PolicySeed(GradeLevelInitializer.GRADE_12, "SYS-ENG-K12-V1")
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
     * Idempotent theo từng phạm vi, dùng đúng phép kiểm mà đường tạo thật dùng
     * ({@code existsActiveForScopeAnyRubricVersion}). Nghĩa là system admin đã tự tạo chính sách cho
     * Khối 10 rồi thì seed không chèn bản thứ hai đè lên, nhưng Khối 11 và 12 vẫn được dựng.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        var language = supportedLanguageRepository.findByCode(LANGUAGE_CODE).orElse(null);
        if (language == null) {
            LOGGER.warn("Chưa có ngôn ngữ {}. Bỏ qua khởi tạo chính sách chấm mẫu của hệ thống", LANGUAGE_CODE);
            return;
        }

        var framework = frameworkRepository.findByCode(FRAMEWORK_CODE).orElse(null);
        if (framework == null || !framework.isActive()) {
            LOGGER.warn("Chưa có khung {} đang hoạt động. Bỏ qua khởi tạo chính sách chấm mẫu", FRAMEWORK_CODE);
            return;
        }

        var frameworkVersion = latestPublishedVersion(framework.getId());
        if (frameworkVersion == null) {
            LOGGER.warn("Khung {} chưa có phiên bản nào được ban hành. Bỏ qua khởi tạo chính sách chấm mẫu",
                FRAMEWORK_CODE);
            return;
        }

        var targetBand = frameworkResultBandRepository
            .findByVersionIdAndCode(frameworkVersion.getId(), TARGET_BAND_CODE).orElse(null);
        if (targetBand == null) {
            LOGGER.warn("Phiên bản khung {} thiếu bậc {}. Bỏ qua khởi tạo chính sách chấm mẫu",
                frameworkVersion.getCode(), TARGET_BAND_CODE);
            return;
        }

        var now = Instant.now();
        var created = 0;
        for (var seed : POLICIES) {
            if (createPolicy(seed, language.getId(), framework.getId(), frameworkVersion, targetBand, now)) {
                created++;
            }
        }

        if (created == 0) {
            LOGGER.info("Chính sách chấm mẫu của hệ thống đã đầy đủ. Bỏ qua khởi tạo");
            return;
        }
        LOGGER.info("Đã khởi tạo {} chính sách chấm mẫu của hệ thống (ngôn ngữ {}, khung {}, bậc mục tiêu {})",
            created, LANGUAGE_CODE, frameworkVersion.getCode(), TARGET_BAND_CODE);
    }

    /** @return true nếu có ghi ra một chính sách mới. */
    private boolean createPolicy(
            PolicySeed seed,
            UUID languageId,
            UUID frameworkId,
            FrameworkVersion frameworkVersion,
            FrameworkResultBand targetBand,
            Instant now) {

        var gradeLevel = gradeLevelRepository.findByCode(seed.gradeLevelCode()).orElse(null);
        if (gradeLevel == null) {
            LOGGER.warn("Không tìm thấy khối {}. Bỏ qua chính sách chấm mẫu cho khối này", seed.gradeLevelCode());
            return false;
        }

        // Phép kiểm trùng của chính đường tạo thật: một phạm vi chỉ được đúng một chính sách còn
        // hiệu lực (DRAFT hoặc PUBLISHED), bất kể trỏ vào phiên bản rubric nào.
        if (assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(
                null, languageId, frameworkVersion.getId(), gradeLevel.getId(), null, null)) {
            return false;
        }

        var rubricVersion = rubricVersionRepository.findByCode(seed.rubricVersionCode()).orElse(null);
        if (rubricVersion == null) {
            LOGGER.warn("Không tìm thấy phiên bản bộ tiêu chí mẫu {}. Bỏ qua chính sách chấm mẫu cho khối {}",
                seed.rubricVersionCode(), seed.gradeLevelCode());
            return false;
        }
        var rubric = rubricRepository.findById(rubricVersion.getRubricId()).orElse(null);
        if (rubric == null || rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            LOGGER.warn("Bộ tiêu chí của phiên bản {} không thuộc sở hữu SYSTEM. Bỏ qua chính sách chấm mẫu"
                + " cho khối {}", seed.rubricVersionCode(), seed.gradeLevelCode());
            return false;
        }
        // Cùng phép kiểm CreateSystemAssessmentPolicyUseCase áp: rubric gắn với FRAMEWORK (không
        // phải phiên bản khung), nên so với getFrameworkId của phiên bản đang dùng.
        if (!rubric.getFrameworkId().equals(frameworkId)) {
            LOGGER.warn("Bộ tiêu chí {} không cùng khung với chính sách. Bỏ qua chính sách chấm mẫu cho khối {}",
                seed.rubricVersionCode(), seed.gradeLevelCode());
            return false;
        }

        int nextVersion = assessmentPolicyRepository.findMaxVersionForScope(
            null, languageId, frameworkVersion.getId(), gradeLevel.getId(), null, null) + 1;

        assessmentPolicyRepository.save(new AssessmentPolicy(
            // schoolId null = chính sách của hệ thống, không thuộc trường nào.
            null,
            gradeLevel.getId(),
            // Niên khóa và Lớp thuộc phạm vi của một trường cụ thể nên chính sách hệ thống luôn để trống.
            null,
            null,
            languageId,
            frameworkVersion.getId(),
            rubricVersion.getId(),
            targetBand.getId(),
            // Xem javadoc lớp: văn bản quy định bậc, không quy định ngưỡng điểm.
            null,
            AssessmentPolicyStrictness.STANDARD,
            nextVersion,
            AssessmentPolicyStatus.PUBLISHED,
            // effectiveTo null = không giới hạn. Bản mẫu chỉ để đọc và sao nên không có lý do hết hạn;
            // khoảng hiệu lực thật là thứ trường tự đặt lúc sao về.
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
     * @param gradeLevelCode     mã khối trong catalog toàn cục.
     * @param rubricVersionCode  mã phiên bản bộ tiêu chí mẫu tương ứng của hệ thống.
     */
    private record PolicySeed(String gradeLevelCode, String rubricVersionCode) {}
}
