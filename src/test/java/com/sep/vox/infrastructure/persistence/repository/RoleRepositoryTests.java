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

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.valueobject.RoleCode;
import com.sep.vox.infrastructure.persistence.adapter.RoleRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    RoleRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoleRepositoryTests extends ContainerTestConfig {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void whenSave_thenReturnsPersistedRole() {
        var role = newRole("SCHOOL_ADMIN", "School Admin");

        var saved = roleRepository.save(role);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode().value()).isEqualTo("SCHOOL_ADMIN");
        assertThat(saved.getName()).isEqualTo("School Admin");
    }

    @Test
    void whenFindById_thenReturnsRole() {
        var saved = roleRepository.save(newRole("TEACHER", "Teacher"));

        var found = roleRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCode().value()).isEqualTo("TEACHER");
    }

    @Test
    void whenFindByCode_thenReturnsRole() {
        roleRepository.save(newRole("STUDENT", "Student"));

        var found = roleRepository.findByCode("STUDENT");

        assertThat(found).isPresent();
        assertThat(found.get().getCode().value()).isEqualTo("STUDENT");
        assertThat(found.get().getName()).isEqualTo("Student");
    }

    @Test
    void whenFindByName_thenReturnsMatchingRoles() {
        roleRepository.save(newRole("EXAM_REVIEWER", "Reviewer"));
        roleRepository.save(newRole("CONTENT_REVIEWER", "Reviewer"));
        roleRepository.save(newRole("SCHOOL_MANAGER", "Manager"));

        var found = roleRepository.findByName("Reviewer");

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(role -> role.getCode().value())
            .containsExactlyInAnyOrder("EXAM_REVIEWER", "CONTENT_REVIEWER");
    }

    @Test
    void whenCount_thenReturnsNumberOfRoles() {
        var before = roleRepository.count();
        roleRepository.save(newRole("COUNT_ROLE", "Count Role"));

        assertThat(roleRepository.count()).isEqualTo(before + 1);
    }

    private static Role newRole(String code, String name) {
        var now = OffsetDateTime.now();
        return new Role(
            new RoleCode(code),
            name,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
