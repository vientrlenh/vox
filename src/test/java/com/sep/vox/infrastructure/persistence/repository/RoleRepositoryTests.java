package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.valueobject.RoleCode;
import com.sep.vox.infrastructure.persistence.adapter.RoleRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    RoleRepositoryImpl.class
})
class RoleRepositoryTests {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void whenSave_thenReturnsPersistedRole() {
        Role role = newRole("SCHOOL_ADMIN", "School Admin");

        Role saved = roleRepository.save(role);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode().value()).isEqualTo("SCHOOL_ADMIN");
        assertThat(saved.getName()).isEqualTo("School Admin");
    }

    @Test
    void whenFindById_thenReturnsRole() {
        Role saved = roleRepository.save(newRole("TEACHER", "Teacher"));

        Optional<Role> found = roleRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCode().value()).isEqualTo("TEACHER");
    }

    @Test
    void whenFindByCode_thenReturnsRole() {
        roleRepository.save(newRole("STUDENT", "Student"));

        Optional<Role> found = roleRepository.findByCode("STUDENT");

        assertThat(found).isPresent();
        assertThat(found.get().getCode().value()).isEqualTo("STUDENT");
        assertThat(found.get().getName()).isEqualTo("Student");
    }

    @Test
    void whenFindByName_thenReturnsMatchingRoles() {
        roleRepository.save(newRole("EXAM_REVIEWER", "Reviewer"));
        roleRepository.save(newRole("CONTENT_REVIEWER", "Reviewer"));
        roleRepository.save(newRole("SCHOOL_MANAGER", "Manager"));

        List<Role> found = roleRepository.findByName("Reviewer");

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(role -> role.getCode().value())
            .containsExactlyInAnyOrder("EXAM_REVIEWER", "CONTENT_REVIEWER");
    }

    @Test
    void whenCount_thenReturnsNumberOfRoles() {
        long before = roleRepository.count();
        roleRepository.save(newRole("COUNT_ROLE", "Count Role"));

        assertThat(roleRepository.count()).isEqualTo(before + 1);
    }

    private static Role newRole(String code, String name) {
        Instant now = Instant.now();
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
