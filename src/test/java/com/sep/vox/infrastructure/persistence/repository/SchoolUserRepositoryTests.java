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
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolUserRepositoryImpl;
import com.sep.vox.infrastructure.persistence.entity.RoleJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassUserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserRoleJpaEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    SchoolUserRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolUserRepositoryTests extends ContainerTestConfig {

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
        var schoolId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var anotherUserId = UUID.randomUUID();
        schoolUserRepository.save(new SchoolUser(schoolId, userId, Instant.now(), null));
        schoolUserRepository.save(new SchoolUser(schoolId, anotherUserId, Instant.now(), null));
        schoolUserRepository.save(new SchoolUser(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), null));

        var found = schoolUserRepository.findByUserIdIn(Set.of(userId, anotherUserId, UUID.randomUUID()));

        assertThat(found)
            .hasSize(2)
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(userId, anotherUserId);
    }

    @Test
    void search_without_role_filter_should_return_all_active_members_excluding_disabled() {
        var schoolId = UUID.randomUUID();
        var studentRoleId = saveRole("STUDENT");
        var teacherRoleId = saveRole("TEACHER");
        var adminRoleId = saveRole("SCHOOL_ADMIN");

        var studentId = saveMember(schoolId, studentRoleId, "student@school.vn", "0900000001", "Nguyễn Văn Học", "ACTIVE");
        var teacherId = saveMember(schoolId, teacherRoleId, "teacher@school.vn", "0900000002", "Trần Thị Viên", "ACTIVE");
        // Admin: thuộc trường -> vẫn hiện khi không bật excludeAdminRoles
        var adminId = saveMember(schoolId, adminRoleId, "admin@school.vn", "0900000003", "Quản Trị", "ACTIVE");
        // Học sinh DISABLED -> bị ẩn mặc định
        saveMember(schoolId, studentRoleId, "disabled@school.vn", "0900000004", "Lê Văn Khóa", "DISABLED");

        var result = schoolUserRepository.findBySchoolId(schoolId, null, null, null, null, false, 1, 20);

        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(studentId, teacherId, adminId);
    }

    @Test
    void search_should_exclude_admins_when_exclude_admin_roles_is_requested() {
        var schoolId = UUID.randomUUID();
        var studentRoleId = saveRole("STUDENT");
        var teacherRoleId = saveRole("TEACHER");
        var schoolAdminRoleId = saveRole("SCHOOL_ADMIN");
        var systemAdminRoleId = saveRole("SYSTEM_ADMIN");

        var studentId = saveMember(schoolId, studentRoleId, "student@school.vn", "0900000001", "Nguyễn Văn Học", "ACTIVE");
        var teacherId = saveMember(schoolId, teacherRoleId, "teacher@school.vn", "0900000002", "Trần Thị Viên", "ACTIVE");
        saveMember(schoolId, schoolAdminRoleId, "admin@school.vn", "0900000003", "Quản Trị Trường", "ACTIVE");
        saveMember(schoolId, systemAdminRoleId, "sysadmin@school.vn", "0900000004", "Quản Trị Hệ Thống", "ACTIVE");

        var result = schoolUserRepository.findBySchoolId(schoolId, null, null, null, null, true, 1, 20);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(studentId, teacherId);
    }

    @Test
    void search_should_return_members_who_have_not_set_password_yet() {
        var schoolId = UUID.randomUUID();
        var studentRoleId = saveRole("STUDENT");
        var pendingId = saveMember(schoolId, studentRoleId, "pending@school.vn", "0900000001", "Chưa Kích Hoạt", "INACTIVE");
        var activeId = saveMember(schoolId, studentRoleId, "active@school.vn", "0900000002", "Đã Kích Hoạt", "ACTIVE");

        var result = schoolUserRepository.findBySchoolId(schoolId, null, null, null, null, true, 1, 20);

        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(pendingId, activeId);
    }

    @Test
    void search_should_filter_by_role_id() {
        var schoolId = UUID.randomUUID();
        var studentRoleId = saveRole("STUDENT");
        var teacherRoleId = saveRole("TEACHER");
        saveMember(schoolId, studentRoleId, "student@school.vn", "0900000001", "Nguyễn Văn Học", "ACTIVE");
        var teacherId = saveMember(schoolId, teacherRoleId, "teacher@school.vn", "0900000002", "Trần Thị Viên", "ACTIVE");

        var result = schoolUserRepository.findBySchoolId(schoolId, null, teacherRoleId, null, null, true, 1, 20);

        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactly(teacherId);
    }

    @Test
    void search_should_match_name_email_or_phone() {
        var schoolId = UUID.randomUUID();
        var studentRoleId = saveRole("STUDENT");
        var studentId = saveMember(schoolId, studentRoleId, "an.nguyen@school.vn", "0911111111", "Nguyễn Văn An", "ACTIVE");
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
        var schoolId = UUID.randomUUID();
        var studentRoleId = saveRole("STUDENT");
        saveMember(schoolId, studentRoleId, "active@school.vn", "0900000001", "Người Hoạt Động", "ACTIVE");
        var disabledId = saveMember(schoolId, studentRoleId, "disabled@school.vn", "0900000002", "Người Khóa", "DISABLED");

        var result = schoolUserRepository.findBySchoolId(schoolId, null, null, "DISABLED", null, true, 1, 20);

        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactly(disabledId);
    }

    @Test
    void search_should_exclude_users_actively_enrolled_in_the_given_class() {
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var studentRoleId = saveRole("STUDENT");
        var enrolledId = saveMember(schoolId, studentRoleId, "enrolled@school.vn", "0900000001", "Đang Trong Lớp", "ACTIVE");
        var leftId = saveMember(schoolId, studentRoleId, "left@school.vn", "0900000002", "Đã Rời Lớp", "ACTIVE");
        var freeId = saveMember(schoolId, studentRoleId, "free@school.vn", "0900000003", "Chưa Vào Lớp", "ACTIVE");
        springDataSchoolClassUserRepository.saveAndFlush(
            new SchoolClassUserJpaEntity(enrolledId, classId, true, NOW, null, enrolledId));
        // Đã rời lớp -> vẫn phải hiện để có thể thêm lại
        springDataSchoolClassUserRepository.saveAndFlush(
            new SchoolClassUserJpaEntity(leftId, classId, false, NOW, NOW, leftId));

        var result = schoolUserRepository.findBySchoolId(schoolId, null, null, null, classId, true, 1, 20);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content())
            .extracting(user -> user.getUserId())
            .containsExactlyInAnyOrder(leftId, freeId);
    }

    private UUID saveRole(String code) {
        return springDataRoleRepository.saveAndFlush(new RoleJpaEntity(null, code, code, NOW, NOW, null, null)).getId();
    }

    private UUID saveMember(UUID schoolId, UUID roleId, String email, String phone, String fullName, String status) {
        var user = springDataUserRepository.saveAndFlush(new UserJpaEntity(
            null, email, "hash", phone, fullName, null, LocalDate.of(2000, 1, 1), null, null,
            status, NOW, NOW, null, null));
        springDataUserRoleRepository.saveAndFlush(new UserRoleJpaEntity(null, user.getId(), roleId, NOW));
        schoolUserRepository.save(new SchoolUser(schoolId, user.getId(), NOW, null));
        return user.getId();
    }
}
