package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.sep.vox.domain.model.schoolclass.SchoolClassStatus;
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
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var schoolGradeId = UUID.randomUUID();
        var levelVersionId = UUID.randomUUID();
        var schoolClass = newSchoolClass(schoolId, languageId, schoolGradeId, "ENG_10_A", "English 10A", levelVersionId);

        var saved = schoolClassRepository.save(schoolClass);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSchoolId()).isEqualTo(schoolId);
        assertThat(saved.getLanguageId()).isEqualTo(languageId);
        assertThat(saved.getSchoolGradeId()).isEqualTo(schoolGradeId);
        assertThat(saved.getCode().value()).isEqualTo("ENG_10_A");
        assertThat(saved.getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);
    }

    @Test
    void whenFindById_thenReturnsSchoolClass() {
        var saved = schoolClassRepository.save(newSchoolClass(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG_10_B",
            "English 10B",
            UUID.randomUUID()
        ));

        var found = schoolClassRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCode().value()).isEqualTo("ENG_10_B");
    }

    @Test
    void whenFindBySchoolIdAndCode_thenReturnsMatchingSchoolClass() {
        var schoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_10_C", "English 10C", UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG_10_C", "Other School Class", UUID.randomUUID()));

        var found = schoolClassRepository.findBySchoolIdAndCode(schoolId, "ENG_10_C");

        assertThat(found).isPresent();
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
        assertThat(found.get().getName()).isEqualTo("English 10C");
    }

    @Test
    void whenFindBySchoolIdAndName_thenReturnsOnlyClassesInSchoolWithName() {
        var schoolId = UUID.randomUUID();
        var className = "English Shared";
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_11_A", className, UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_11_B", className, UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG_11_C", className, UUID.randomUUID()));

        var found = schoolClassRepository.findBySchoolIdAndName(schoolId, className);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactlyInAnyOrder("ENG_11_A", "ENG_11_B");
    }

    @Test
    void whenFindBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId_thenReturnsMatchingClasses() {
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var levelVersionId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, UUID.randomUUID(), "ENG_12_A", "English 12A", levelVersionId));
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, UUID.randomUUID(), "ENG_12_B", "English 12B", levelVersionId));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_12_C", "Other Language", levelVersionId));
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, UUID.randomUUID(), "ENG_12_D", "Other Level", UUID.randomUUID()));

        var found = schoolClassRepository.findBySchoolIdAndLanguageIdAndTargetSchoolLevelVersionId(
            schoolId,
            languageId,
            levelVersionId
        );

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactlyInAnyOrder("ENG_12_A", "ENG_12_B");
    }

    private static SchoolClass newSchoolClass(UUID schoolId, UUID languageId, UUID schoolGradeId, String code,
            String name, UUID targetSchoolLevelVersionId) {
        var now = OffsetDateTime.now();
        return new SchoolClass(
            schoolId,
            languageId,
            schoolGradeId,
            new ClassCode(code),
            name,
            "Repository test class",
            targetSchoolLevelVersionId,
            SchoolClassStatus.ACTIVE,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
