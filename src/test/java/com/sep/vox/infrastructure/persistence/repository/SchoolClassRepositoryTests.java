package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
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
    void whenFindBySchoolIdAndCodeIn_thenReturnsMatchingSchoolClasses() {
        var schoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_10_D", "English 10D", UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_10_E", "English 10E", UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_10_F", "English 10F", UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG_10_D", "Other School", UUID.randomUUID()));

        var found = schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, List.of("ENG_10_D", "ENG_10_E"));

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactlyInAnyOrder("ENG_10_D", "ENG_10_E");
    }

    @Test
    void whenFindBySchoolId_thenReturnsPagedClassesOnlyInSchool() {
        var schoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_10_G", "English 10G", UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_10_H", "English 10H", UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG_10_I", "English 10I", UUID.randomUUID()));
        schoolClassRepository.save(newSchoolClass(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ENG_10_J", "Other School", UUID.randomUUID()));

        var page = schoolClassRepository.findBySchoolId(schoolId, new PageRequest(1, 2));

        assertThat(page.content()).hasSize(2);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content())
            .extracting(SchoolClass::getSchoolId)
            .containsOnly(schoolId);
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

    @Test
    void whenUpdateMutableFieldsWithMatchingConditions_thenUpdatesMutableFieldsOnly() {
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        var originalLevelVersionId = UUID.randomUUID();
        var newLevelVersionId = UUID.randomUUID();
        var saved = schoolClassRepository.save(newSchoolClass(
            schoolId,
            languageId,
            gradeId,
            "ENG_12_UPDATE",
            "English 12 Update",
            originalLevelVersionId
        ));
        var persistedBeforeUpdate = schoolClassRepository.findById(saved.getId()).orElseThrow();
        var originalCreatedAt = persistedBeforeUpdate.getCreatedAt();
        var originalCreatedBy = persistedBeforeUpdate.getCreatedBy();
        var updatedAt = OffsetDateTime.now().plusMinutes(1).withNano(0);
        var updatedBy = UUID.randomUUID();

        var updatedRows = schoolClassRepository.updateMutableFields(
            saved.getId(),
            schoolId,
            languageId,
            "English 12 Updated",
            "Updated repository description",
            newLevelVersionId,
            SchoolClassStatus.INACTIVE,
            updatedAt,
            updatedBy
        );

        assertThat(updatedRows).isEqualTo(1);
        var found = schoolClassRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("English 12 Updated");
        assertThat(found.getDescription()).isEqualTo("Updated repository description");
        assertThat(found.getTargetSchoolLevelVersionId()).isEqualTo(newLevelVersionId);
        assertThat(found.getStatus()).isEqualTo(SchoolClassStatus.INACTIVE);
        assertThat(found.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(found.getUpdatedBy()).isEqualTo(updatedBy);
        assertThat(found.getSchoolId()).isEqualTo(schoolId);
        assertThat(found.getLanguageId()).isEqualTo(languageId);
        assertThat(found.getSchoolGradeId()).isEqualTo(gradeId);
        assertThat(found.getCode().value()).isEqualTo("ENG_12_UPDATE");
        assertThat(found.getCreatedAt().toInstant().getEpochSecond()).isEqualTo(originalCreatedAt.toInstant().getEpochSecond());
        assertThat(found.getCreatedBy()).isEqualTo(originalCreatedBy);
    }

    @Test
    void whenUpdateMutableFieldsConditionsDoNotMatch_thenDoesNotUpdate() {
        var schoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var originalLevelVersionId = UUID.randomUUID();
        var saved = schoolClassRepository.save(newSchoolClass(
            schoolId,
            languageId,
            UUID.randomUUID(),
            "ENG_12_NO_UPDATE",
            "English 12 No Update",
            originalLevelVersionId
        ));

        var updatedRows = schoolClassRepository.updateMutableFields(
            saved.getId(),
            UUID.randomUUID(),
            languageId,
            "Should Not Update",
            "Should not update description",
            UUID.randomUUID(),
            SchoolClassStatus.ARCHIVED,
            OffsetDateTime.now().plusMinutes(1),
            UUID.randomUUID()
        );

        assertThat(updatedRows).isEqualTo(0);
        var found = schoolClassRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("English 12 No Update");
        assertThat(found.getDescription()).isEqualTo("Repository test class");
        assertThat(found.getTargetSchoolLevelVersionId()).isEqualTo(originalLevelVersionId);
        assertThat(found.getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);

        var wrongLanguageRows = schoolClassRepository.updateMutableFields(
            saved.getId(),
            schoolId,
            UUID.randomUUID(),
            "Should Not Update Either",
            "Should not update either",
            UUID.randomUUID(),
            SchoolClassStatus.INACTIVE,
            OffsetDateTime.now().plusMinutes(2),
            UUID.randomUUID()
        );

        assertThat(wrongLanguageRows).isEqualTo(0);
        var afterWrongLanguage = schoolClassRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterWrongLanguage.getName()).isEqualTo("English 12 No Update");
        assertThat(afterWrongLanguage.getDescription()).isEqualTo("Repository test class");
        assertThat(afterWrongLanguage.getTargetSchoolLevelVersionId()).isEqualTo(originalLevelVersionId);
        assertThat(afterWrongLanguage.getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);
    }

    @Test
    void whenDeleteById_thenRemovesSchoolClass() {
        var saved = schoolClassRepository.save(newSchoolClass(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG_12_E",
            "English 12E",
            UUID.randomUUID()
        ));

        schoolClassRepository.deleteById(saved.getId());

        assertThat(schoolClassRepository.findById(saved.getId())).isEmpty();
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
