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

import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;

/**
 * Khai trần bậc mục tiêu cho ba khối THPT trên phiên bản KNLNNVN đang ban hành.
 *
 * <p>Không có bảng này thì {@code GradeLevelBandScopeGuardService} chạy ở chế độ HỞ -- không tìm
 * thấy dòng cấu hình nào thì nó cho qua mọi bậc. Nghĩa là trần chỉ thực sự có hiệu lực kể từ khi
 * initializer này chạy; trước đó trường vẫn đặt được Bậc 6 cho Khối 10 như cũ.
 *
 * <h2>Hai loại bài kiểm tra, hai trần khác nhau</h2>
 *
 * <p>{@code BAND_CODE_DEFAULT_TARGET} là trần cho bài **CENTRALIZE** (kiểm tra tập trung -- policy
 * áp cho cả Khối hoặc cả Niên khóa, không neo vào 1 Lớp cụ thể): Khối 10/11/12 chỉ được chọn Bậc 3
 * hoặc Bậc 4. {@code BAND_CODE_HARD_MAX} là trần cho bài **CLASS_TEST** (lớp chuyên có bài kiểm
 * tra riêng -- policy neo đúng vào 1 {@code schoolClassId} cụ thể): được nới thêm tới Bậc 5. Xem
 * {@link GradeLevelBandScopeGuardService.Batch#effectiveCeilingBand} cho phần chọn trần theo
 * phạm vi lúc canh.
 *
 * <p>Cả ba khối dùng chung một cặp trần vì Chương trình GDPT 2018 môn Tiếng Anh (Thông tư
 * 32/2018/TT-BGDĐT) chốt chuẩn đầu ra theo CẤP HỌC chứ không theo từng lớp (hết THPT đạt Bậc 3),
 * và không có văn bản nào phân biệt CENTRALIZE/CLASS_TEST theo khối -- đây là quyết định nghiệp vụ
 * của dự án, dễ mở rộng thêm khi catalog khối có thêm cấp THCS.
 *
 * <p>Điều cần chặn tuyệt đối là Bậc 6 (ở cả hai loại bài kiểm tra). Trong giáo dục phổ thông không
 * có văn bản nào đặt C2 làm đích cho HỌC SINH; C1/C2 chính thức là chuẩn của GIÁO VIÊN tiếng Anh
 * THPT theo Đề án Ngoại ngữ Quốc gia. Nhầm hai chuẩn đó với nhau nhiều khả năng chính là nguồn gốc
 * của việc trường đặt bậc mục tiêu cao quá sức học sinh.
 */
@Component
@Order(8)
public class GradeLevelBandScopeInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradeLevelBandScopeInitializer.class);

    private static final String FRAMEWORK_CODE = "KNLNNVN";

    // Phải trùng khít mã bậc mà FrameworkInitializer dựng ra. Sai một ký tự thì initializer này
    // không tìm thấy bậc và bỏ qua toàn bộ -- có log cảnh báo, không âm thầm.
    // BAND_CODE_DEFAULT_TARGET = trần CENTRALIZE, BAND_CODE_HARD_MAX = trần CLASS_TEST -- xem
    // Javadoc lớp.
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

    public GradeLevelBandScopeInitializer(
            GradeLevelRepository gradeLevelRepository,
            GradeLevelBandScopeRepository gradeLevelBandScopeRepository,
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.gradeLevelBandScopeRepository = gradeLevelBandScopeRepository;
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
    }

    /**
     * Idempotent theo TỪNG DÒNG, khác {@link GradeLevelInitializer} vốn chỉ seed khi bảng trống.
     * Khoá duy nhất của bảng là {@code (grade_level_id, framework_version_id)}, nên thêm dòng còn
     * thiếu là thao tác cộng thêm chứ không đè lên cấu hình ai đó đã sửa. Cần vậy vì mỗi phiên bản
     * khung mới ban hành lại cần một bộ trần riêng, và bộ cũ vẫn phải giữ nguyên.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        var framework = frameworkRepository.findByCode(FRAMEWORK_CODE).orElse(null);
        if (framework == null || !framework.isActive()) {
            LOGGER.warn("Chưa có khung {} đang hoạt động. Bỏ qua khởi tạo trần bậc theo khối", FRAMEWORK_CODE);
            return;
        }

        var frameworkVersion = latestPublishedVersion(framework.getId());
        if (frameworkVersion == null) {
            LOGGER.warn("Khung {} chưa có phiên bản nào được ban hành. Bỏ qua khởi tạo trần bậc theo khối",
                FRAMEWORK_CODE);
            return;
        }

        var defaultTargetBand = frameworkResultBandRepository
            .findByVersionIdAndCode(frameworkVersion.getId(), BAND_CODE_DEFAULT_TARGET).orElse(null);
        var hardMaxBand = frameworkResultBandRepository
            .findByVersionIdAndCode(frameworkVersion.getId(), BAND_CODE_HARD_MAX).orElse(null);
        if (defaultTargetBand == null || hardMaxBand == null) {
            LOGGER.warn("Phiên bản khung {} thiếu bậc {} hoặc {}. Bỏ qua khởi tạo trần bậc theo khối",
                frameworkVersion.getCode(), BAND_CODE_DEFAULT_TARGET, BAND_CODE_HARD_MAX);
            return;
        }

        // Bất biến mà DB không diễn đạt được (cần join sang framework_result_bands) và
        // GradeLevelBandScopeGuardService canh lúc chạy. Kiểm luôn ở đây để sai hằng số phía trên
        // làm hỏng lúc khởi động, thay vì sinh ra cấu hình trần ngược đời rồi mới lộ ra khi
        // trường tạo chính sách.
        if (defaultTargetBand.getOrder() > hardMaxBand.getOrder()) {
            throw new IllegalStateException("Bậc mặc định " + BAND_CODE_DEFAULT_TARGET
                + " đang cao hơn bậc trần " + BAND_CODE_HARD_MAX + " trong khung " + FRAMEWORK_CODE + ".");
        }

        var now = Instant.now();
        var created = 0;
        for (var gradeLevelCode : GRADE_LEVEL_CODES) {
            var gradeLevel = gradeLevelRepository.findByCode(gradeLevelCode).orElse(null);
            if (gradeLevel == null) {
                LOGGER.warn("Không tìm thấy khối {}. Bỏ qua trần bậc cho khối này", gradeLevelCode);
                continue;
            }
            if (gradeLevelBandScopeRepository
                    .findByGradeLevelIdAndFrameworkVersionId(gradeLevel.getId(), frameworkVersion.getId())
                    .isPresent()) {
                continue;
            }

            gradeLevelBandScopeRepository.save(new GradeLevelBandScope(
                gradeLevel.getId(),
                frameworkVersion.getId(),
                defaultTargetBand.getId(),
                hardMaxBand.getId(),
                now,
                now,
                null,
                null
            ));
            created++;
        }

        if (created == 0) {
            LOGGER.info("Trần bậc theo khối trên phiên bản khung {} đã đầy đủ. Bỏ qua", frameworkVersion.getCode());
            return;
        }
        LOGGER.info("Đã khởi tạo {} trần bậc theo khối trên phiên bản khung {} (CENTRALIZE {}, CLASS_TEST {})",
            created, frameworkVersion.getCode(), BAND_CODE_DEFAULT_TARGET, BAND_CODE_HARD_MAX);
    }

    private FrameworkVersion latestPublishedVersion(UUID frameworkId) {
        return frameworkVersionRepository
            .findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED).stream()
            .max(Comparator.comparingInt(version -> version.getVersion()))
            .orElse(null);
    }
}
