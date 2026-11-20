package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolGradeRepositoryImpl;
import com.sep.vox.infrastructure.persistence.entity.GradeLevelJpaEntity;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    SchoolGradeRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolGradeRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SchoolGradeRepository schoolGradeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenFindBySchoolIdAndCodeIn_thenReturnsOnlyGradesOfThatSchool() {
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        // Khối lớp giờ dùng chung: hai trường cùng trỏ vào một grade level, phân biệt bằng schoolId.
        var level = persistGradeLevel("LEVEL-BATCH-01", 1);
        schoolGradeRepository.save(newSchoolGrade(schoolId, level.getId(), "G10", "Grade 10"));
        schoolGradeRepository.save(newSchoolGrade(schoolId, level.getId(), "G11", "Grade 11"));
        schoolGradeRepository.save(newSchoolGrade(otherSchoolId, level.getId(), "G12", "Other Grade 12"));

        var found = schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10", "G11", "G12", "MISSING"));

        assertThat(found)
            .hasSize(2)
            .extracting(grade -> grade.getCode())
            .containsExactlyInAnyOrder("G10", "G11");
    }

    private GradeLevelJpaEntity persistGradeLevel(String code, int order) {
        var now = Instant.now();
        var level = new GradeLevelJpaEntity(
            null,
            code,
            code,
            "Repository test grade level",
            order,
            "ACTIVE",
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        entityManager.persist(level);
        entityManager.flush();
        entityManager.refresh(level);
        return level;
    }

    private static SchoolGrade newSchoolGrade(UUID schoolId, UUID gradeLevelId, String code, String name) {
        var now = Instant.now();
        return new SchoolGrade(
            schoolId,
            gradeLevelId,
            code,
            name,
            "Repository test grade",
            LocalDate.now(),
            LocalDate.now().plusMonths(6),
            SchoolGradeStatus.ACTIVE,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
