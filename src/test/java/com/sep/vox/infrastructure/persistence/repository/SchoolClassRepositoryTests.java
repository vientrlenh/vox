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
        var schoolClass = newSchoolClass(UUID.randomUUID(), "ENG-01", "English 01");

        var saved = schoolClassRepository.save(schoolClass);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode().value()).isEqualTo("ENG-01");
        assertThat(saved.getName()).isEqualTo("English 01");
        assertThat(saved.getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);
    }

    @Test
    void whenFindBySchoolIdAndCode_thenReturnsOnlyMatchingClass() {
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, "ENG-01", "English 01"));
        schoolClassRepository.save(newSchoolClass(otherSchoolId, "ENG-01", "Other English 01"));

        var found = schoolClassRepository.findBySchoolIdAndCode(schoolId, "ENG-01");

        assertThat(found).isPresent();
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
        assertThat(found.get().getName()).isEqualTo("English 01");
        assertThat(schoolClassRepository.findBySchoolIdAndCode(schoolId, "MISSING")).isEmpty();
    }

    @Test
    void whenFindBySchoolIdWithFilters_thenReturnsMatchingPagedClasses() {
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        var languageId = UUID.randomUUID();
        var gradeId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, gradeId, "ENG-01", "English 01", SchoolClassStatus.ACTIVE));
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, UUID.randomUUID(), "MATH-01", "Mathematics 01", SchoolClassStatus.ACTIVE));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), gradeId, "ENG-ARCHIVE", "Archived English", SchoolClassStatus.ARCHIVED));
        schoolClassRepository.save(newSchoolClass(otherSchoolId, languageId, gradeId, "ENG-01", "Other English 01", SchoolClassStatus.ACTIVE));

        var found = schoolClassRepository.findBySchoolId(
            schoolId,
            "eng",
            SchoolClassStatus.ACTIVE,
            languageId,
            gradeId,
            new PageRequest(1, 10)
        );

        assertThat(found.content()).hasSize(1);
        assertThat(found.content().get(0).getSchoolId()).isEqualTo(schoolId);
        assertThat(found.content().get(0).getCode().value()).isEqualTo("ENG-01");
        assertThat(found.totalElements()).isEqualTo(1);
        assertThat(found.totalPages()).isEqualTo(1);
    }

    @Test
    void whenFindBySchoolIdWithSearchByName_thenMatchesCaseInsensitively() {
        var schoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, "ENG-02", "Advanced English"));

        var found = schoolClassRepository.findBySchoolId(
            schoolId,
            "advanced",
            null,
            null,
            null,
            new PageRequest(1, 10)
        );

        assertThat(found.content()).hasSize(1);
        assertThat(found.content().get(0).getName()).isEqualTo("Advanced English");
    }

    private static SchoolClass newSchoolClass(UUID schoolId, String code, String name) {
        return newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), code, name, SchoolClassStatus.ACTIVE);
    }

    private static SchoolClass newSchoolClass(UUID schoolId, UUID languageId, UUID gradeId, String code, String name,
            SchoolClassStatus status) {
        var now = OffsetDateTime.now();
        var userId = UUID.randomUUID();
        return new SchoolClass(
            schoolId,
            languageId,
            gradeId,
            new ClassCode(code),
            name,
            "Repository test class",
            status,
            now,
            now,
            userId,
            userId
        );
    }
}
