package com.sep.vox.infrastructure.initializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.model.school.SchoolDirectoryOrigin;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(5)
public class SchoolDirectoryInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoolDirectoryInitializer.class);

    private static final String SEED_PATH = "school/school-directory-seed.json";

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final JsonMapper jsonMapper = new JsonMapper();

    public SchoolDirectoryInitializer(SchoolDirectoryRepository schoolDirectoryRepository) {
        this.schoolDirectoryRepository = schoolDirectoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        var seeds = readSeeds();
        if (seeds.isEmpty()) {
            LOGGER.info("[school-directory] File seed rỗng, bỏ qua khởi tạo danh mục trường");
            return;
        }

        var existingCodes = schoolDirectoryRepository
            .findByCodeIn(seeds.stream().map(seed -> seed.code()).toList())
            .stream()
            .map(directory -> directory.getCode())
            .collect(Collectors.toSet());

        var now = Instant.now();
        var inserted = 0;
        for (var seed : seeds) {
            if (existingCodes.contains(seed.code())) {
                continue;
            }
            schoolDirectoryRepository.save(toDirectory(seed, now));
            inserted++;
        }

        LOGGER.info("[school-directory] Đã nạp {} danh mục trường mới, bỏ qua {} mã đã có",
            inserted, seeds.size() - inserted);
    }

    private List<SeedRow> readSeeds() throws IOException {
        var resource = new ClassPathResource(SEED_PATH);
        var json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return jsonMapper.readValue(json, new TypeReference<List<SeedRow>>() {
        });
    }

    /**
     * createdBy/updatedBy để null: cột cho phép null và lúc initializer chạy thì không có người
     * dùng nào đứng sau hành động này -- gán bừa id của system admin sẽ thành dấu vết sai.
     */
    private SchoolDirectory toDirectory(SeedRow seed, Instant now) {
        return new SchoolDirectory(
            seed.code(),
            seed.name(),
            seed.provinceCode(),
            seed.provinceName(),
            seed.districtName(),
            seed.domain(),
            seed.address(),
            SchoolDirectoryOrigin.OFFICIAL_IMPORT,
            true,
            now,
            now,
            null,
            null
        );
    }

    private record SeedRow(
        String code,
        String name,
        @JsonProperty("province_code")
        String provinceCode,
        @JsonProperty("province_name")
        String provinceName,
        @JsonProperty("district_name")
        String districtName,
        String domain,
        String address
    ) {
    }
}
