package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.infrastructure.persistence.entity.RoleJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolClassUserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolGradeJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.GradeLevelJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.SchoolUserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.UserRoleJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Danh bạ kỳ thi chạy trên DB thật.
 *
 * <p>Hai thứ chỉ DB mới lộ ra: (1) một học sinh học nhiều lớp trong cùng tập lọc — JOIN
 * to-many sẽ nhân dòng và làm lệch {@code totalElements} so với query COUNT; (2) chuỗi
 * JPQL dựng bằng EntityManager không được compiler kiểm, sai tên entity/field chỉ nổ
 * lúc chạy.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
@Transactional
class JpaExamDirectoryQueryRepositoryTests extends ContainerTestConfig {

    @Autowired
    private JpaExamDirectoryQueryRepository repository;

    @PersistenceContext
    private EntityManager em;

    private UUID schoolId;
    private UUID gradeLevelId;
    private UUID studentRoleId;
    private UUID teacherRoleId;
    private Instant now;

    // Id do DB sinh (@Generated(INSERT), insertable=false) nên fixture phải persist với
    // id null rồi đọc lại — truyền id sẵn bị coi là detached.
    private <T> T persisted(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        now = OffsetDateTime.parse("2026-07-29T09:00:00+07:00").toInstant();
        gradeLevelId = persisted(new GradeLevelJpaEntity(
            null, "K10-" + suffix(), "Khối 10", null, nextGradeLevelOrder(), "ACTIVE", now, now, null, null)).getId();
        studentRoleId = roleId("STUDENT");
        teacherRoleId = roleId("TEACHER");
    }

    @Test
    void should_count_a_student_once_even_when_they_belong_to_several_filtered_classes() {
        var class1 = schoolClass("A1");
        var class2 = schoolClass("A2");
        var student = student("Nguyen Van An", "an");
        enroll(student, class1, true);
        enroll(student, class2, true);

        var page = repository.findUsersByClassIds(List.of(class1, class2), "STUDENT", null, 1, 20);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).extracting(user -> user.userId()).containsExactly(student);
    }

    @Test
    void should_ignore_inactive_memberships_and_users_of_other_classes() {
        var mine = schoolClass("B1");
        var other = schoolClass("B2");
        var active = student("Active Student", "active");
        var leftClass = student("Left Student", "left");
        var elsewhere = student("Other Student", "other");
        enroll(active, mine, true);
        enroll(leftClass, mine, false);
        enroll(elsewhere, other, true);

        var page = repository.findUsersByClassIds(List.of(mine), "STUDENT", null, 1, 20);

        assertThat(page.content()).extracting(user -> user.userId()).containsExactly(active);
    }

    @Test
    void should_return_empty_page_for_a_caller_who_teaches_no_class() {
        // Tập lớp rỗng tuyệt đối không được suy thành "toàn trường".
        var page = repository.findUsersByClassIds(List.of(), "STUDENT", null, 1, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void should_filter_school_users_by_role_and_search() {
        var student = student("Tran Thi Binh", "binh");
        var teacher = teacher("Le Van Cuong", "cuong");
        joinSchool(student);
        joinSchool(teacher);

        var students = repository.findUsersBySchoolId(schoolId, "STUDENT", null, 1, 20);
        var teachers = repository.findUsersBySchoolId(schoolId, "TEACHER", null, 1, 20);
        var searched = repository.findUsersBySchoolId(schoolId, "STUDENT", "BINH", 1, 20);
        var missed = repository.findUsersBySchoolId(schoolId, "STUDENT", "cuong", 1, 20);

        assertThat(students.content()).extracting(user -> user.userId()).containsExactly(student);
        assertThat(teachers.content()).extracting(user -> user.userId()).containsExactly(teacher);
        assertThat(searched.content()).extracting(user -> user.userId()).containsExactly(student);
        assertThat(missed.content()).isEmpty();
    }

    @Test
    void should_list_active_grades_of_the_school_only() {
        var active = grade("NK-2026", "ACTIVE");
        grade("NK-2020", "ARCHIVED");
        // Khối lớp dùng chung -> niên khóa trường khác vẫn trỏ cùng gradeLevelId, chỉ khác school_id.
        persisted(new SchoolGradeJpaEntity(null, UUID.randomUUID(), gradeLevelId, "NK-OTHER", "Niên khóa trường khác", null,
            LocalDate.parse("2026-09-01"), LocalDate.parse("2027-06-01"), "ACTIVE", now, now, null, null));

        var page = repository.findGradesBySchoolId(schoolId, null, 1, 20);

        assertThat(page.content()).extracting(g -> g.id()).containsExactly(active);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void should_clamp_page_size_so_a_huge_request_cannot_drain_the_school() {
        joinSchool(student("Pham Thi Dung", "dung"));

        var page = repository.findUsersBySchoolId(schoolId, "STUDENT", null, 1, 100_000);

        assertThat(page.size()).isEqualTo(100);
    }

    // ---------- fixtures ----------

    private static final java.util.concurrent.atomic.AtomicInteger GRADE_LEVEL_ORDER =
        new java.util.concurrent.atomic.AtomicInteger(1000);

    // grade_level_order unique toàn cục -> mỗi lần persist phải lấy số mới.
    private static int nextGradeLevelOrder() {
        return GRADE_LEVEL_ORDER.incrementAndGet();
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private UUID roleId(String code) {
        var existing = em.createQuery(
                "SELECT r.id FROM RoleJpaEntity r WHERE r.code = :code", UUID.class)
            .setParameter("code", code)
            .getResultList();
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        return persisted(new RoleJpaEntity(null, code, code, now, now, null, null)).getId();
    }

    private UUID grade(String code, String status) {
        return persisted(new SchoolGradeJpaEntity(null, schoolId, gradeLevelId, code + "-" + suffix(), code, null,
            LocalDate.parse("2026-09-01"), LocalDate.parse("2027-06-01"), status, now, now, null, null)).getId();
    }

    private UUID schoolClass(String code) {
        // school_classes.created_by và updated_by đều là NOT NULL.
        var actor = UUID.randomUUID();
        return persisted(new SchoolClassJpaEntity(null, schoolId, UUID.randomUUID(), grade("NK", "ACTIVE"),
            code + "-" + suffix(), code, null, "ACTIVE", now, now, actor, actor)).getId();
    }

    private UUID user(String fullName, String local, UUID roleId) {
        var userId = persisted(new UserJpaEntity(null, local + "-" + suffix() + "@test.local", "hash", null,
            fullName, null, LocalDate.parse("2008-01-01"), null, null, "ACTIVE", now, now, null, null)).getId();
        persisted(new UserRoleJpaEntity(null, userId, roleId, now));
        return userId;
    }

    private UUID student(String fullName, String local) {
        return user(fullName, local, studentRoleId);
    }

    private UUID teacher(String fullName, String local) {
        return user(fullName, local, teacherRoleId);
    }

    private void joinSchool(UUID userId) {
        persisted(new SchoolUserJpaEntity(null, schoolId, userId, now, null));
    }

    private void enroll(UUID userId, UUID classId, boolean active) {
        // school_class_users.assigned_by là NOT NULL.
        persisted(new SchoolClassUserJpaEntity(
            null, userId, classId, active, now, active ? null : now, UUID.randomUUID()));
    }
}
