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

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolUserRepositoryImpl;
import com.sep.vox.infrastructure.persistence.entity.user.RoleJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolClassUserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.user.UserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.user.UserRoleJpaEntity;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    SchoolUserRepositoryImpl.class
})
class SchoolUserRepositoryTests {

    private static final Instant NOW = Instant.now();

    @Autowired
    private SchoolUserRepository schoolUserRepository;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private SpringDataRoleRepository springDataRoleRepository;

    @Autowired
    private SpringDataUserRoleRepository springDataUserRoleRepository;

    @Autowired
    private SpringDataSchoolClassUserRepository springDataSchoolClassUserRepository;

    @Test
    void whenFindByUserIdIn_thenReturnsMatchingSchoolUsers() {
        UUID schoolId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        schoolUserRepository.save(SchoolUser.builder()
            .schoolId(schoolId)
            .userId(userId)
            .startDate(Instant.now())
            .endDate(null)
            .build());
        schoolUserRepository.save(SchoolUser.builder()
            .schoolId(schoolId)
            .userId(anotherUserId)
            .startDate(Instant.now())
            .endDate(null)
            .build());
        schoolUserRepository.save(SchoolUser.builder()
            .schoolId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .startDate(Instant.now())
            .endDate(null)
            .build());

        List<SchoolUser> found = schoolUserRepository.findByUserIdIn(Set.of(userId, anotherUserId, UUID.randomUUID()));

        assertThat(found)
            .hasSize(2)
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(userId, anotherUserId);
    }

    @Test
    void search_without_role_filter_should_return_all_active_members_excluding_disabled() {
        UUID schoolId = UUID.randomUUID();
        UUID studentRoleId = saveRole("STUDENT");
        UUID teacherRoleId = saveRole("TEACHER");
        UUID adminRoleId = saveRole("SCHOOL_ADMIN");

        UUID studentId = saveMember(schoolId, studentRoleId, "student@school.vn", "0900000001", "Nguyễn Văn Học", "ACTIVE");
        UUID teacherId = saveMember(schoolId, teacherRoleId, "teacher@school.vn", "0900000002", "Trần Thị Viên", "ACTIVE");
        // Admin: belongs to the school -> still shown when excludeAdminRoles is off.
        UUID adminId = saveMember(schoolId, adminRoleId, "admin@school.vn", "0900000003", "Quản Trị", "ACTIVE");
        // A DISABLED student -> hidden by default.
        saveMember(schoolId, studentRoleId, "disabled@school.vn", "0900000004", "Lê Văn Khóa", "DISABLED");

        PageResult<SchoolUser> result = schoolUserRepository.findBySchoolId(schoolId, null, null, null, null, false, 1, 20);

        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(studentId, teacherId, adminId);
    }

    @Test
    void search_should_exclude_admins_when_exclude_admin_roles_is_requested() {
        UUID schoolId = UUID.randomUUID();
        UUID studentRoleId = saveRole("STUDENT");
        UUID teacherRoleId = saveRole("TEACHER");
        UUID schoolAdminRoleId = saveRole("SCHOOL_ADMIN");
        UUID systemAdminRoleId = saveRole("SYSTEM_ADMIN");

        UUID studentId = saveMember(schoolId, studentRoleId, "student@school.vn", "0900000001", "Nguyễn Văn Học", "ACTIVE");
        UUID teacherId = saveMember(schoolId, teacherRoleId, "teacher@school.vn", "0900000002", "Trần Thị Viên", "ACTIVE");
        saveMember(schoolId, schoolAdminRoleId, "admin@school.vn", "0900000003", "Quản Trị Trường", "ACTIVE");
        saveMember(schoolId, systemAdminRoleId, "sysadmin@school.vn", "0900000004", "Quản Trị Hệ Thống", "ACTIVE");

        PageResult<SchoolUser> result = schoolUserRepository.findBySchoolId(schoolId, null, null, null, null, true, 1, 20);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(studentId, teacherId);
    }

    @Test
    void search_should_return_members_who_have_not_set_password_yet() {
        UUID schoolId = UUID.randomUUID();
        UUID studentRoleId = saveRole("STUDENT");
        UUID pendingId = saveMember(schoolId, studentRoleId, "pending@school.vn", "0900000001", "Chưa Kích Hoạt", "INACTIVE");
        UUID activeId = saveMember(schoolId, studentRoleId, "active@school.vn", "0900000002", "Đã Kích Hoạt", "ACTIVE");

        PageResult<SchoolUser> result = schoolUserRepository.findBySchoolId(schoolId, null, null, null, null, true, 1, 20);

        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(pendingId, activeId);
    }

    @Test
    void search_should_filter_by_role_id() {
        UUID schoolId = UUID.randomUUID();
        UUID studentRoleId = saveRole("STUDENT");
        UUID teacherRoleId = saveRole("TEACHER");
        saveMember(schoolId, studentRoleId, "student@school.vn", "0900000001", "Nguyễn Văn Học", "ACTIVE");
        UUID teacherId = saveMember(schoolId, teacherRoleId, "teacher@school.vn", "0900000002", "Trần Thị Viên", "ACTIVE");

        PageResult<SchoolUser> result = schoolUserRepository.findBySchoolId(schoolId, null, teacherRoleId, null, null, true, 1, 20);

        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactly(teacherId);
    }

    @Test
    void search_should_match_name_email_or_phone() {
        UUID schoolId = UUID.randomUUID();
        UUID studentRoleId = saveRole("STUDENT");
        UUID studentId = saveMember(schoolId, studentRoleId, "an.nguyen@school.vn", "0911111111", "Nguyễn Văn An", "ACTIVE");
        saveMember(schoolId, studentRoleId, "binh.tran@school.vn", "0922222222", "Trần Thị Bình", "ACTIVE");

        assertThat(schoolUserRepository.findBySchoolId(schoolId, "an.nguyen", null, null, null, true, 1, 20).content())
            .extracting(user -> user.getUserId()).containsExactly(studentId);
        assertThat(schoolUserRepository.findBySchoolId(schoolId, "0911111111", null, null, null, true, 1, 20).content())
            .extracting(user -> user.getUserId()).containsExactly(studentId);
        assertThat(schoolUserRepository.findBySchoolId(schoolId, "văn an", null, null, null, true, 1, 20).content())
            .extracting(user -> user.getUserId()).containsExactly(studentId);
    }

    @Test
    void search_should_return_disabled_when_status_filter_provided() {
        UUID schoolId = UUID.randomUUID();
        UUID studentRoleId = saveRole("STUDENT");
        saveMember(schoolId, studentRoleId, "active@school.vn", "0900000001", "Người Hoạt Động", "ACTIVE");
        UUID disabledId = saveMember(schoolId, studentRoleId, "disabled@school.vn", "0900000002", "Người Khóa", "DISABLED");

        PageResult<SchoolUser> result = schoolUserRepository.findBySchoolId(schoolId, null, null, "DISABLED", null, true, 1, 20);

        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactly(disabledId);
    }

    @Test
    void search_should_exclude_users_actively_enrolled_in_the_given_class() {
        UUID schoolId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID studentRoleId = saveRole("STUDENT");
        UUID enrolledId = saveMember(schoolId, studentRoleId, "enrolled@school.vn", "0900000001", "Đang Trong Lớp", "ACTIVE");
        UUID leftId = saveMember(schoolId, studentRoleId, "left@school.vn", "0900000002", "Đã Rời Lớp", "ACTIVE");
        UUID freeId = saveMember(schoolId, studentRoleId, "free@school.vn", "0900000003", "Chưa Vào Lớp", "ACTIVE");
        springDataSchoolClassUserRepository.saveAndFlush(
            SchoolClassUserJpaEntity.builder()
                .userId(enrolledId)
                .schoolClassId(classId)
                .isActive(true)
                .joinedAt(NOW)
                .leftAt(null)
                .assignedBy(enrolledId)
                .build());
        // Has left the class -> must still be shown so they can be added again.
        springDataSchoolClassUserRepository.saveAndFlush(
            SchoolClassUserJpaEntity.builder()
                .userId(leftId)
                .schoolClassId(classId)
                .isActive(false)
                .joinedAt(NOW)
                .leftAt(NOW)
                .assignedBy(leftId)
                .build());

        PageResult<SchoolUser> result = schoolUserRepository.findBySchoolId(schoolId, null, null, null, classId, true, 1, 20);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(leftId, freeId);
    }

    private UUID saveRole(String code) {
        return springDataRoleRepository.saveAndFlush(RoleJpaEntity.builder()
            .code(code)
            .name(code)
            .createdAt(NOW)
            .updatedAt(NOW)
            .build()).getId();
    }

    private UUID saveMember(UUID schoolId, UUID roleId, String email, String phone, String fullName, String status) {
        UserJpaEntity user = springDataUserRepository.saveAndFlush(UserJpaEntity.builder()
            .email(email)
            .passwordHash("hash")
            .phone(phone)
            .fullName(fullName)
            .dateOfBirth(LocalDate.of(2000, 1, 1))
            .status(status)
            .createdAt(NOW)
            .updatedAt(NOW)
            .build());
        springDataUserRoleRepository.saveAndFlush(UserRoleJpaEntity.builder()
            .userId(user.getId())
            .roleId(roleId)
            .createdAt(NOW)
            .build());
        schoolUserRepository.save(SchoolUser.builder()
            .schoolId(schoolId)
            .userId(user.getId())
            .startDate(NOW)
            .endDate(null)
            .build());
        return user.getId();
    }
}
