package com.sep.vox.infrastructure.initializer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.Gender;
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

/**
 * Seed dữ liệu mẫu cho việc test thủ công: 1 trường + 1 school admin + giáo viên + học sinh,
 * tất cả đều có dòng school_users (kể cả giáo viên). Chỉ chạy khi bật cờ {@code sample-data.enabled=true}.
 * Idempotent: bỏ qua nếu trường mẫu đã tồn tại. Để reseed: xóa trường mẫu (hoặc drop schema) rồi chạy lại.
 */
@Component
@Order(value = 3)
public class SampleSchoolDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleSchoolDataInitializer.class);
    private static final String SAMPLE_SCHOOL_CODE = "SAMPLE01";

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    @Value("${sample-data.enabled:false}")
    private boolean enabled;

    @Value("${sample-data.password:Password@123}")
    private String password;

    public SampleSchoolDataInitializer(
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            SchoolUserRepository schoolUserRepository,
            PasswordEncoderPort passwordEncoderPort) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }
        if (schoolRepository.existsByCode(SAMPLE_SCHOOL_CODE)) {
            LOGGER.info("Sample school data already exists. Skip seeding sample data");
            return;
        }
        var schoolAdminRole = roleRepository.findByCode("SCHOOL_ADMIN").orElse(null);
        var teacherRole = roleRepository.findByCode("TEACHER").orElse(null);
        var studentRole = roleRepository.findByCode("STUDENT").orElse(null);
        if (schoolAdminRole == null || teacherRole == null || studentRole == null) {
            LOGGER.info("Roles not initialized yet. Skip seeding sample data");
            return;
        }

        var now = OffsetDateTime.now();
        var hashedPassword = passwordEncoderPort.hash(password);

        var school = schoolRepository.save(School.create(
            SAMPLE_SCHOOL_CODE, "Trường THPT Mẫu", "Trường mẫu dùng để kiểm thử",
            "0900000001", "admin.sample@vox.edu.vn", null, "Hà Nội", 0, null, now));
        var schoolId = school.getId();

        // School admin
        var admin = saveMember("admin.sample@vox.edu.vn", "0900000001", "Quản Trị Mẫu",
            Gender.MALE, LocalDate.of(1985, 5, 20), hashedPassword, now, schoolAdminRole.getId(), schoolId, now, null);

        // Giáo viên (school_users không thời hạn)
        saveMember("teacher1.sample@vox.edu.vn", "0900000011", "Nguyễn Văn Giáo",
            Gender.MALE, LocalDate.of(1988, 3, 12), hashedPassword, now, teacherRole.getId(), schoolId, now, null);
        saveMember("teacher2.sample@vox.edu.vn", "0900000012", "Trần Thị Viên",
            Gender.FEMALE, LocalDate.of(1990, 7, 8), hashedPassword, now, teacherRole.getId(), schoolId, now, null);

        // Học sinh (school_users có thời hạn)
        var endDate = now.plusYears(3);
        saveMember("student1.sample@vox.edu.vn", "0900000021", "Lê Văn Học",
            Gender.MALE, LocalDate.of(2008, 1, 15), hashedPassword, now, studentRole.getId(), schoolId, now, endDate);
        saveMember("student2.sample@vox.edu.vn", "0900000022", "Phạm Thị Sinh",
            Gender.FEMALE, LocalDate.of(2009, 9, 2), hashedPassword, now, studentRole.getId(), schoolId, now, endDate);
        saveMember("student3.sample@vox.edu.vn", "0900000023", "Hoàng Văn Trò",
            Gender.MALE, LocalDate.of(2008, 11, 30), hashedPassword, now, studentRole.getId(), schoolId, now, endDate);

        LOGGER.info("Sample school data seeded successfully (school code {}, admin {}). Default password: {}",
            SAMPLE_SCHOOL_CODE, admin.getEmail().value(), password);
    }

    private User saveMember(String email, String phone, String fullName, Gender gender, LocalDate dateOfBirth,
            String hashedPassword, OffsetDateTime now, UUID roleId, UUID schoolId,
            OffsetDateTime startDate, OffsetDateTime endDate) {
        var user = new User(
            new Email(email), hashedPassword, new Phone(phone), new FullName(fullName), gender,
            new DateOfBirth(dateOfBirth), null, null, UserStatus.ACTIVE, now, now, null, null);
        var savedUser = userRepository.save(user);
        userRoleRepository.save(new UserRole(savedUser.getId(), roleId, now));
        schoolUserRepository.save(SchoolUser.create(savedUser.getId(), schoolId, startDate, endDate));
        return savedUser;
    }
}
