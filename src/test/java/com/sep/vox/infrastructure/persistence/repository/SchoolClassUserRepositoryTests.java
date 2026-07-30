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
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.RoleCode;
import com.sep.vox.infrastructure.persistence.adapter.RoleRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.SchoolClassUserRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.UserRepositoryImpl;
import com.sep.vox.infrastructure.persistence.adapter.UserRoleRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    SchoolClassUserRepositoryImpl.class,
    UserRepositoryImpl.class,
    RoleRepositoryImpl.class,
    UserRoleRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolClassUserRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SchoolClassUserRepository schoolClassUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    void whenSave_thenReturnsPersistedSchoolClassUser() {
        var userId = UUID.randomUUID();
        var schoolClassId = UUID.randomUUID();
        var assignedBy = UUID.randomUUID();

        var saved = schoolClassUserRepository.save(
            new SchoolClassUser(userId, schoolClassId, true, Instant.now(), null, assignedBy)
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
            new SchoolClassUser(userId, schoolClassId, true, Instant.now(), null, UUID.randomUUID())
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
            new SchoolClassUser(userId, classId, true, Instant.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(anotherUserId, anotherClassId, true, Instant.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(UUID.randomUUID(), UUID.randomUUID(), true, Instant.now(), null, UUID.randomUUID())
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
            new SchoolClassUser(userId, classId, true, Instant.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(userId, anotherClassId, true, Instant.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(UUID.randomUUID(), UUID.randomUUID(), true, Instant.now(), null, UUID.randomUUID())
        );

        var found = schoolClassUserRepository.findByUserId(userId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(classuser -> classuser.getSchoolClassId())
            .containsExactlyInAnyOrder(classId, anotherClassId);
    }

    @Test
    void whenFindBySchoolClassId_thenReturnsOnlyMembershipsForThatClass() {
        var schoolClassId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var anotherUserId = UUID.randomUUID();
        schoolClassUserRepository.save(
            new SchoolClassUser(userId, schoolClassId, true, Instant.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(anotherUserId, schoolClassId, true, Instant.now(), null, UUID.randomUUID())
        );
        schoolClassUserRepository.save(
            new SchoolClassUser(UUID.randomUUID(), UUID.randomUUID(), true, Instant.now(), null, UUID.randomUUID())
        );

        var found = schoolClassUserRepository.findBySchoolClassId(schoolClassId, 1, 2);

        assertThat(found).isNotNull();
    }

    @Test
    void whenExistsBySchoolClassId_thenReturnsWhetherMembershipExists() {
        var schoolClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            new SchoolClassUser(UUID.randomUUID(), schoolClassId, true, Instant.now(), null, UUID.randomUUID())
        );

        assertThat(schoolClassUserRepository.existsBySchoolClassId(schoolClassId)).isTrue();
        assertThat(schoolClassUserRepository.existsBySchoolClassId(UUID.randomUUID())).isFalse();
    }

    @Test
    void whenFindBySchoolClassIdWithRoleCode_thenReturnsOnlyMembersHoldingThatRole() {
        var schoolClassId = UUID.randomUUID();
        var teacherRole = roleRepository.save(newRole("TEACHER_FILTER", "Giáo viên"));
        var studentRole = roleRepository.save(newRole("STUDENT_FILTER", "Học sinh"));
        var teacher = joinClass(schoolClassId, "role-teacher@example.com", "0911000001", "Nguyen Van Teacher");
        var student = joinClass(schoolClassId, "role-student@example.com", "0911000002", "Tran Thi Student");
        grantRole(teacher, teacherRole.getId());
        grantRole(student, studentRole.getId());

        var teachers = schoolClassUserRepository.findBySchoolClassId(schoolClassId, "TEACHER_FILTER", null, 1, 20);

        assertThat(teachers.content())
            .extracting(member -> member.getUserId())
            .containsExactly(teacher);
        assertThat(teachers.totalElements()).isEqualTo(1);
    }

    /**
     * EXISTS thay vì JOIN role: người mang cả hai vai trò vẫn phải là một dòng,
     * nếu không cả nội dung trang lẫn totalElements đều sai.
     */
    @Test
    void whenMemberHoldsMultipleRoles_thenRowIsNotDuplicated() {
        var schoolClassId = UUID.randomUUID();
        var firstRole = roleRepository.save(newRole("DUAL_ROLE_A", "Vai trò A"));
        var secondRole = roleRepository.save(newRole("DUAL_ROLE_B", "Vai trò B"));
        var userId = joinClass(schoolClassId, "dual-role@example.com", "0911000003", "Le Van Dual");
        grantRole(userId, firstRole.getId());
        grantRole(userId, secondRole.getId());

        var filtered = schoolClassUserRepository.findBySchoolClassId(schoolClassId, "DUAL_ROLE_A", null, 1, 20);
        var unfiltered = schoolClassUserRepository.findBySchoolClassId(schoolClassId, null, null, 1, 20);

        assertThat(filtered.content()).hasSize(1);
        assertThat(filtered.totalElements()).isEqualTo(1);
        assertThat(unfiltered.content()).hasSize(1);
        assertThat(unfiltered.totalElements()).isEqualTo(1);
    }

    @Test
    void whenFindBySchoolClassIdWithSearch_thenMatchesFullNameOrEmail() {
        var schoolClassId = UUID.randomUUID();
        var byName = joinClass(schoolClassId, "search-a@example.com", "0911000004", "Hoang Thi Mai");
        var byEmail = joinClass(schoolClassId, "mai-search@example.com", "0911000005", "Do Van Nam");
        joinClass(schoolClassId, "search-c@example.com", "0911000006", "Pham Van Khac");

        var found = schoolClassUserRepository.findBySchoolClassId(schoolClassId, null, "mai", 1, 20);

        assertThat(found.content())
            .extracting(member -> member.getUserId())
            .containsExactlyInAnyOrder(byName, byEmail);
        assertThat(found.totalElements()).isEqualTo(2);
    }

    @Test
    void whenCountActiveBySchoolClassIdIn_thenCountsOnlyActiveMembers() {
        var schoolClassId = UUID.randomUUID();
        var emptyClassId = UUID.randomUUID();
        var now = Instant.now();
        schoolClassUserRepository.save(new SchoolClassUser(UUID.randomUUID(), schoolClassId, true, now, null, UUID.randomUUID()));
        schoolClassUserRepository.save(new SchoolClassUser(UUID.randomUUID(), schoolClassId, true, now, null, UUID.randomUUID()));
        schoolClassUserRepository.save(new SchoolClassUser(UUID.randomUUID(), schoolClassId, false, now, now, UUID.randomUUID()));

        var counts = schoolClassUserRepository.countActiveBySchoolClassIdIn(Set.of(schoolClassId, emptyClassId));

        assertThat(counts).containsEntry(schoolClassId, 2);
        // Lớp rỗng không xuất hiện trong GROUP BY — loader phải tự bù 0.
        assertThat(counts).doesNotContainKey(emptyClassId);
    }

    @Test
    void whenCountActiveBySchoolClassIdInWithEmptyInput_thenReturnsEmptyMap() {
        assertThat(schoolClassUserRepository.countActiveBySchoolClassIdIn(Set.of())).isEmpty();
    }

    private UUID joinClass(UUID schoolClassId, String email, String phone, String fullName) {
        var user = userRepository.save(newUser(email, phone, fullName));
        schoolClassUserRepository.save(
            new SchoolClassUser(user.getId(), schoolClassId, true, Instant.now(), null, UUID.randomUUID())
        );
        return user.getId();
    }

    private void grantRole(UUID userId, UUID roleId) {
        userRoleRepository.save(new UserRole(userId, roleId, Instant.now()));
    }

    private static Role newRole(String code, String name) {
        var now = Instant.now();
        return new Role(new RoleCode(code), name, now, now, UUID.randomUUID(), UUID.randomUUID());
    }

    private static User newUser(String email, String phone, String fullName) {
        var now = Instant.now();
        return new User(
            new Email(email),
            "password-hash",
            new Phone(phone),
            new FullName(fullName),
            null,
            new DateOfBirth(LocalDate.of(2000, 1, 1)),
            "Ho Chi Minh City",
            null,
            UserStatus.ACTIVE,
            now,
            now,
            null,
            null
        );
    }
}
