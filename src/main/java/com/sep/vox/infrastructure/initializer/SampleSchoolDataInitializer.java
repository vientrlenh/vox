package com.sep.vox.infrastructure.initializer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Seeds one school and a small set of school members for local development.
 * The initializer is disabled by default and can safely recover from a partial seed.
 */
@Component
@Order(4)
@ConditionalOnProperty(prefix = "sample-data", name = "enabled", havingValue = "true")
public class SampleSchoolDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleSchoolDataInitializer.class);

    private static final String SAMPLE_SCHOOL_CODE = "SAMPLE01";
    private static final String SCHOOL_ADMIN_ROLE_CODE = "SCHOOL_ADMIN";
    private static final String TEACHER_ROLE_CODE = "TEACHER";
    private static final String STUDENT_ROLE_CODE = "STUDENT";
    private static final String SYSTEM_ADMIN_ROLE_CODE = "SYSTEM_ADMIN";

    private static final List<MemberSeed> MEMBERS = List.of(
        new MemberSeed(
            "admin.sample@vox.edu.vn",
            "0900000001",
            "Sample School Administrator",
            Gender.MALE,
            LocalDate.of(1985, 5, 20),
            SCHOOL_ADMIN_ROLE_CODE,
            null
        ),
        new MemberSeed(
            "teacher1.sample@vox.edu.vn",
            "0900000011",
            "Nguyen Van Teacher",
            Gender.MALE,
            LocalDate.of(1988, 3, 12),
            TEACHER_ROLE_CODE,
            null
        ),
        new MemberSeed(
            "teacher2.sample@vox.edu.vn",
            "0900000012",
            "Tran Thi Teacher",
            Gender.FEMALE,
            LocalDate.of(1990, 7, 8),
            TEACHER_ROLE_CODE,
            null
        ),
        new MemberSeed(
            "student1.sample@vox.edu.vn",
            "0900000021",
            "Le Van Student",
            Gender.MALE,
            LocalDate.of(2008, 1, 15),
            STUDENT_ROLE_CODE,
            3
        ),
        new MemberSeed(
            "student2.sample@vox.edu.vn",
            "0900000022",
            "Pham Thi Student",
            Gender.FEMALE,
            LocalDate.of(2009, 9, 2),
            STUDENT_ROLE_CODE,
            3
        ),
        new MemberSeed(
            "student3.sample@vox.edu.vn",
            "0900000023",
            "Hoang Van Student",
            Gender.MALE,
            LocalDate.of(2008, 11, 30),
            STUDENT_ROLE_CODE,
            3
        )
    );

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final PasswordEncoderPort passwordEncoderPort;

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
    public void run(ApplicationArguments args) {
        var roleIds = new RoleIds(
            requireRoleId(SCHOOL_ADMIN_ROLE_CODE),
            requireRoleId(TEACHER_ROLE_CODE),
            requireRoleId(STUDENT_ROLE_CODE)
        );
        var now = OffsetDateTime.now();
        var auditUserId = resolveSystemAdminId();
        var school = findOrCreateSchool(auditUserId, now);
        var passwordHash = passwordEncoderPort.hash(password);
        var createdUserCount = 0;

        for (var member : MEMBERS) {
            if (ensureMember(member, school.getId(), roleIds.forCode(member.roleCode()), passwordHash,
                    auditUserId, now)) {
                createdUserCount++;
            }
        }

        LOGGER.info(
            "Sample school data initialized successfully for school code {}. Created {} new users and ensured {} memberships",
            SAMPLE_SCHOOL_CODE,
            createdUserCount,
            MEMBERS.size()
        );
    }

    private School findOrCreateSchool(UUID auditUserId, OffsetDateTime now) {
        return schoolRepository.findByCode(SAMPLE_SCHOOL_CODE)
            .orElseGet(() -> schoolRepository.save(School.create(
                SAMPLE_SCHOOL_CODE,
                "Vox Sample High School",
                "Sample school for local development",
                "0900000001",
                "contact.sample@vox.edu.vn",
                "sample.edu.vn",
                "Ha Noi",
                3,
                auditUserId,
                now
            )));
    }

    private boolean ensureMember(
            MemberSeed seed,
            UUID schoolId,
            UUID roleId,
            String passwordHash,
            UUID auditUserId,
            OffsetDateTime now) {
        var existingUser = userRepository.findByEmail(seed.email());
        var user = existingUser.orElseGet(() -> createUser(seed, passwordHash, auditUserId, now));

        ensureRole(user.getId(), roleId, now);
        ensureSchoolMembership(user.getId(), schoolId, now, seed.membershipYears());
        return existingUser.isEmpty();
    }

    private User createUser(MemberSeed seed, String passwordHash, UUID auditUserId, OffsetDateTime now) {
        if (userRepository.existsByPhone(seed.phone())) {
            throw new IllegalStateException(
                "Cannot seed sample user because the phone number is already assigned: " + seed.phone()
            );
        }

        return userRepository.save(new User(
            new Email(seed.email()),
            passwordHash,
            new Phone(seed.phone()),
            new FullName(seed.fullName()),
            seed.gender(),
            new DateOfBirth(seed.dateOfBirth()),
            "Ha Noi",
            null,
            UserStatus.ACTIVE,
            now,
            now,
            auditUserId,
            auditUserId
        ));
    }

    private void ensureRole(UUID userId, UUID roleId, OffsetDateTime now) {
        if (userRoleRepository.findByUserIdAndRoleId(userId, roleId).isEmpty()) {
            userRoleRepository.save(new UserRole(userId, roleId, now));
        }
    }

    private void ensureSchoolMembership(UUID userId, UUID schoolId, OffsetDateTime now, Integer membershipYears) {
        var existingMembership = schoolUserRepository.findByUserId(userId);
        if (existingMembership.isPresent()) {
            if (!existingMembership.get().getSchoolId().equals(schoolId)) {
                throw new IllegalStateException(
                    "Cannot seed sample user because the account already belongs to another school: " + userId
                );
            }
            return;
        }

        var endDate = membershipYears == null ? null : now.plusYears(membershipYears);
        schoolUserRepository.save(SchoolUser.create(userId, schoolId, now, endDate));
    }

    private UUID requireRoleId(String roleCode) {
        return roleRepository.findByCode(roleCode)
            .orElseThrow(() -> new IllegalStateException("Required role is missing: " + roleCode))
            .getId();
    }

    private UUID resolveSystemAdminId() {
        return roleRepository.findByCode(SYSTEM_ADMIN_ROLE_CODE)
            .flatMap(role -> userRoleRepository.findByRoleId(role.getId()).stream().findFirst())
            .map(ur -> ur.getUserId())
            .orElse(null);
    }

    private record MemberSeed(
        String email,
        String phone,
        String fullName,
        Gender gender,
        LocalDate dateOfBirth,
        String roleCode,
        Integer membershipYears
    ) {
    }

    private record RoleIds(UUID schoolAdmin, UUID teacher, UUID student) {

        private UUID forCode(String roleCode) {
            return switch (roleCode) {
                case SCHOOL_ADMIN_ROLE_CODE -> schoolAdmin;
                case TEACHER_ROLE_CODE -> teacher;
                case STUDENT_ROLE_CODE -> student;
                default -> throw new IllegalArgumentException("Unsupported sample role: " + roleCode);
            };
        }
    }
}
