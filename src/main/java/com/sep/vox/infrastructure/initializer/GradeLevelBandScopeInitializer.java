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
            log.warn("Chưa có khung {} đang hoạt động. Bỏ qua khởi tạo trần bậc theo khối", FRAMEWORK_CODE);
            return;
        }

        FrameworkVersion frameworkVersion = latestPublishedVersion(framework.getId());
        if (frameworkVersion == null) {
            log.warn("Khung {} chưa có phiên bản nào được ban hành. Bỏ qua khởi tạo trần bậc theo khối",
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
            log.warn("Phiên bản khung {} thiếu bậc {} hoặc {}. Bỏ qua khởi tạo trần bậc theo khối",
                frameworkVersion.getCode(), BAND_CODE_DEFAULT_TARGET, BAND_CODE_HARD_MAX);
            return;
        }

        requireDefaultBandNotAboveHardMax(defaultTargetBand, hardMaxBand);

        int created = createMissingScopes(frameworkVersion, defaultTargetBand, hardMaxBand);

        if (created == 0) {
            log.info("Trần bậc theo khối trên phiên bản khung {} đã đầy đủ. Bỏ qua", frameworkVersion.getCode());
            return;
        }
        log.info("Đã khởi tạo {} trần bậc theo khối trên phiên bản khung {} (CENTRALIZE {}, CLASS_TEST {})",
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
            throw new IllegalStateException("Bậc mặc định " + BAND_CODE_DEFAULT_TARGET
                + " đang cao hơn bậc trần " + BAND_CODE_HARD_MAX + " trong khung " + FRAMEWORK_CODE + ".");
        }
    }

    private int createMissingScopes(FrameworkVersion frameworkVersion, FrameworkResultBand defaultTargetBand,
            FrameworkResultBand hardMaxBand) {
        int created = 0;
        for (String gradeLevelCode : GRADE_LEVEL_CODES) {
            GradeLevel gradeLevel = gradeLevelRepository.findByCode(gradeLevelCode).orElse(null);
            if (gradeLevel == null) {
                log.warn("Không tìm thấy khối {}. Bỏ qua trần bậc cho khối này", gradeLevelCode);
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
