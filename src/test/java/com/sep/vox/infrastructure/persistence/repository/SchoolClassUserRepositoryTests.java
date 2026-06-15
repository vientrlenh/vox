package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolClassUserRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    SchoolClassUserRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolClassUserRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SchoolClassUserRepository schoolClassUserRepository;

    @Test
    void whenSave_thenReturnsPersistedSchoolClassUser() {
        var userId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        var assignedBy = UUID.randomUUID();

        var saved = schoolClassUserRepository.save(
            new SchoolClassUser(userId, schoolClassId, true, OffsetDateTime.now(), null, assignedBy)
        );

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getSchoolClassId()).isEqualTo(schoolClassId);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getAssignedBy()).isEqualTo(assignedBy);
    }

    @Test
    void whenFindByUserIdAndSchoolClassId_thenReturnsMatchingMembership() {
        var userId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            new SchoolClassUser(userId, schoolClassId, true, OffsetDateTime.now(), null, UUID.randomUUID())
        );

        var found = schoolClassUserRepository.findByUserIdAndSchoolClassId(userId, schoolClassId);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getSchoolClassId()).isEqualTo(schoolClassId);
    }

    @Test
    void whenFindByUserIdInAndSchoolClassIdIn_thenReturnsMatchingMemberships() {
        var userId = UUID.randomUUID();
        var anotherUserId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var anotherClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            new SchoolClassUser(userId, classId, true, OffsetDateTime.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(anotherUserId, anotherClassId, true, OffsetDateTime.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(UUID.randomUUID(), UUID.randomUUID(), true, OffsetDateTime.now(), null, UUID.randomUUID())
        );

        var found = schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(
            Set.of(userId, anotherUserId),
            Set.of(classId, anotherClassId)
        );

        assertThat(found)
            .hasSize(2)
            .extracting(membership -> membership.getUserId() + "|" + membership.getSchoolClassId())
            .containsExactlyInAnyOrder(userId + "|" + classId, anotherUserId + "|" + anotherClassId);
    }

    @Test
    void whenFindByUserId_thenReturnsOnlyMembershipsForThatUser() {
        var userId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var anotherClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            new SchoolClassUser(userId, classId, true, OffsetDateTime.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(userId, anotherClassId, true, OffsetDateTime.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(UUID.randomUUID(), UUID.randomUUID(), true, OffsetDateTime.now(), null, UUID.randomUUID())
        );

        var found = schoolClassUserRepository.findByUserId(userId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(SchoolClassUser::getSchoolClassId)
            .containsExactlyInAnyOrder(classId, anotherClassId);
    }

    @Test
    void whenFindBySchoolClassId_thenReturnsOnlyMembershipsForThatClass() {
        var schoolClassId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var anotherUserId = UUID.randomUUID();
        schoolClassUserRepository.save(
            new SchoolClassUser(userId, schoolClassId, true, OffsetDateTime.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(anotherUserId, schoolClassId, true, OffsetDateTime.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(UUID.randomUUID(), UUID.randomUUID(), true, OffsetDateTime.now(), null, UUID.randomUUID())
        );

        var found = schoolClassUserRepository.findBySchoolClassId(schoolClassId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(SchoolClassUser::getUserId)
            .containsExactlyInAnyOrder(userId, anotherUserId);
    }

    @Test
    void whenExistsBySchoolClassId_thenReturnsWhetherMembershipExists() {
        var schoolClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            new SchoolClassUser(UUID.randomUUID(), schoolClassId, true, OffsetDateTime.now(), null, UUID.randomUUID())
        );

        assertThat(schoolClassUserRepository.existsBySchoolClassId(schoolClassId)).isTrue();
        assertThat(schoolClassUserRepository.existsBySchoolClassId(UUID.randomUUID())).isFalse();
    }
}
