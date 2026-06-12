package com.sep.vox.infrastructure.initializer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.Gender;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

@Component
@Order(value = 3)
public class SeedDataInitializer implements ApplicationRunner {

    private static final String SEED_PASSWORD = "Abc@1234";
    private static final UUID DEFAULT_SCHOOL_CREATED_BY_ID = UUID.fromString("019ea126-32d2-74e9-bdfd-212d74abbd5c");
    private static final Logger LOGGER = LoggerFactory.getLogger(SeedDataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    public SeedDataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            PasswordEncoderPort passwordEncoderPort
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.existsByEmail("admin1@vox.local")) {
            LOGGER.info("Seed data already exists. Skip seeding");
            return;
        }

        var schoolAdminRole = roleRepository.findByCode("SCHOOL_ADMIN").orElse(null);
        var teacherRole = roleRepository.findByCode("TEACHER").orElse(null);
        var studentRole = roleRepository.findByCode("STUDENT").orElse(null);

        if (schoolAdminRole == null || teacherRole == null || studentRole == null) {
            LOGGER.info("Roles not found. Skip seeding");
            return;
        }

        var hashedPassword = passwordEncoderPort.hash(SEED_PASSWORD);

        // Create 3 schools
        var now = OffsetDateTime.now();
        var schoolCreatedBy = defaultIfNull(null);
        var school1 = schoolRepository.save(School.create(
                "LQD", "THPT Lê Quý Đôn", "Trường THPT Lê Quý Đôn - TP.HCM",
                "0904000001", "contact@lequydon.edu.vn", "lequydon.edu.vn",
                "123 Lê Quý Đôn, Quận 3, TP.HCM", 1200, schoolCreatedBy, now
        ));
        var school2 = schoolRepository.save(School.create(
                "NHH", "THPT Nguyễn Huệ", "Trường THPT Nguyễn Huệ - TP.HCM",
                "0904000002", "contact@nguyenhue.edu.vn", "nguyenhue.edu.vn",
                "456 Nguyễn Huệ, Quận 1, TP.HCM", 1000, schoolCreatedBy, now
        ));
        var school3 = schoolRepository.save(School.create(
                "THD", "THPT Trần Hưng Đạo", "Trường THPT Trần Hưng Đạo - TP.HCM",
                "0904000003", "contact@tranhungdao.edu.vn", "tranhungdao.edu.vn",
                "789 Trần Hưng Đạo, Quận 5, TP.HCM", 800, schoolCreatedBy, now
        ));

        // 3 School Admins (1 per school, ACTIVE for immediate login)
        var admin1 = saveUser("admin1@vox.local", hashedPassword, "0901000001",
                "Nguyễn Văn Admin", Gender.MALE, LocalDate.of(1985, 1, 15),
                "TP.HCM", UserStatus.ACTIVE, null, now);
        saveSchoolUser(admin1.getId(), school1.getId(), now);
        assignRole(admin1, schoolAdminRole, now);
        admin1 = linkAdminOwnership(admin1, school1, now);

        var admin2 = saveUser("admin2@vox.local", hashedPassword, "0901000002",
                "Trần Thị Quản", Gender.FEMALE, LocalDate.of(1988, 3, 20),
                "TP.HCM", UserStatus.ACTIVE, null, now);
        saveSchoolUser(admin2.getId(), school2.getId(), now);
        assignRole(admin2, schoolAdminRole, now);
        admin2 = linkAdminOwnership(admin2, school2, now);

        var admin3 = saveUser("admin3@vox.local", hashedPassword, "0901000003",
                "Lê Văn Trị", Gender.MALE, LocalDate.of(1982, 7, 10),
                "TP.HCM", UserStatus.ACTIVE, null, now);
        saveSchoolUser(admin3.getId(), school3.getId(), now);
        assignRole(admin3, schoolAdminRole, now);
        admin3 = linkAdminOwnership(admin3, school3, now);

        // 10 Teachers
        var teachers = List.of(
                teacherData("teacher1@vox.local", "0902000001", "Phạm Minh Tuấn", Gender.MALE, LocalDate.of(1990, 2, 14), school1.getId()),
                teacherData("teacher2@vox.local", "0902000002", "Hoàng Thị Lan", Gender.FEMALE, LocalDate.of(1992, 5, 22), school1.getId()),
                teacherData("teacher3@vox.local", "0902000003", "Vũ Đức Phong", Gender.MALE, LocalDate.of(1991, 8, 30), school1.getId()),
                teacherData("teacher4@vox.local", "0902000004", "Đỗ Thị Hằng", Gender.FEMALE, LocalDate.of(1993, 1, 8), school2.getId()),
                teacherData("teacher5@vox.local", "0902000005", "Bùi Văn Khang", Gender.MALE, LocalDate.of(1989, 11, 17), school2.getId()),
                teacherData("teacher6@vox.local", "0902000006", "Ngô Thanh Thảo", Gender.FEMALE, LocalDate.of(1994, 4, 5), school2.getId()),
                teacherData("teacher7@vox.local", "0902000007", "Đinh Quang Hải", Gender.MALE, LocalDate.of(1990, 9, 12), school3.getId()),
                teacherData("teacher8@vox.local", "0902000008", "Lý Phương Mai", Gender.FEMALE, LocalDate.of(1995, 6, 28), school3.getId()),
                teacherData("teacher9@vox.local", "0902000009", "Trịnh Văn Dũng", Gender.MALE, LocalDate.of(1987, 12, 3), school3.getId()),
                teacherData("teacher10@vox.local", "0902000010", "Hồ Thị Bích", Gender.FEMALE, LocalDate.of(1996, 3, 19), school3.getId())
        );
        for (var t : teachers) {
            var user = saveUser(t.email, hashedPassword, t.phone, t.fullName, t.gender, t.dob, "TP.HCM", UserStatus.ACTIVE, creatorIdForSchool(t.schoolId, school1, admin1, school2, admin2, school3, admin3), now);
            saveSchoolUser(user.getId(), t.schoolId, now);
            assignRole(user, teacherRole, now);
        }

        // 7 Students
        var students = List.of(
                studentData("student1@vox.local", "0903000001", "Nguyễn Gia Bảo", Gender.MALE, LocalDate.of(2008, 4, 10), school1.getId()),
                studentData("student2@vox.local", "0903000002", "Trần Thị Cẩm Tú", Gender.FEMALE, LocalDate.of(2008, 9, 25), school1.getId()),
                studentData("student3@vox.local", "0903000003", "Lê Hoàng Nam", Gender.MALE, LocalDate.of(2007, 1, 15), school2.getId()),
                studentData("student4@vox.local", "0903000004", "Phạm Thị Thuỳ Linh", Gender.FEMALE, LocalDate.of(2008, 7, 7), school2.getId()),
                studentData("student5@vox.local", "0903000005", "Huỳnh Tấn Phát", Gender.MALE, LocalDate.of(2007, 11, 30), school2.getId()),
                studentData("student6@vox.local", "0903000006", "Võ Thị Kim Ngân", Gender.FEMALE, LocalDate.of(2008, 2, 18), school3.getId()),
                studentData("student7@vox.local", "0903000007", "Đặng Minh Khoa", Gender.MALE, LocalDate.of(2007, 6, 12), school3.getId())
        );
        for (var s : students) {
            var user = saveUser(s.email, hashedPassword, s.phone, s.fullName, s.gender, s.dob, "TP.HCM", UserStatus.ACTIVE, creatorIdForSchool(s.schoolId, school1, admin1, school2, admin2, school3, admin3), now);
            saveSchoolUser(user.getId(), s.schoolId, now);
            assignRole(user, studentRole, now);
        }

        LOGGER.info("Seed data initialized successfully: 3 schools, 3 admins, 10 teachers, 7 students");
    }

    private User saveUser(String email, String passwordHash, String phone, String fullName,
                          Gender gender, LocalDate dob, String address, UserStatus status,
                          UUID createdBy, OffsetDateTime now) {
        return userRepository.save(new User(
                new Email(email), passwordHash, new Phone(phone), new FullName(fullName),
                gender, new DateOfBirth(dob), address, null, status,
                now, now, createdBy, createdBy
        ));
    }

    private void assignRole(User user, Role role, OffsetDateTime now) {
        userRoleRepository.save(new UserRole(user.getId(), role.getId(), now));
    }

    private UUID defaultIfNull(UUID value) {
        return value == null ? DEFAULT_SCHOOL_CREATED_BY_ID : value;
    }

    private void saveSchoolUser(UUID userId, UUID schoolId, OffsetDateTime now) {
        schoolUserRepository.save(SchoolUser.create(userId, schoolId, now, null));
    }

    private User linkAdminOwnership(User admin, School school, OffsetDateTime now) {
        admin.setCreatedBy(admin.getId());
        admin.setUpdatedBy(admin.getId());
        admin.setUpdatedAt(now);
        var savedAdmin = userRepository.save(admin);

        school.setCreatedBy(savedAdmin.getId());
        school.setUpdatedBy(savedAdmin.getId());
        school.setUpdatedAt(now);
        schoolRepository.save(school);

        return savedAdmin;
    }

    private UUID creatorIdForSchool(UUID schoolId, School school1, User admin1, School school2, User admin2, School school3, User admin3) {
        if (school1.getId().equals(schoolId)) {
            return admin1.getId();
        }
        if (school2.getId().equals(schoolId)) {
            return admin2.getId();
        }
        if (school3.getId().equals(schoolId)) {
            return admin3.getId();
        }
        return DEFAULT_SCHOOL_CREATED_BY_ID;
    }

    private TeacherData teacherData(String email, String phone, String fullName, Gender gender, LocalDate dob, UUID schoolId) {
        return new TeacherData(email, phone, fullName, gender, dob, schoolId);
    }

    private StudentData studentData(String email, String phone, String fullName, Gender gender, LocalDate dob, UUID schoolId) {
        return new StudentData(email, phone, fullName, gender, dob, schoolId);
    }

    private record TeacherData(String email, String phone, String fullName, Gender gender, LocalDate dob, UUID schoolId) {}
    private record StudentData(String email, String phone, String fullName, Gender gender, LocalDate dob, UUID schoolId) {}
}
