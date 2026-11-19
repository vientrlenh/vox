package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.infrastructure.persistence.adapter.SchoolClassRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.SchoolClassUserRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    SchoolClassRepositoryImpl.class,
    SchoolClassUserRepositoryImpl.class
})
class SchoolClassRepositoryTests {

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SchoolClassUserRepository schoolClassUserRepository;

    @Test
    void whenSave_thenReturnsPersistedSchoolClass() {
        SchoolClass schoolClass = newSchoolClass(UUID.randomUUID(), "ENG-01", "English 01");

        SchoolClass saved = schoolClassRepository.save(schoolClass);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode().value()).isEqualTo("ENG-01");
        assertThat(saved.getName()).isEqualTo("English 01");
        assertThat(saved.getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);
    }

    @Test
    void whenFindBySchoolIdAndCode_thenReturnsOnlyMatchingClass() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, "ENG-01", "English 01"));
        schoolClassRepository.save(newSchoolClass(otherSchoolId, "ENG-01", "Other English 01"));

        Optional<SchoolClass> found = schoolClassRepository.findBySchoolIdAndCode(schoolId, "ENG-01");

        assertThat(found).isPresent();
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
        assertThat(found.get().getName()).isEqualTo("English 01");
        assertThat(schoolClassRepository.findBySchoolIdAndCode(schoolId, "MISSING")).isEmpty();
    }

    @Test
    void whenFindBySchoolIdAndCodeIn_thenReturnsOnlyMatchingClasses() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, "ENG-BATCH-01", "English Batch 01"));
        schoolClassRepository.save(newSchoolClass(schoolId, "MATH-BATCH-01", "Mathematics Batch 01"));
        schoolClassRepository.save(newSchoolClass(otherSchoolId, "SCI-BATCH-01", "Science Batch Other"));

        List<SchoolClass> found = schoolClassRepository.findBySchoolIdAndCodeIn(
            schoolId,
            Set.of("ENG-BATCH-01", "MATH-BATCH-01", "SCI-BATCH-01", "MISSING")
        );

        assertThat(found)
            .hasSize(2)
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactlyInAnyOrder("ENG-BATCH-01", "MATH-BATCH-01");
    }

    @Test
    void whenFindById_thenReturnsSchoolClass() {
        UUID schoolId = UUID.randomUUID();
        SchoolClass saved = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-DETAIL", "English Detail"));

        Optional<SchoolClass> found = schoolClassRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
        assertThat(found.get().getCode().value()).isEqualTo("ENG-DETAIL");
        assertThat(found.get().getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);
    }

    @Test
    void whenDeleteById_thenRemovesSchoolClass() {
        SchoolClass saved = schoolClassRepository.save(newSchoolClass(UUID.randomUUID(), "ENG-DELETE", "English Delete"));

        schoolClassRepository.deleteById(saved.getId());

        assertThat(schoolClassRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void whenFindBySchoolIdWithFilters_thenReturnsMatchingPagedClasses() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        UUID languageId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, gradeId, "ENG-01", "English 01", SchoolClassStatus.ACTIVE));
        schoolClassRepository.save(newSchoolClass(schoolId, languageId, UUID.randomUUID(), "MATH-01", "Mathematics 01", SchoolClassStatus.ACTIVE));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), gradeId, "ENG-ARCHIVE", "Archived English", SchoolClassStatus.ARCHIVED));
        schoolClassRepository.save(newSchoolClass(otherSchoolId, languageId, gradeId, "ENG-01", "Other English 01", SchoolClassStatus.ACTIVE));

        PageResult<SchoolClass> found = schoolClassRepository.findBySchoolId(
            schoolId,
            "eng",
            SchoolClassStatus.ACTIVE,
            languageId,
            gradeId,
            1, 
            10
        );

        assertThat(found.content()).hasSize(1);
        assertThat(found.content().get(0).getSchoolId()).isEqualTo(schoolId);
        assertThat(found.content().get(0).getCode().value()).isEqualTo("ENG-01");
        assertThat(found.totalElements()).isEqualTo(1);
        assertThat(found.totalPages()).isEqualTo(1);
    }

    @Test
    void whenFindBySchoolIdWithSearchByName_thenMatchesCaseInsensitively() {
        UUID schoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, "ENG-02", "Advanced English"));

        PageResult<SchoolClass> found = schoolClassRepository.findBySchoolId(
            schoolId,
            "advanced",
            null,
            null,
            null,
            1, 
            10
        );

        assertThat(found.content()).hasSize(1);
        assertThat(found.content().get(0).getName()).isEqualTo("Advanced English");
    }

    @Test
    void whenFindBySchoolIdWithNullSearch_thenReturnsClassesWithoutSearchError() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, "ENG-NULL-01", "English Null 01"));
        schoolClassRepository.save(newSchoolClass(schoolId, "MATH-NULL-01", "Mathematics Null 01"));
        schoolClassRepository.save(newSchoolClass(otherSchoolId, "ENG-OTHER-NULL", "Other English Null"));

        PageResult<SchoolClass> found = schoolClassRepository.findBySchoolId(
            schoolId,
            null,
            null,
            null,
            null,
            1, 
            10
        );

        assertThat(found.content()).hasSize(2);
        assertThat(found.content())
            .extracting(schoolClass -> schoolClass.getSchoolId())
            .containsOnly(schoolId);
        assertThat(found.totalElements()).isEqualTo(2);
    }

    @Test
    void whenFindBySchoolIdWithBlankSearch_thenReturnsClassesWithoutSearchError() {
        UUID schoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, "ENG-BLANK-01", "English Blank 01"));
        schoolClassRepository.save(newSchoolClass(schoolId, "MATH-BLANK-01", "Mathematics Blank 01"));

        PageResult<SchoolClass> found = schoolClassRepository.findBySchoolId(
            schoolId,
            "   ",
            null,
            null,
            null,
            1, 
            10
        );

        assertThat(found.content()).hasSize(2);
        assertThat(found.totalElements()).isEqualTo(2);
    }

    @Test
    void whenUpdateMutableFieldsWithMatchingSchool_thenUpdatesSchoolClass() {
        UUID schoolId = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        SchoolClass saved = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-UPD", "English Old"));
        Instant updatedAt = Instant.now().plus(1, ChronoUnit.DAYS);

        int updatedRows = schoolClassRepository.updateMutableFields(
            saved.getId(),
            schoolId,
            "English Updated",
            true,
            "Updated description",
            true,
            SchoolClassStatus.INACTIVE,
            true,
            updatedAt,
            updatedBy
        );

        Optional<SchoolClass> found = schoolClassRepository.findById(saved.getId());
        assertThat(updatedRows).isEqualTo(1);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("English Updated");
        assertThat(found.get().getDescription()).isEqualTo("Updated description");
        assertThat(found.get().getStatus()).isEqualTo(SchoolClassStatus.INACTIVE);
        assertThat(found.get().getUpdatedBy()).isEqualTo(updatedBy);
        assertThat(found.get().getUpdatedAt()).isAfter(saved.getUpdatedAt());
        assertThat(found.get().getCode().value()).isEqualTo("ENG-UPD");
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
    }

    @Test
    void whenUpdateMutableFieldsNameOnly_thenPreservesDescriptionAndStatus() {
        UUID schoolId = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        SchoolClass saved = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-NAME-UPD", "English Old"));

        int updatedRows = schoolClassRepository.updateMutableFields(
            saved.getId(),
            schoolId,
            "English Name Updated",
            true,
            null,
            false,
            null,
            false,
            Instant.now().plus(1, ChronoUnit.MINUTES),
            updatedBy
        );

        Optional<SchoolClass> found = schoolClassRepository.findById(saved.getId());
        assertThat(updatedRows).isEqualTo(1);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("English Name Updated");
        assertThat(found.get().getDescription()).isEqualTo("Repository test class");
        assertThat(found.get().getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);
        assertThat(found.get().getUpdatedBy()).isEqualTo(updatedBy);
    }

    @Test
    void whenUpdateMutableFieldsDescriptionNull_thenClearsDescription() {
        UUID schoolId = UUID.randomUUID();
        SchoolClass saved = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-DESC-NULL", "English Description"));

        int updatedRows = schoolClassRepository.updateMutableFields(
            saved.getId(),
            schoolId,
            null,
            false,
            null,
            true,
            null,
            false,
            Instant.now().plus(1, ChronoUnit.MINUTES),
            UUID.randomUUID()
        );

        Optional<SchoolClass> found = schoolClassRepository.findById(saved.getId());
        assertThat(updatedRows).isEqualTo(1);
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isNull();
        assertThat(found.get().getName()).isEqualTo("English Description");
        assertThat(found.get().getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);
    }

    @Test
    void whenUpdateMutableFieldsStatusOnly_thenPreservesNameAndDescription() {
        UUID schoolId = UUID.randomUUID();
        SchoolClass saved = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-STATUS-UPD", "English Status"));

        int updatedRows = schoolClassRepository.updateMutableFields(
            saved.getId(),
            schoolId,
            null,
            false,
            null,
            false,
            SchoolClassStatus.ARCHIVED,
            true,
            Instant.now().plus(1, ChronoUnit.MINUTES),
            UUID.randomUUID()
        );

        Optional<SchoolClass> found = schoolClassRepository.findById(saved.getId());
        assertThat(updatedRows).isEqualTo(1);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("English Status");
        assertThat(found.get().getDescription()).isEqualTo("Repository test class");
        assertThat(found.get().getStatus()).isEqualTo(SchoolClassStatus.ARCHIVED);
    }

    @Test
    void whenUpdateMutableFieldsWithDifferentSchool_thenDoesNotUpdateSchoolClass() {
        UUID schoolId = UUID.randomUUID();
        SchoolClass saved = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-NO-UPD", "English Old"));

        int updatedRows = schoolClassRepository.updateMutableFields(
            saved.getId(),
            UUID.randomUUID(),
            "English Updated",
            true,
            "Updated description",
            true,
            SchoolClassStatus.INACTIVE,
            true,
            Instant.now().plus(1, ChronoUnit.MINUTES),
            UUID.randomUUID()
        );

        Optional<SchoolClass> found = schoolClassRepository.findById(saved.getId());
        assertThat(updatedRows).isZero();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("English Old");
        assertThat(found.get().getStatus()).isEqualTo(SchoolClassStatus.ACTIVE);
        assertThat(found.get().getSchoolId()).isEqualTo(schoolId);
    }

    @Test
    void whenFindByUserId_thenReturnsClassesUserBelongsToIncludingLeftOnes() {
        UUID schoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SchoolClass activeClass = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-CU-01", "English CU 01"));
        SchoolClass leftClass = schoolClassRepository.save(newSchoolClass(schoolId, "MATH-CU-01", "Math CU 01"));
        SchoolClass otherUserClass = schoolClassRepository.save(newSchoolClass(schoolId, "SCI-CU-01", "Science CU 01"));
        SchoolClass otherSchoolClass = schoolClassRepository.save(newSchoolClass(otherSchoolId, "ENG-CU-02", "Other School CU"));
        Instant now = Instant.now();

        schoolClassUserRepository.save(new SchoolClassUser(userId, activeClass.getId(), true, now, null, UUID.randomUUID()));
        schoolClassUserRepository.save(new SchoolClassUser(userId, leftClass.getId(), false, now, now.plus(1, ChronoUnit.DAYS), UUID.randomUUID()));
        schoolClassUserRepository.save(new SchoolClassUser(UUID.randomUUID(), otherUserClass.getId(), true, now, null, UUID.randomUUID()));
        schoolClassUserRepository.save(new SchoolClassUser(userId, otherSchoolClass.getId(), true, now, null, UUID.randomUUID()));

        PageResult<SchoolClass> found = schoolClassRepository.findByUserId(schoolId, userId, null, 1, 20);

        assertThat(found.content())
            .extracting(schoolClass -> schoolClass.getId())
            .containsExactlyInAnyOrder(activeClass.getId(), leftClass.getId());
        assertThat(found.totalElements()).isEqualTo(2);
    }

    @Test
    void whenFindBySchoolIdWithoutStatus_thenHidesArchivedByDefault() {
        UUID schoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-VIS", "Visible English", SchoolClassStatus.ACTIVE));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-INACT", "Inactive English", SchoolClassStatus.INACTIVE));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-ARCH", "Archived English", SchoolClassStatus.ARCHIVED));

        PageResult<SchoolClass> defaultView = schoolClassRepository.findBySchoolId(schoolId, null, null, null, null, 1, 10);

        assertThat(defaultView.content())
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactlyInAnyOrder("ENG-VIS", "ENG-INACT");
        assertThat(defaultView.totalElements()).isEqualTo(2);

        PageResult<SchoolClass> archivedView = schoolClassRepository.findBySchoolId(schoolId, null, SchoolClassStatus.ARCHIVED, null, null, 1, 10);

        assertThat(archivedView.content())
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactly("ENG-ARCH");
    }

    @Test
    void whenFindByUserIdWithoutStatus_thenHidesArchivedByDefault() {
        UUID schoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SchoolClass activeClass = schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-CU-ACT", "Active CU", SchoolClassStatus.ACTIVE));
        SchoolClass archivedClass = schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-CU-ARCH", "Archived CU", SchoolClassStatus.ARCHIVED));
        Instant now = Instant.now();
        schoolClassUserRepository.save(new SchoolClassUser(userId, activeClass.getId(), true, now, null, UUID.randomUUID()));
        schoolClassUserRepository.save(new SchoolClassUser(userId, archivedClass.getId(), true, now, null, UUID.randomUUID()));

        PageResult<SchoolClass> defaultView = schoolClassRepository.findByUserId(schoolId, userId, null, 1, 20);

        assertThat(defaultView.content())
            .extracting(schoolClass -> schoolClass.getId())
            .containsExactly(activeClass.getId());

        PageResult<SchoolClass> archivedView = schoolClassRepository.findByUserId(schoolId, userId, SchoolClassStatus.ARCHIVED, 1, 20);

        assertThat(archivedView.content())
            .extracting(schoolClass -> schoolClass.getId())
            .containsExactly(archivedClass.getId());
    }

    @Test
    void whenFindBySchoolIdIn_thenHidesArchivedClasses() {
        UUID schoolId = UUID.randomUUID();
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-IN-ACT", "Active In", SchoolClassStatus.ACTIVE));
        schoolClassRepository.save(newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-IN-ARCH", "Archived In", SchoolClassStatus.ARCHIVED));

        List<SchoolClass> found = schoolClassRepository.findBySchoolIdIn(Set.of(schoolId), 1, 10);

        assertThat(found)
            .extracting(schoolClass -> schoolClass.getCode().value())
            .containsExactly("ENG-IN-ACT");
    }

    @Test
    void whenFindByUserIdWithSearch_thenMatchesCodeOrNameAcrossWholeDataset() {
        UUID schoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SchoolClass englishByCode = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-SEARCH-01", "Lop giao tiep"));
        SchoolClass englishByName = schoolClassRepository.save(newSchoolClass(schoolId, "KH-SEARCH-02", "English nang cao"));
        SchoolClass unrelated = schoolClassRepository.save(newSchoolClass(schoolId, "MATH-SEARCH-03", "Toan hoc"));
        Instant now = Instant.now();
        schoolClassUserRepository.save(new SchoolClassUser(userId, englishByCode.getId(), true, now, null, UUID.randomUUID()));
        schoolClassUserRepository.save(new SchoolClassUser(userId, englishByName.getId(), true, now, null, UUID.randomUUID()));
        schoolClassUserRepository.save(new SchoolClassUser(userId, unrelated.getId(), true, now, null, UUID.randomUUID()));

        PageResult<SchoolClass> found = schoolClassRepository.findByUserId(schoolId, userId, "eng", null, 1, 20);

        assertThat(found.content())
            .extracting(schoolClass -> schoolClass.getId())
            .containsExactlyInAnyOrder(englishByCode.getId(), englishByName.getId());
        assertThat(found.totalElements()).isEqualTo(2);
    }

    @Test
    void whenFindByUserIdWithBlankSearch_thenBehavesLikeNoSearch() {
        UUID schoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SchoolClass schoolClass = schoolClassRepository.save(newSchoolClass(schoolId, "ENG-BLANK-01", "Blank search"));
        schoolClassUserRepository.save(
            new SchoolClassUser(userId, schoolClass.getId(), true, Instant.now(), null, UUID.randomUUID())
        );

        PageResult<SchoolClass> found = schoolClassRepository.findByUserId(schoolId, userId, "   ", null, 1, 20);

        assertThat(found.content())
            .extracting(sc -> sc.getId())
            .containsExactly(schoolClass.getId());
    }

    /** page is 1-based: page 2 must skip exactly the first `size` elements, not 2*size. */
    @Test
    void whenFindByUserIdWithSearch_thenPageNumberIsOneBased() {
        UUID schoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        for (int index = 1; index <= 3; index++) {
            SchoolClass schoolClass = schoolClassRepository.save(
                newSchoolClass(schoolId, "ENG-PAGE-0" + index, "Paged English " + index)
            );
            schoolClassUserRepository.save(
                new SchoolClassUser(userId, schoolClass.getId(), true, now, null, UUID.randomUUID())
            );
        }

        PageResult<SchoolClass> firstPage = schoolClassRepository.findByUserId(schoolId, userId, "eng-page", null, 1, 2);
        PageResult<SchoolClass> secondPage = schoolClassRepository.findByUserId(schoolId, userId, "eng-page", null, 2, 2);

        assertThat(firstPage.page()).isEqualTo(1);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.page()).isEqualTo(2);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.content())
            .extracting(sc -> sc.getId())
            .doesNotContain(secondPage.content().getFirst().getId());
    }

    private static SchoolClass newSchoolClass(UUID schoolId, String code, String name) {
        return newSchoolClass(schoolId, UUID.randomUUID(), UUID.randomUUID(), code, name, SchoolClassStatus.ACTIVE);
    }

    private static SchoolClass newSchoolClass(UUID schoolId, UUID languageId, UUID gradeId, String code, String name,
            SchoolClassStatus status) {
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
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
