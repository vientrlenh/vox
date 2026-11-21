package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.common.PageResult;
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
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    SchoolClassUserRepositoryImpl.class,
    UserRepositoryImpl.class,
    RoleRepositoryImpl.class,
    UserRoleRepositoryImpl.class
})
class SchoolClassUserRepositoryTests {

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
        UUID userId = UUID.randomUUID();
        UUID schoolClassId = UUID.randomUUID();
        UUID assignedBy = UUID.randomUUID();

        SchoolClassUser saved = schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(userId)
                .schoolClassId(schoolClassId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(assignedBy)
                .build()
        );

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getSchoolClassId()).isEqualTo(schoolClassId);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getAssignedBy()).isEqualTo(assignedBy);
    }

    @Test
    void whenFindByUserIdAndSchoolClassId_thenReturnsMatchingMembership() {
        UUID userId = UUID.randomUUID();
        UUID schoolClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(userId)
                .schoolClassId(schoolClassId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );

        Optional<SchoolClassUser> found = schoolClassUserRepository.findByUserIdAndSchoolClassId(userId, schoolClassId);

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
        assertThat(found.get().getSchoolClassId()).isEqualTo(schoolClassId);
    }

    @Test
    void whenFindByUserIdInAndSchoolClassIdIn_thenReturnsMatchingMemberships() {
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID anotherClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(userId)
                .schoolClassId(classId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(anotherUserId)
                .schoolClassId(anotherClassId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(UUID.randomUUID())
                .schoolClassId(UUID.randomUUID())
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );

        List<SchoolClassUser> found = schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(
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
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID anotherClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(userId)
                .schoolClassId(classId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(userId)
                .schoolClassId(anotherClassId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(UUID.randomUUID())
                .schoolClassId(UUID.randomUUID())
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );

        List<SchoolClassUser> found = schoolClassUserRepository.findByUserId(userId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(classuser -> classuser.getSchoolClassId())
            .containsExactlyInAnyOrder(classId, anotherClassId);
    }

    @Test
    void whenFindBySchoolClassId_thenReturnsOnlyMembershipsForThatClass() {
        UUID schoolClassId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(userId)
                .schoolClassId(schoolClassId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(anotherUserId)
                .schoolClassId(schoolClassId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(UUID.randomUUID())
                .schoolClassId(UUID.randomUUID())
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );

        PageResult<SchoolClassUser> found = schoolClassUserRepository.findBySchoolClassId(schoolClassId, 1, 2);

        assertThat(found).isNotNull();
    }

    @Test
    void whenExistsBySchoolClassId_thenReturnsWhetherMembershipExists() {
        UUID schoolClassId = UUID.randomUUID();
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(UUID.randomUUID())
                .schoolClassId(schoolClassId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );

        assertThat(schoolClassUserRepository.existsBySchoolClassId(schoolClassId)).isTrue();
        assertThat(schoolClassUserRepository.existsBySchoolClassId(UUID.randomUUID())).isFalse();
    }

    @Test
    void whenFindBySchoolClassIdWithRoleCode_thenReturnsOnlyMembersHoldingThatRole() {
        UUID schoolClassId = UUID.randomUUID();
        Role teacherRole = roleRepository.save(newRole("TEACHER_FILTER", "Giáo viên"));
        Role studentRole = roleRepository.save(newRole("STUDENT_FILTER", "Học sinh"));
        UUID teacher = joinClass(schoolClassId, "role-teacher@example.com", "0911000001", "Nguyen Van Teacher");
        UUID student = joinClass(schoolClassId, "role-student@example.com", "0911000002", "Tran Thi Student");
        grantRole(teacher, teacherRole.getId());
        grantRole(student, studentRole.getId());

        PageResult<SchoolClassUser> teachers = schoolClassUserRepository.findBySchoolClassId(schoolClassId, "TEACHER_FILTER", null, 1, 20);

        assertThat(teachers.content())
            .extracting(member -> member.getUserId())
            .containsExactly(teacher);
        assertThat(teachers.totalElements()).isEqualTo(1);
    }

    /**
     * EXISTS instead of a JOIN on role: a person with both roles must still be one row, otherwise
     * both the page content and totalElements are wrong.
     */
    @Test
    void whenMemberHoldsMultipleRoles_thenRowIsNotDuplicated() {
        UUID schoolClassId = UUID.randomUUID();
        Role firstRole = roleRepository.save(newRole("DUAL_ROLE_A", "Vai trò A"));
        Role secondRole = roleRepository.save(newRole("DUAL_ROLE_B", "Vai trò B"));
        UUID userId = joinClass(schoolClassId, "dual-role@example.com", "0911000003", "Le Van Dual");
        grantRole(userId, firstRole.getId());
        grantRole(userId, secondRole.getId());

        PageResult<SchoolClassUser> filtered = schoolClassUserRepository.findBySchoolClassId(schoolClassId, "DUAL_ROLE_A", null, 1, 20);
        PageResult<SchoolClassUser> unfiltered = schoolClassUserRepository.findBySchoolClassId(schoolClassId, null, null, 1, 20);

        assertThat(filtered.content()).hasSize(1);
        assertThat(filtered.totalElements()).isEqualTo(1);
        assertThat(unfiltered.content()).hasSize(1);
        assertThat(unfiltered.totalElements()).isEqualTo(1);
    }

    @Test
    void whenFindBySchoolClassIdWithSearch_thenMatchesFullNameOrEmail() {
        UUID schoolClassId = UUID.randomUUID();
        UUID byName = joinClass(schoolClassId, "search-a@example.com", "0911000004", "Hoang Thi Mai");
        UUID byEmail = joinClass(schoolClassId, "mai-search@example.com", "0911000005", "Do Van Nam");
        joinClass(schoolClassId, "search-c@example.com", "0911000006", "Pham Van Khac");

        PageResult<SchoolClassUser> found = schoolClassUserRepository.findBySchoolClassId(schoolClassId, null, "mai", 1, 20);

        assertThat(found.content())
            .extracting(member -> member.getUserId())
            .containsExactlyInAnyOrder(byName, byEmail);
        assertThat(found.totalElements()).isEqualTo(2);
    }

    @Test
    void whenCountActiveBySchoolClassIdIn_thenCountsOnlyActiveMembers() {
        UUID schoolClassId = UUID.randomUUID();
        UUID emptyClassId = UUID.randomUUID();
        Instant now = Instant.now();
        schoolClassUserRepository.save(SchoolClassUser.builder()
            .userId(UUID.randomUUID())
            .schoolClassId(schoolClassId)
            .active(true)
            .joinedAt(now)
            .leftAt(null)
            .assignedBy(UUID.randomUUID())
            .build());
        schoolClassUserRepository.save(SchoolClassUser.builder()
            .userId(UUID.randomUUID())
            .schoolClassId(schoolClassId)
            .active(true)
            .joinedAt(now)
            .leftAt(null)
            .assignedBy(UUID.randomUUID())
            .build());
        schoolClassUserRepository.save(SchoolClassUser.builder()
            .userId(UUID.randomUUID())
            .schoolClassId(schoolClassId)
            .active(false)
            .joinedAt(now)
            .leftAt(now)
            .assignedBy(UUID.randomUUID())
            .build());

        Map<UUID, Integer> counts = schoolClassUserRepository.countActiveBySchoolClassIdIn(Set.of(schoolClassId, emptyClassId));

        assertThat(counts).containsEntry(schoolClassId, 2);
        // An empty class does not appear in the GROUP BY -- the loader must fill in 0 itself.
        assertThat(counts).doesNotContainKey(emptyClassId);
    }

    @Test
    void whenCountActiveBySchoolClassIdInWithEmptyInput_thenReturnsEmptyMap() {
        assertThat(schoolClassUserRepository.countActiveBySchoolClassIdIn(Set.of())).isEmpty();
    }

    private UUID joinClass(UUID schoolClassId, String email, String phone, String fullName) {
        User user = userRepository.save(newUser(email, phone, fullName));
        schoolClassUserRepository.save(
            SchoolClassUser.builder()
                .userId(user.getId())
                .schoolClassId(schoolClassId)
                .active(true)
                .joinedAt(Instant.now())
                .leftAt(null)
                .assignedBy(UUID.randomUUID())
                .build()
        );
        return user.getId();
    }

    private void grantRole(UUID userId, UUID roleId) {
        userRoleRepository.save(UserRole.builder()
            .userId(userId)
            .roleId(roleId)
            .createdAt(Instant.now())
            .build());
    }

    private static Role newRole(String code, String name) {
        Instant now = Instant.now();
        return Role.builder()
            .code(new RoleCode(code))
            .name(name)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(UUID.randomUUID().toString())
            .updatedBy(UUID.randomUUID().toString())
            .build();
    }

    private static User newUser(String email, String phone, String fullName) {
        Instant now = Instant.now();
        return User.builder()
            .email(new Email(email))
            .passwordHash("password-hash")
            .phone(new Phone(phone))
            .fullName(new FullName(fullName))
            .dateOfBirth(new DateOfBirth(LocalDate.of(2000, 1, 1)))
            .address("Ho Chi Minh City")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
