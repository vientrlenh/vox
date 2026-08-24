package com.sep.vox.infrastructure.initializer;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
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
 * <h2>Vì sao mặc định giống nhau nhưng trần lại khác nhau giữa các khối</h2>
 *
 * <p>Chương trình GDPT 2018 môn Tiếng Anh (Thông tư 32/2018/TT-BGDĐT) chốt chuẩn đầu ra theo CẤP
 * HỌC chứ không theo từng lớp: hết tiểu học đạt Bậc 1, hết THCS đạt Bậc 2, hết THPT đạt Bậc 3. Cả
 * Khối 10, 11 lẫn 12 vì thế cùng nhắm Bậc 3 làm MẶC ĐỊNH -- không có văn bản nào phân biệt ba khối
 * này khác nhau (xem thêm ghi chú cũ về 3.1/3.2/3.3 không tách được vì khung chỉ có 6 bậc nguyên).
 *
 * <p>Nhưng TRẦN (bậc cao nhất một Lớp cụ thể được phép chọn, vd. lớp chuyên Anh) chỉ nới lên Bậc 4
 * cho Khối 12 -- Khối 10 và 11 giữ trần bằng đúng mặc định, tức KHÔNG có khoảng nới nào cả dù phạm
 * vi hẹp tới đâu. Đây là quyết định nghiệp vụ của dự án (không có Thông tư nào quy định phân biệt
 * trần theo khối như vậy), xuất phát từ việc lớp chuyên thường chỉ thực sự vượt chuẩn Bậc 3 rõ rệt
 * vào năm cuối cấp.
 *
 * <p>Cấu hình khác nhau giữa các khối KHÔNG có nghĩa map trần dưới đây là thừa việc: phần làm việc
 * thật là TRẦN CỨNG theo từng khối, và nó dễ mở rộng thêm khi catalog khối có thêm cấp THCS
 * (Khối 6-9 nhắm Bậc 2, trần cũng theo từng khối tương tự).
 *
 * <h2>Vì sao trần của Khối 12 là Bậc 4 chứ không phải Bậc 3</h2>
 *
 * <p>Siết đúng Bậc 3 sẽ chặn mọi lớp chuyên Anh, vốn thường lấy B2 làm chuẩn thực tế. Lưu ý là
 * chuẩn đó KHÔNG nằm trong quy chế trường chuyên (Thông tư 05/2023/TT-BGDĐT không quy định chuẩn
 * đầu ra ngoại ngữ nào cho lớp chuyên, dù nhiều nguồn thứ cấp nói vậy) -- nó là thông lệ của
 * trường, nên để trường tự chọn trong khoảng cho phép là đúng.
 *
 * <p>Điều cần chặn là Bậc 5 và Bậc 6. Trong giáo dục phổ thông không có văn bản nào đặt C1/C2 làm
 * đích cho HỌC SINH; chỗ C1 xuất hiện chính thức là chuẩn của GIÁO VIÊN tiếng Anh THPT theo Đề án
 * Ngoại ngữ Quốc gia. Nhầm hai chuẩn đó với nhau nhiều khả năng chính là nguồn gốc của việc trường
 * đặt bậc mục tiêu cao quá sức học sinh.
 */
@Component
@Order(8)
public class GradeLevelBandScopeInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradeLevelBandScopeInitializer.class);

    private static final String FRAMEWORK_CODE = "KNLNNVN";

    // Phải trùng khít mã bậc mà FrameworkInitializer dựng ra. Sai một ký tự thì initializer này
    // không tìm thấy bậc và bỏ qua toàn bộ -- có log cảnh báo, không âm thầm.
    private static final String BAND_CODE_DEFAULT_TARGET = "BAC_3";
    private static final String BAND_CODE_HARD_MAX = "BAC_4";

    // Trần theo TỪNG khối, không còn dùng chung 1 hằng số -- chỉ Khối 12 được nới lên
    // BAND_CODE_HARD_MAX (Bậc 4) cho lớp chuyên; Khối 10/11 trần bằng đúng BAND_CODE_DEFAULT_TARGET
    // (Bậc 3), tức không có khoảng nới nào dù phạm vi hẹp tới đâu. Xem Javadoc lớp.
    private static final Map<String, String> HARD_MAX_BAND_CODE_BY_GRADE_LEVEL = Map.of(
        GradeLevelInitializer.GRADE_10, BAND_CODE_DEFAULT_TARGET,
        GradeLevelInitializer.GRADE_11, BAND_CODE_DEFAULT_TARGET,
        GradeLevelInitializer.GRADE_12, BAND_CODE_HARD_MAX
    );

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
        if (defaultTargetBand == null) {
            LOGGER.warn("Phiên bản khung {} thiếu bậc {}. Bỏ qua khởi tạo trần bậc theo khối",
                frameworkVersion.getCode(), BAND_CODE_DEFAULT_TARGET);
            return;
        }

        // Trần khác nhau theo khối (xem HARD_MAX_BAND_CODE_BY_GRADE_LEVEL) nên fetch trước tất cả
        // band code trần khác nhau đang dùng, cache lại để không query lặp cho các khối cùng trần.
        Map<String, FrameworkResultBand> hardMaxBandByCode = new HashMap<>();
        for (var hardMaxBandCode : HARD_MAX_BAND_CODE_BY_GRADE_LEVEL.values()) {
            if (hardMaxBandByCode.containsKey(hardMaxBandCode)) {
                continue;
            }
            var hardMaxBand = frameworkResultBandRepository
                .findByVersionIdAndCode(frameworkVersion.getId(), hardMaxBandCode).orElse(null);
            if (hardMaxBand == null) {
                LOGGER.warn("Phiên bản khung {} thiếu bậc trần {}. Bỏ qua khởi tạo trần bậc theo khối",
                    frameworkVersion.getCode(), hardMaxBandCode);
                return;
            }
            // Bất biến mà DB không diễn đạt được (cần join sang framework_result_bands) và
            // GradeLevelBandScopeGuardService canh lúc chạy. Kiểm luôn ở đây để sai hằng số phía
            // trên làm hỏng lúc khởi động, thay vì sinh ra cấu hình trần ngược đời rồi mới lộ ra
            // khi trường tạo chính sách.
            if (defaultTargetBand.getOrder() > hardMaxBand.getOrder()) {
                throw new IllegalStateException("Bậc mặc định " + BAND_CODE_DEFAULT_TARGET
                    + " đang cao hơn bậc trần " + hardMaxBandCode + " trong khung " + FRAMEWORK_CODE + ".");
            }
            hardMaxBandByCode.put(hardMaxBandCode, hardMaxBand);
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

            var hardMaxBand = hardMaxBandByCode.get(HARD_MAX_BAND_CODE_BY_GRADE_LEVEL.get(gradeLevelCode));

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
        LOGGER.info("Đã khởi tạo {} trần bậc theo khối trên phiên bản khung {} (mặc định {}, trần theo khối: {})",
            created, frameworkVersion.getCode(), BAND_CODE_DEFAULT_TARGET, HARD_MAX_BAND_CODE_BY_GRADE_LEVEL);
    }

    private FrameworkVersion latestPublishedVersion(UUID frameworkId) {
        return frameworkVersionRepository
            .findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED).stream()
            .max(Comparator.comparingInt(version -> version.getVersion()))
            .orElse(null);
    }
}
