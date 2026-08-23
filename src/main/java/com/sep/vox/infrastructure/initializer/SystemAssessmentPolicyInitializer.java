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
 * Dựng sẵn sáu chính sách chấm MẪU của hệ thống: mỗi bộ tiêu chí mẫu THPT do
 * {@link SystemRubricTemplateInitializer} tạo ra có một bản chuẩn (Bậc 3, khai theo Khối) và một bản
 * cho lớp chuyên (Bậc 4, không khai phạm vi).
 *
 * <p>Đây là thứ làm cho {@code CloneSystemRubricToSchoolUseCase} có chính sách để sao kèm bộ tiêu
 * chí. Trước khi có file này, trường muốn dùng bản mẫu phải sao rubric rồi tự gõ lại toàn bộ thông
 * số chính sách -- chính là bước thủ công mà đường sao chép sinh ra để bỏ đi.
 *
 * <h2>PUBLISHED ngay, và vì sao phải đi vòng qua use case</h2>
 *
 * <p>{@code CloneSystemRubricToSchoolUseCase} từ chối mọi bản mẫu chưa ban hành, nên chính sách mẫu
 * ở trạng thái nháp là chính sách không ai sao được.
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
 * tiêu thuộc đúng phiên bản khung, rubric thuộc sở hữu SYSTEM và cùng khung với chính sách.
 *
 * <h2>Bản chuẩn khai theo Khối, bản chuyên để trống phạm vi</h2>
 *
 * <p>Khối lớp là catalog toàn cục và không gắn với niên khóa nào, nên chính sách mẫu khai theo Khối
 * vẫn đúng khi trường mở niên khóa năm sau -- không phải khai lại hằng năm. Đó cũng là lý do
 * {@code resolveScope} của use case sao chép bắt bản sao GIỮ NGUYÊN khối của bản mẫu: đổi khối là
 * giữ nguyên thông số soạn cho Khối 10 rồi dán nhãn Khối 12.
 *
 * <p>Chính điều đó khiến bản CHUYÊN không được khai Khối: bản sao sẽ bị ghim vào cả khối, trong khi
 * thứ trường cần là áp riêng cho lớp chuyên. Thêm nữa mỗi phạm vi chỉ được một chính sách còn hiệu
 * lực, nên bản chuẩn và bản chuyên không cùng tồn tại ở phạm vi Khối được. Để trống phạm vi là cách
 * duy nhất để trường sao bản chuẩn cho Khối/Niên khóa và bản chuyên cho đúng lớp chuyên.
 *
 * <h2>Hai bậc mục tiêu, không phải ba khối ba bậc</h2>
 *
 * <p>Bản chuẩn của cả ba khối cùng {@code BAC_3}. Theo Thông tư 32/2018/TT-BGDĐT thì chuẩn đầu ra là
 * hết THPT đạt Bậc 3, không phân theo từng lớp. Xem {@link GradeLevelBandScopeInitializer} để biết vì
 * sao khung 6 bậc nguyên không diễn đạt được khác biệt 3.1/3.2/3.3 giữa ba khối.
 *
 * <p>Bản chuyên nhắm {@code BAC_4} (CEFR B2) -- đúng bằng TRẦN CỨNG mà bảng trần đặt cho cả ba khối,
 * nên bản sao luôn lọt {@code GradeLevelBandScopeGuardService}. B2 là thông lệ của trường chuyên chứ
 * không phải quy định (Thông tư 05/2023/TT-BGDĐT không đặt chuẩn ngoại ngữ nào cho lớp chuyên), nên
 * nó ở đây dưới dạng bản mẫu tuỳ chọn: trường nào không có lớp chuyên thì không sao bản này.
 *
 * <p>Bậc mục tiêu quyết định bộ mô tả nào được gửi sang AI chấm
 * ({@code SubmitExamSessionUseCase.buildCriteriaFrameworks}), nên hai bậc nghĩa là hai thước đo khác
 * nhau -- điểm thô của lớp chuyên và lớp thường không so trực tiếp được. Đó là chủ đích: đây là hai
 * nhóm học sinh có đích cần đạt khác nhau.
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
     * Idempotent theo từng BẢN MẪU, khoá là cặp (phiên bản bộ tiêu chí, bậc mục tiêu) -- xem
     * {@link #createPolicy}. Nghĩa là bản nào đã có thì bỏ qua, các bản còn lại vẫn được dựng, và
     * chạy lại nhiều lần không sinh thêm gì.
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

        var now = Instant.now();
        var created = 0;
        for (var seed : POLICIES) {
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

    /** @return true nếu có ghi ra một chính sách mới. */
    private boolean createPolicy(
            PolicySeed seed,
            UUID languageId,
            UUID frameworkId,
            FrameworkVersion frameworkVersion,
            Instant now) {

        var targetBand = frameworkResultBandRepository
            .findByVersionIdAndCode(frameworkVersion.getId(), seed.targetBandCode()).orElse(null);
        if (targetBand == null) {
            LOGGER.warn("Phiên bản khung {} thiếu bậc {}. Bỏ qua chính sách chấm mẫu tương ứng",
                frameworkVersion.getCode(), seed.targetBandCode());
            return false;
        }

        // Bản chuyên không khai Khối: phạm vi để trống thì trường tự chọn lúc sao về.
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

        var rubricVersion = rubricVersionRepository.findByCode(seed.rubricVersionCode()).orElse(null);
        if (rubricVersion == null) {
            LOGGER.warn("Không tìm thấy phiên bản bộ tiêu chí mẫu {}. Bỏ qua chính sách chấm mẫu bậc {}",
                seed.rubricVersionCode(), seed.targetBandCode());
            return false;
        }
        var rubric = rubricRepository.findById(rubricVersion.getRubricId()).orElse(null);
        if (rubric == null || rubric.getOwnerType() != RubricOwnerType.SYSTEM) {
            LOGGER.warn("Bộ tiêu chí của phiên bản {} không thuộc sở hữu SYSTEM. Bỏ qua chính sách chấm mẫu"
                + " bậc {}", seed.rubricVersionCode(), seed.targetBandCode());
            return false;
        }
        // Cùng phép kiểm CreateSystemAssessmentPolicyUseCase áp: rubric gắn với FRAMEWORK (không
        // phải phiên bản khung), nên so với getFrameworkId của phiên bản đang dùng.
        if (!rubric.getFrameworkId().equals(frameworkId)) {
            LOGGER.warn("Bộ tiêu chí {} không cùng khung với chính sách. Bỏ qua chính sách chấm mẫu bậc {}",
                seed.rubricVersionCode(), seed.targetBandCode());
            return false;
        }

        // Idempotent theo ĐÚNG danh tính của một bản mẫu: (phiên bản bộ tiêu chí, bậc mục tiêu).
        //
        // Trước đây chỗ này dùng existsActiveForScopeAnyRubricVersion -- phép kiểm "mỗi phạm vi một
        // chính sách" của đường tạo thật. Luật đó là bất biến của chính sách TRƯỜNG (mỗi lớp chỉ được
        // chấm theo một chính sách), không phải của thư mục bản mẫu: hai bản mẫu cùng khối khác bậc
        // là đúng thứ ta muốn dựng ra. Giữ nguyên nó thì bản chuyên bị bỏ qua âm thầm, và ba bản
        // chuyên (đều không khai khối) còn đè lên nhau vì cùng một phạm vi rỗng.
        //
        // Bỏ luật đó ở đây KHÔNG ảnh hưởng gì: chính sách hệ thống không đi chấm bài (đường chấm đọc
        // exams.assessment_policy_id), findActivePolicy không còn nơi gọi, và idx_assessment_policies
        // _scope_version không bắt được hàng nào có NULL trong khoá. Đường tạo tay của system admin
        // (CreateSystemAssessmentPolicyUseCase) vẫn giữ nguyên luật cũ.
        boolean alreadySeeded = assessmentPolicyRepository
            .findPublishedSystemWideByRubricVersionId(rubricVersion.getId()).stream()
            .anyMatch(existing -> targetBand.getId().equals(existing.getTargetFrameworkBandId()));
        if (alreadySeeded) {
            return false;
        }

        int nextVersion = assessmentPolicyRepository.findMaxVersionForScope(
            null, languageId, frameworkVersion.getId(), gradeLevelId, null, null) + 1;

        assessmentPolicyRepository.save(new AssessmentPolicy(
            // schoolId null = chính sách của hệ thống, không thuộc trường nào.
            null,
            gradeLevelId,
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
     * @param gradeLevelCode     mã khối trong catalog toàn cục; null = bản mẫu không khai phạm vi,
     *                           trường tự chọn lúc sao về (dùng cho bản lớp chuyên).
     * @param rubricVersionCode  mã phiên bản bộ tiêu chí mẫu tương ứng của hệ thống.
     * @param targetBandCode     mã bậc mục tiêu; đây là thứ phân biệt bản chuẩn với bản chuyên.
     */
    private record PolicySeed(String gradeLevelCode, String rubricVersionCode, String targetBandCode) {}
}
