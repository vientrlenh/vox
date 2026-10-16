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

import lombok.extern.slf4j.Slf4j;

@Component
@Order(7)
@Slf4j 
public class GradeLevelInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradeLevelInitializer.class);
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
