package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolGradeRepositoryImpl;
import com.sep.vox.infrastructure.persistence.entity.GradeLevelJpaEntity;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

@RepositoryTest
@Import({
    SchoolGradeRepositoryImpl.class
})
class SchoolGradeRepositoryTests {

    @Autowired
    private SchoolGradeRepository schoolGradeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenFindBySchoolIdAndCodeIn_thenReturnsOnlyGradesOfThatSchool() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        // Grade levels are now shared: two schools point at the same grade level, distinguished by
        // schoolId.
        GradeLevelJpaEntity level = persistGradeLevel("LEVEL-BATCH-01", 1);
        schoolGradeRepository.save(newSchoolGrade(schoolId, level.getId(), "G10", "Grade 10"));
        schoolGradeRepository.save(newSchoolGrade(schoolId, level.getId(), "G11", "Grade 11"));
        schoolGradeRepository.save(newSchoolGrade(otherSchoolId, level.getId(), "G12", "Other Grade 12"));

        List<SchoolGrade> found = schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, Set.of("G10", "G11", "G12", "MISSING"));

        assertThat(found)
            .hasSize(2)
            .extracting(grade -> grade.getCode())
            .containsExactlyInAnyOrder("G10", "G11");
    }

    private GradeLevelJpaEntity persistGradeLevel(String code, int order) {
        Instant now = Instant.now();
        GradeLevelJpaEntity level = new GradeLevelJpaEntity(
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
        Instant now = Instant.now();
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
