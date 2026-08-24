package com.sep.vox.infrastructure.initializer;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;

/**
 * Dựng sẵn catalog khối lớp THPT (Khối 10, 11, 12) dùng chung toàn hệ thống.
 *
 * <p>Đoạn seed này trước nằm ở bước 8 của {@code V41__grade_level_global_catalog.sql}. Chuyển ra
 * đây vì ba lý do:
 *
 * <ul>
 *   <li>Migration KHÔNG chạy trong test: {@code src/test/resources/application-test.yaml} đặt
 *       {@code ddl-auto: create-drop}, nên Flyway bị bỏ qua hoàn toàn khi chạy {@code ./gradlew
 *       test}. Dữ liệu seed nằm trong file .sql là dữ liệu không test nào nhìn thấy.</li>
 *   <li>Migration chỉ chạy đúng một lần. Sửa nội dung seed sau khi V41 đã áp lên một môi trường
 *       nào đó là đổi checksum -- Flyway sẽ từ chối khởi động. Initializer chạy lại mỗi lần boot
 *       nên sửa được.</li>
 *   <li>Ba khối này là dữ liệu nghiệp vụ chứ không phải cấu trúc bảng. Để cạnh
 *       {@link FrameworkInitializer} và {@link SystemRubricTemplateInitializer} thì tìm được bằng
 *       cách đọc code Java, thay vì phải nhớ là nó nằm lẫn trong một file migration.</li>
 * </ul>
 *
 * <h2>Chỉ seed khi catalog HOÀN TOÀN trống</h2>
 *
 * <p>Giữ nguyên ngữ nghĩa của câu {@code where not exists (select 1 from grade_levels)} ở V41, chứ
 * không kiểm theo từng mã. Hai lý do:
 *
 * <ul>
 *   <li>{@code grade_level_order} unique toàn cục ({@code idx_grade_levels_order}). Chèn GRADE_10 ở
 *       thứ tự 1 vào một catalog đã có khối khác chiếm thứ tự 1 sẽ nổ ràng buộc ngay lúc khởi
 *       động.</li>
 *   <li>Một hệ đã chạy thật có catalog do V41 gộp lên từ {@code school_grade_levels} của các
 *       trường. Chồng thêm bộ THPT mặc định lên trên đó là thêm khối trùng nghĩa mà không ai
 *       khai.</li>
 * </ul>
 *
 * <p>Nghĩa là: xóa hết khối đi rồi khởi động lại thì bộ mặc định quay về; xóa đúng một khối thì
 * không. Chủ ý -- xóa một khối là một quyết định, không phải sự cố cần initializer chữa.
 */
@Component
@Order(7)
public class GradeLevelInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradeLevelInitializer.class);

    /**
     * Mã khối là khoá đối chiếu đi ra ngoài: {@link GradeLevelBandScopeInitializer} và
     * {@link SystemAssessmentPolicyInitializer} đều tra theo đúng ba chuỗi này, và luồng import
     * Excel chuẩn hoá bằng {@code toUpperCase} trước khi so. Đổi mã ở đây là đổi ở cả ba chỗ.
     */
    static final String GRADE_10 = "GRADE_10";
    static final String GRADE_11 = "GRADE_11";
    static final String GRADE_12 = "GRADE_12";

    private static final List<GradeLevelSeed> GRADE_LEVELS = List.of(
        new GradeLevelSeed(GRADE_10, "Khối 10", "Khối lớp 10 bậc trung học phổ thông", 1),
        new GradeLevelSeed(GRADE_11, "Khối 11", "Khối lớp 11 bậc trung học phổ thông", 2),
        new GradeLevelSeed(GRADE_12, "Khối 12", "Khối lớp 12 bậc trung học phổ thông", 3)
    );

    private final GradeLevelRepository gradeLevelRepository;

    public GradeLevelInitializer(GradeLevelRepository gradeLevelRepository) {
        this.gradeLevelRepository = gradeLevelRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // findAll nhận page theo lối 1-based (GradeLevelRepositoryImpl gọi PageRequest.of(page - 1,
        // size)), nên trang đầu là 1 chứ không phải 0. Lấy size 1 vì chỉ cần totalElements.
        var existing = gradeLevelRepository.findAll(null, null, 1, 1);
        if (existing.totalElements() > 0) {
            LOGGER.info("Catalog khối lớp đã có {} bản ghi. Bỏ qua khởi tạo khối lớp mặc định",
                existing.totalElements());
            return;
        }

        var now = Instant.now();
        for (var seed : GRADE_LEVELS) {
            gradeLevelRepository.save(new GradeLevel(
                seed.code(),
                seed.name(),
                seed.description(),
                seed.order(),
                GradeLevelStatus.ACTIVE,
                now,
                now,
                // Lúc khởi động chưa có phiên đăng nhập nào nên không có danh tính để ghi vào
                // created_by/updated_by. Null ở đây đọc đúng nghĩa: do hệ thống dựng.
                null,
                null
            ));
        }

        LOGGER.info("Đã khởi tạo {} khối lớp mặc định bậc THPT", GRADE_LEVELS.size());
    }

    private record GradeLevelSeed(String code, String name, String description, int order) {}
}
