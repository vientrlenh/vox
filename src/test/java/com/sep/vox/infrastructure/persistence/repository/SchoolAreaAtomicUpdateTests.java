package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sep.vox.infrastructure.persistence.entity.gradelevel.GradeLevelJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolGradeJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolRoomJpaEntity;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

/** The atomic school-area statements must tell an omitted field (keep) from an explicit null (clear). */
@RepositoryTest
class SchoolAreaAtomicUpdateTests {

    private static final String STORED_TEXT = "Stored";

    @Autowired
    private SpringDataSchoolRepository springDataSchoolRepository;

    @Autowired
    private SpringDataSchoolRoomRepository springDataSchoolRoomRepository;

    @Autowired
    private SpringDataSchoolGradeRepository springDataSchoolGradeRepository;

    @Autowired
    private SpringDataGradeLevelRepository springDataGradeLevelRepository;

    @Autowired
    private EntityManager entityManager;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private UUID savedSchool() {
        SchoolJpaEntity school = SchoolJpaEntity.builder().code("S-" + uniqueSuffix()).name("School")
            .description(STORED_TEXT).contactPhone("0900" + uniqueSuffix()).contactEmail(uniqueSuffix() + "@example.com")
            .domain(STORED_TEXT + uniqueSuffix()).address("Address").studentCount(10).isActive(true).build();
        return springDataSchoolRepository.saveAndFlush(school).getId();
    }

    private void updateSchool(UUID id, String description, boolean descriptionProvided, String domain, boolean domainProvided) {
        springDataSchoolRepository.updateSchoolAtomic(id, null, description, descriptionProvided, null, null,
            domain, domainProvided, null, null, Instant.now(), "user");
        flushAndClear();
    }

    @Test
    void should_keep_school_description_and_domain_when_not_provided() {
        UUID id = savedSchool();

        updateSchool(id, null, false, null, false);

        SchoolJpaEntity reloaded = springDataSchoolRepository.findById(id).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo(STORED_TEXT);
        assertThat(reloaded.getDomain()).startsWith(STORED_TEXT);
    }

    @Test
    void should_clear_school_description_and_domain_when_provided_as_null() {
        UUID id = savedSchool();

        updateSchool(id, null, true, null, true);

        SchoolJpaEntity reloaded = springDataSchoolRepository.findById(id).orElseThrow();
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getDomain()).isNull();
        assertThat(reloaded.getName()).isEqualTo("School");
    }

    @Test
    void should_replace_school_description_and_domain_when_provided_with_a_value() {
        UUID id = savedSchool();

        updateSchool(id, "New", true, "new.edu.vn", true);

        SchoolJpaEntity reloaded = springDataSchoolRepository.findById(id).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo("New");
        assertThat(reloaded.getDomain()).isEqualTo("new.edu.vn");
    }

    private UUID savedRoom() {
        SchoolRoomJpaEntity room = SchoolRoomJpaEntity.builder().schoolId(UUID.randomUUID()).code("R-" + uniqueSuffix())
            .name("Room").description(STORED_TEXT).isActive(true).build();
        return springDataSchoolRoomRepository.saveAndFlush(room).getId();
    }

    private void updateRoom(UUID id, String description, boolean descriptionProvided) {
        springDataSchoolRoomRepository.updateSchoolRoomAtomic(id, null, description, descriptionProvided, Instant.now(), "user");
        flushAndClear();
    }

    @Test
    void should_keep_room_description_when_not_provided() {
        UUID id = savedRoom();

        updateRoom(id, null, false);

        assertThat(springDataSchoolRoomRepository.findById(id).orElseThrow().getDescription()).isEqualTo(STORED_TEXT);
    }

    @Test
    void should_clear_room_description_when_provided_as_null() {
        UUID id = savedRoom();

        updateRoom(id, null, true);

        SchoolRoomJpaEntity reloaded = springDataSchoolRoomRepository.findById(id).orElseThrow();
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getName()).isEqualTo("Room");
    }

    @Test
    void should_replace_room_description_when_provided_with_a_value() {
        UUID id = savedRoom();

        updateRoom(id, "New", true);

        assertThat(springDataSchoolRoomRepository.findById(id).orElseThrow().getDescription()).isEqualTo("New");
    }

    private UUID savedGrade() {
        SchoolGradeJpaEntity grade = SchoolGradeJpaEntity.builder().schoolId(UUID.randomUUID()).gradeLevelId(UUID.randomUUID())
            .code("G-" + uniqueSuffix()).name("Grade").description(STORED_TEXT).startDate(LocalDate.of(2026, 9, 5))
            .endDate(LocalDate.of(2027, 5, 31)).status("ACTIVE").build();
        return springDataSchoolGradeRepository.saveAndFlush(grade).getId();
    }

    private void updateGrade(UUID id, String description, boolean descriptionProvided) {
        springDataSchoolGradeRepository.updateSchoolGradeAtomic(id, null, description, descriptionProvided, null, null,
            Instant.now(), "user");
        flushAndClear();
    }

    @Test
    void should_keep_grade_description_when_not_provided() {
        UUID id = savedGrade();

        updateGrade(id, null, false);

        assertThat(springDataSchoolGradeRepository.findById(id).orElseThrow().getDescription()).isEqualTo(STORED_TEXT);
    }

    @Test
    void should_clear_grade_description_when_provided_as_null() {
        UUID id = savedGrade();

        updateGrade(id, null, true);

        SchoolGradeJpaEntity reloaded = springDataSchoolGradeRepository.findById(id).orElseThrow();
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 5));
    }

    @Test
    void should_replace_grade_description_when_provided_with_a_value() {
        UUID id = savedGrade();

        updateGrade(id, "New", true);

        assertThat(springDataSchoolGradeRepository.findById(id).orElseThrow().getDescription()).isEqualTo("New");
    }

    private UUID savedGradeLevel() {
        GradeLevelJpaEntity level = GradeLevelJpaEntity.builder().code("L-" + uniqueSuffix()).name("Level")
            .description(STORED_TEXT).order(Math.abs(UUID.randomUUID().hashCode()) % 1_000_000 + 1_000).status("ACTIVE").build();
        return springDataGradeLevelRepository.saveAndFlush(level).getId();
    }

    private void updateGradeLevel(UUID id, String description, boolean descriptionProvided) {
        springDataGradeLevelRepository.updateGradeLevelAtomic(id, null, description, descriptionProvided, null,
            Instant.now(), "user");
        flushAndClear();
    }

    @Test
    void should_keep_grade_level_description_when_not_provided() {
        UUID id = savedGradeLevel();

        updateGradeLevel(id, null, false);

        assertThat(springDataGradeLevelRepository.findById(id).orElseThrow().getDescription()).isEqualTo(STORED_TEXT);
    }

    @Test
    void should_clear_grade_level_description_when_provided_as_null() {
        UUID id = savedGradeLevel();

        updateGradeLevel(id, null, true);

        GradeLevelJpaEntity reloaded = springDataGradeLevelRepository.findById(id).orElseThrow();
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getName()).isEqualTo("Level");
    }

    @Test
    void should_replace_grade_level_description_when_provided_with_a_value() {
        UUID id = savedGradeLevel();

        updateGradeLevel(id, "New", true);

        assertThat(springDataGradeLevelRepository.findById(id).orElseThrow().getDescription()).isEqualTo("New");
    }
}
