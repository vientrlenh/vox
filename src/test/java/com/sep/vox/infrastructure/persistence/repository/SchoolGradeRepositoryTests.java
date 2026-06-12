package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolGradeRepositoryImpl;
import com.sep.vox.infrastructure.persistence.entity.SchoolGradeLevelJpaEntity;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    SchoolGradeRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolGradeRepositoryTests {

    @Autowired
    private SchoolGradeRepository schoolGradeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenFindBySchoolIdAndCodeIn_thenReturnsGradesThroughMatchingGradeLevels() {
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        var level = persistGradeLevel(schoolId, "LEVEL-BATCH-01", 1);
        var otherLevel = persistGradeLevel(otherSchoolId, "LEVEL-BATCH-OTHER", 1);
        schoolGradeRepository.save(newSchoolGrade(level.getId(), "G10", "Grade 10"));
        schoolGradeRepository.save(newSchoolGrade(level.getId(), "G11", "Grade 11"));
        schoolGradeRepository.save(newSchoolGrade(otherLevel.getId(), "G12", "Other Grade 12"));

        var found = schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10", "G11", "G12", "MISSING"));

        assertThat(found)
            .hasSize(2)
            .extracting(SchoolGrade::getCode)
            .containsExactlyInAnyOrder("G10", "G11");
    }

    private SchoolGradeLevelJpaEntity persistGradeLevel(UUID schoolId, String code, int order) {
        var now = OffsetDateTime.now();
        var level = new SchoolGradeLevelJpaEntity(
            null,
            schoolId,
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

    private static SchoolGrade newSchoolGrade(UUID schoolGradeLevelId, String code, String name) {
        var now = OffsetDateTime.now();
        return new SchoolGrade(
            schoolGradeLevelId,
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
