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

    private static SchoolClass newSchoolClass(UUID schoolId, String code, String name) {
        var now = OffsetDateTime.now();
        var userId = UUID.randomUUID();
        return new SchoolClass(
            schoolId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            new ClassCode(code),
            name,
            "Repository test class",
            SchoolClassStatus.ACTIVE,
            now,
            now,
            userId,
            userId
        );
    }
}
