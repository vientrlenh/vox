package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.model.languagelevel.LevelStatus;
import com.sep.vox.domain.model.languagelevel.SchoolLevel;
import com.sep.vox.domain.model.languagelevel.SchoolLevelVersion;
import com.sep.vox.domain.model.schoolgrade.SchoolGrade;
import com.sep.vox.domain.model.schoolgrade.SchoolGradeStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolLevelRepository;
import com.sep.vox.domain.repository.SchoolLevelVersionRepository;
import com.sep.vox.domain.valueobject.LevelCode;
import com.sep.vox.domain.valueobject.LevelOrder;
import com.sep.vox.domain.valueobject.LevelVersion;
import com.sep.vox.infrastructure.persistence.adapter.SchoolGradeRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.SchoolLevelRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.SchoolLevelVersionRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    SchoolGradeRepositoryImpl.class,
    SchoolLevelRepositoryImpl.class,
    SchoolLevelVersionRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolClassImportLookupRepositoryTests {

    @Autowired
    private SchoolGradeRepository schoolGradeRepository;

    @Autowired
    private SchoolLevelRepository schoolLevelRepository;

    @Autowired
    private SchoolLevelVersionRepository schoolLevelVersionRepository;

    @Test
    void school_grade_repository_should_find_by_school_id_and_code() {
        var schoolId = UUID.randomUUID();
        schoolGradeRepository.save(grade(schoolId, "G10"));
        schoolGradeRepository.save(grade(UUID.randomUUID(), "G10"));

        var found = schoolGradeRepository.findBySchoolIdAndCode(schoolId, "G10");

        assertThat(found).isPresent();
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
        assertThat(found.get().getCode()).isEqualTo("G10");
    }

    @Test
    void school_level_repository_should_find_by_school_language_and_code() {
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        schoolLevelRepository.save(level(schoolId, languageId, "A1"));
        schoolLevelRepository.save(level(schoolId, UUID.randomUUID(), "A1"));

        var found = schoolLevelRepository.findBySchoolIdAndLanguageIdAndCode(schoolId, languageId, "A1");

        assertThat(found).isPresent();
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
        assertThat(found.get().getLanguageId()).isEqualTo(languageId);
        assertThat(found.get().getCode().value()).isEqualTo("A1");
    }

    @Test
    void school_level_version_repository_should_find_by_school_level_id_and_version() {
        var level = schoolLevelRepository.save(level(UUID.randomUUID(), UUID.randomUUID(), "B1"));
        schoolLevelVersionRepository.save(levelVersion(level.getId(), 1));
        schoolLevelVersionRepository.save(levelVersion(level.getId(), 2));

        var found = schoolLevelVersionRepository.findBySchoolLevelIdAndVersion(level.getId(), 2);

        assertThat(found).isPresent();
        assertThat(found.get().getSchoolLevelId()).isEqualTo(level.getId());
        assertThat(found.get().getVersion().value()).isEqualTo(2);
    }

    private static SchoolGrade grade(UUID schoolId, String code) {
        var now = OffsetDateTime.now();
        return new SchoolGrade(
            schoolId,
            code,
            "Grade " + code,
            "Import lookup test",
            LocalDate.now(),
            LocalDate.now().plusMonths(9),
            SchoolGradeStatus.ACTIVE,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private static SchoolLevel level(UUID schoolId, UUID languageId, String code) {
        var now = OffsetDateTime.now();
        return new SchoolLevel(
            schoolId,
            languageId,
            UUID.randomUUID(),
            new LevelCode(code),
            null,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }

    private static SchoolLevelVersion levelVersion(UUID schoolLevelId, int version) {
        var now = OffsetDateTime.now();
        return new SchoolLevelVersion(
            schoolLevelId,
            new LevelVersion(version),
            "Version " + version,
            "Import lookup test",
            null,
            null,
            null,
            null,
            LevelStatus.DRAFT,
            new LevelOrder(version),
            null,
            null,
            now,
            null,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
