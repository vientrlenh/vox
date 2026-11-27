package com.sep.vox.infrastructure.initializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Preloads the directory of high schools from school/school-directory-seed.json (generated from the Excel file the
 * business provided) so the system has the directory ready for the registration flow, instead of importing by hand through
 * PreviewSchoolDirectoryImportFromFileUseCase every time a new environment is built.
 *
 * <p>Written with origin OFFICIAL_IMPORT and verified = true: this is the official list loaded up front
 * (not typed row by row by an admin), and already trusted, so a school with an email on the registered domain
 * can pass via OTP instead of submitting documents -- see RegisterFromSchoolDirectoryUseCase.
 *
 * <p>Idempotent by SCHOOL CODE and not by "is the table empty": it only inserts the codes that
 * do not exist yet. So adding a new school to the seed file and restarting loads the missing part, while
 * NOT touching entries an admin edited by hand or verified earlier.
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class SchoolDirectoryInitializer implements ApplicationRunner {

    private static final String SEED_PATH = "school/school-directory-seed.json";

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final JsonMapper jsonMapper = new JsonMapper();

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        List<SeedRow> seeds = readSeeds();
        if (seeds.isEmpty()) {
            log.info("[school-directory] The seed file is empty, skipping school directory initialization");
            return;
        }

        Set<String> existingCodes = schoolDirectoryRepository
            .findByCodeIn(seeds.stream().map(seed -> seed.code()).toList())
            .stream()
            .map(directory -> directory.getCode())
            .collect(Collectors.toSet());

        int inserted = 0;
        for (SeedRow seed : seeds) {
            if (existingCodes.contains(seed.code())) {
                continue;
            }
            schoolDirectoryRepository.save(toDirectory(seed));
            inserted++;
        }

        log.info("[school-directory] Loaded {} new school directory entries, skipped {} existing codes",
            inserted, seeds.size() - inserted);
    }

    private List<SeedRow> readSeeds() throws IOException {
        ClassPathResource resource = new ClassPathResource(SEED_PATH);
        String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return jsonMapper.readValue(json, new TypeReference<List<SeedRow>>() {
        });
    }

    /**
     * The audit actor is left to the auditing hook: no user stands behind this action while the initializer runs,
     * so it records "System" -- assigning the id of some system admin would leave a false trail.
     */
    private SchoolDirectory toDirectory(SeedRow seed) {
        return SchoolDirectory.builder()
            .code(seed.code())
            .name(seed.name())
            .provinceCode(seed.provinceCode())
            .provinceName(seed.provinceName())
            .districtName(seed.districtName())
            .domain(seed.domain())
            .address(seed.address())
            .origin(SchoolDirectoryOrigin.OFFICIAL_IMPORT)
            .verified(true)
            .build();
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
