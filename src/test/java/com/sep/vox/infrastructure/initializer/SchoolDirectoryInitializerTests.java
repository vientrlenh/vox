package com.sep.vox.infrastructure.initializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.school.SchoolDirectoryOrigin;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

/**
 * Cùng lý do với FrameworkInitializerTests: {@code ApplicationRunner} KHÔNG được gọi trong
 * {@code @SpringBootTest}, nên phải gọi thẳng {@code run(null)} trên bean thật với DB thật --
 * ràng buộc unique trên school_directories.code chỉ lộ khi có lượt ghi thật.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class SchoolDirectoryInitializerTests extends ContainerTestConfig {

    /** Số dòng trong school/school-directory-seed.json, sinh từ file Excel nghiệp vụ. */
    private static final int SEED_COUNT = 71;

    private static final int PAGE_SIZE = SEED_COUNT + 50;
    /** findAll của repository đếm trang từ 1, không phải từ 0 -- xem SchoolDirectoryRepositoryImpl. */
    private static final int FIRST_PAGE = 1;
    private static final String SAMPLE_CODE = "PTNK-HCM-1";

    @Autowired
    private SchoolDirectoryInitializer schoolDirectoryInitializer;

    @Autowired
    private SchoolDirectoryRepository schoolDirectoryRepository;

    @Test
    void nap_du_toan_bo_danh_muc_tu_file_seed() throws Exception {
        schoolDirectoryInitializer.run(null);

        assertThat(schoolDirectoryRepository.findAll(FIRST_PAGE, PAGE_SIZE).totalElements())
            .isGreaterThanOrEqualTo(SEED_COUNT);
    }

    /**
     * Cờ verified là thứ quyết định trường đăng ký được bằng OTP theo tên miền hay buộc phải nộp
     * tài liệu (xem RegisterFromSchoolDirectoryUseCase.getVerificationMethod) -- seed sai cờ này
     * là âm thầm đẩy toàn bộ 71 trường sang đường nộp tài liệu.
     */
    @Test
    void seed_ghi_dung_origin_va_verified() throws Exception {
        schoolDirectoryInitializer.run(null);

        var found = schoolDirectoryRepository.findByCodeIn(List.of(SAMPLE_CODE));
        assertThat(found).hasSize(1);

        var directory = found.get(0);
        assertThat(directory.getOrigin()).isEqualTo(SchoolDirectoryOrigin.OFFICIAL_IMPORT);
        assertThat(directory.isVerified()).isTrue();
        assertThat(directory.getName()).isEqualTo("Trường Phổ thông Năng Khiếu, Đại học Quốc gia TP.HCM");
        assertThat(directory.getProvinceName()).isEqualTo("Thành phố Hồ Chí Minh");
        assertThat(directory.getDomain()).isEqualTo("ptnk.edu.vn");
        assertThat(directory.getAddress()).isNotBlank();
    }

    /**
     * Initializer chạy MỖI lần khởi động. Không idempotent thì lần khởi động thứ hai nổ ngay ở
     * ràng buộc unique idx_school_directories_code và cả ứng dụng không lên được.
     */
    @Test
    void chay_lai_khong_tao_ban_ghi_trung() throws Exception {
        schoolDirectoryInitializer.run(null);
        var afterFirstRun = schoolDirectoryRepository.findAll(FIRST_PAGE, PAGE_SIZE).totalElements();

        schoolDirectoryInitializer.run(null);

        assertThat(schoolDirectoryRepository.findAll(FIRST_PAGE, PAGE_SIZE).totalElements())
            .as("lần chạy thứ hai không được chèn thêm gì")
            .isEqualTo(afterFirstRun);
        assertThat(schoolDirectoryRepository.findByCodeIn(List.of(SAMPLE_CODE))).hasSize(1);
    }

    /** Hai dòng trong file seed không có tên miền -- cột nullable, phải vào DB là null. */
    @Test
    void dong_thieu_ten_mien_luu_null() throws Exception {
        schoolDirectoryInitializer.run(null);

        var withoutDomain = schoolDirectoryRepository.findAll(FIRST_PAGE, PAGE_SIZE).content().stream()
            .filter(directory -> directory.getDomain() == null)
            .count();
        assertThat(withoutDomain).isEqualTo(2);
    }

    /** Tên trường trong Excel có dòng thừa khoảng trắng đuôi -- seed phải đã chuẩn hoá sẵn. */
    @Test
    void ten_truong_da_duoc_chuan_hoa_khoang_trang() throws Exception {
        schoolDirectoryInitializer.run(null);

        assertThat(schoolDirectoryRepository.findAll(FIRST_PAGE, PAGE_SIZE).content())
            .allSatisfy(directory -> assertThat(directory.getName()).isEqualTo(directory.getName().strip()));
    }
}
