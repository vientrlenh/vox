package com.sep.vox.infrastructure.initializer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.model.gradelevel.GradeLevelStatus;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;

import lombok.extern.slf4j.Slf4j;

@Component
@Order(7)
@Slf4j 
public class GradeLevelInitializer implements ApplicationRunner {

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
        PageResult<GradeLevel> existing = gradeLevelRepository.findAll(null, null, 0, 1);
        if (existing.totalElements() > 0) {
            log.info("Catalog khối lớp đã có {} bản ghi. Bỏ qua khởi tạo khối lớp mặc định",
                existing.totalElements());
            return;
        }

        List<GradeLevel> models = new ArrayList<>();
        for (GradeLevelSeed seed : GRADE_LEVELS) {
            GradeLevel gradeLevel = GradeLevel.builder()
                .code(Code.from(seed.code))
                .name(seed.name)
                .description(seed.description)
                .order(PositiveInteger.from(seed.order))
                .status(GradeLevelStatus.ACTIVE)
                .build();
            models.add(gradeLevel);
        }
        gradeLevelRepository.saveAll(models);
        log.info("Đã khởi tạo {} khối lớp mặc định bậc THPT", GRADE_LEVELS.size());
    }

    private record GradeLevelSeed(String code, String name, String description, int order) {}
}
