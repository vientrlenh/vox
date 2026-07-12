package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolGradeLevelRepositoryImpl;
import com.sep.vox.infrastructure.persistence.entity.SchoolGradeLevelJpaEntity;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    SchoolGradeLevelRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolGradeLevelRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SchoolGradeLevelRepository schoolGradeLevelRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenFindBySchoolId_thenReturnsOnlyMatchingSchoolOrderedByOrder() {
        var schoolId = UUID.randomUUID();
        var otherSchoolId = UUID.randomUUID();
        persist(schoolId, "K3", "Khối 3", 3, "ACTIVE");
        persist(schoolId, "K1", "Khối 1", 1, "ACTIVE");
        persist(schoolId, "K2", "Khối 2", 2, "ACTIVE");
        persist(otherSchoolId, "K9", "Khối 9", 1, "ACTIVE");

        var result = schoolGradeLevelRepository.findBySchoolId(schoolId, null, null, 1, 20);

        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.content())
            .extracting(gradelevel -> gradelevel.getOrder())
            .containsExactly(1, 2, 3);
    }

    @Test
    void whenFindBySchoolIdWithSearch_thenFiltersByCodeOrName() {
        var schoolId = UUID.randomUUID();
        persist(schoolId, "GRADE-A", "Alpha", 1, "ACTIVE");
        persist(schoolId, "GRADE-B", "Beta", 2, "ACTIVE");

        var byCode = schoolGradeLevelRepository.findBySchoolId(schoolId, "grade-a", null, 1, 20);
        assertThat(byCode.content())
            .extracting(gradelevel -> gradelevel.getCode())
            .containsExactly("GRADE-A");

        var byName = schoolGradeLevelRepository.findBySchoolId(schoolId, "beta", null, 1, 20);
        assertThat(byName.content())
            .extracting(gradelevel -> gradelevel.getName())
            .containsExactly("Beta");
    }

    @Test
    void whenFindBySchoolIdWithStatus_thenFiltersByStatus() {
        var schoolId = UUID.randomUUID();
        persist(schoolId, "K1", "Khối 1", 1, "ACTIVE");
        persist(schoolId, "K2", "Khối 2", 2, "INACTIVE");

        var result = schoolGradeLevelRepository.findBySchoolId(schoolId, null, SchoolGradeLevelStatus.INACTIVE, 1, 20);

        assertThat(result.content())
            .extracting(gradelevel -> gradelevel.getCode())
            .containsExactly("K2");
    }

    @Test
    void whenFindBySchoolIdWithPagination_thenReturnsPageMetadata() {
        var schoolId = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) {
            persist(schoolId, "K" + i, "Khối " + i, i, "ACTIVE");
        }

        var result = schoolGradeLevelRepository.findBySchoolId(schoolId, null, null, 1, 2);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content())
            .extracting(gradelevel -> gradelevel.getOrder())
            .containsExactly(1, 2);
    }

    @Test
    void whenUpdateAtomic_thenChangesProvidedFieldsAndKeepsNullOnes() {
        var schoolId = UUID.randomUUID();
        var level = persist(schoolId, "K1", "Khối 1", 1, "ACTIVE");

        var rows = schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(
            level.getId(), "Khối 1 mới", null, 9, OffsetDateTime.now(), UUID.randomUUID());
        entityManager.clear(); // bulk update bỏ qua persistence context, cần clear để đọc lại từ DB

        assertThat(rows).isEqualTo(1);
        var reloaded = schoolGradeLevelRepository.findById(level.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Khối 1 mới");
        assertThat(reloaded.getDescription()).isEqualTo("Repository test grade level"); // null param giữ nguyên
        assertThat(reloaded.getOrder()).isEqualTo(9);
    }

    @Test
    void whenUpdateAtomicWithWrongId_thenUpdatesZeroRows() {
        var schoolId = UUID.randomUUID();
        persist(schoolId, "K1", "Khối 1", 1, "ACTIVE");

        var rows = schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(
            UUID.randomUUID(), "X", null, null, OffsetDateTime.now(), UUID.randomUUID());

        assertThat(rows).isZero();
    }

    @Test
    void whenUpdateAtomicToDuplicateOrder_thenThrowsDataIntegrityViolation() {
        var schoolId = UUID.randomUUID();
        persist(schoolId, "K1", "Khối 1", 1, "ACTIVE");
        var second = persist(schoolId, "K2", "Khối 2", 2, "ACTIVE");

        assertThatThrownBy(() -> {
            schoolGradeLevelRepository.updateSchoolGradeLevelAtomic(
                second.getId(), null, null, 1, OffsetDateTime.now(), UUID.randomUUID());
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private SchoolGradeLevelJpaEntity persist(UUID schoolId, String code, String name, int order, String status) {
        var now = OffsetDateTime.now();
        var entity = new SchoolGradeLevelJpaEntity(
            null, schoolId, code, name, "Repository test grade level", order, status,
            now, now, UUID.randomUUID(), UUID.randomUUID()
        );
        entityManager.persist(entity);
        entityManager.flush();
        entityManager.refresh(entity);
        return entity;
    }
}
