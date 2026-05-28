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
import com.sep.vox.domain.model.schoolclass.SchoolClass;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.infrastructure.persistence.adapter.SchoolClassRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    SchoolClassRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolClassRepositoryTests {

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Test
    void whenSave_thenReturnsPersistedSchoolClass() {
        var schoolClass = newSchoolClass(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG_A1_01", "English A1");

        var saved = schoolClassRepository.save(schoolClass);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSchoolId()).isEqualTo(schoolClass.getSchoolId());
        assertThat(saved.getLanguageId()).isEqualTo(schoolClass.getLanguageId());
        assertThat(saved.getLevelId()).isEqualTo(schoolClass.getLevelId());
        assertThat(saved.getCode().value()).isEqualTo("ENG_A1_01");
        assertThat(saved.getName()).isEqualTo("English A1");
    }

    @Test
    void whenFindById_thenReturnsSchoolClass() {
        var saved = schoolClassRepository.save(
            newSchoolClass(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG_A1_02", "English A1")
        );

        var found = schoolClassRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCode().value()).isEqualTo("ENG_A1_02");
    }

    @Test
    void whenFindBySchoolIdAndCode_thenReturnsMatchingSchoolClass() {
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var levelId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, levelId, "ENG_A2_01", "English A2"));
        schoolClassRepository.save(newSchoolClass(UUID.randomUUID(), languageId, levelId, "ENG_A2_01", "English A2"));

        var found = schoolClassRepository.findBySchoolIdAndCode(schoolId, "ENG_A2_01");

        assertThat(found).isPresent();
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
        assertThat(found.get().getCode().value()).isEqualTo("ENG_A2_01");
    }

    @Test
    void whenFindBySchoolIdAndName_thenReturnsOnlyClassesForThatSchoolAndName() {
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var levelId = UUID.randomUUID();
        var classCode = "ENG_B1_01";
        var anotherClassCode = "ENG_B1_02";
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, levelId, classCode, "English B1"));
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, levelId, anotherClassCode, "English B1"));
        schoolClassRepository.save(newSchoolClass(UUID.randomUUID(), languageId, levelId, "ENG_B1_03", "English B1"));

        var found = schoolClassRepository.findBySchoolIdAndName(schoolId, "English B1");

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactlyInAnyOrder(classCode, anotherClassCode);
    }

    @Test
    void whenFindBySchoolIdAndLanguageIdAndLevelId_thenReturnsMatchingClasses() {
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var levelId = UUID.randomUUID();
        var classCode = "ENG_C1_01";
        var anotherClassCode = "ENG_C1_02";
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, levelId, classCode, "English C1 Morning"));
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, levelId, anotherClassCode, "English C1 Evening"));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), levelId, "FRA_C1_01", "French C1"));

        var found = schoolClassRepository.findBySchoolIdAndLanguageIdAndLevelId(schoolId, languageId, levelId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactlyInAnyOrder(classCode, anotherClassCode);
    }

    private static SchoolClass newSchoolClass(UUID schoolId, UUID languageId, UUID levelId, String code, String name) {
        var now = OffsetDateTime.now();
        return new SchoolClass(
            schoolId,
            languageId,
            new ClassCode(code),
            name,
            "Repository test class",
            levelId,
            LocalDate.now(),
            LocalDate.now().plusMonths(3),
            true,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
