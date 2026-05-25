package com.sep.vox.infrastructure.initializer;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.model.role.Role;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.valueobject.RoleCode;

@Component
@Order(value = 1)
public class RoleInitializer implements ApplicationRunner {

    private static final String STUDENT_CODE = "STUDENT";
    private static final String TEACHER_CODE = "TEACHER";
    private static final String SCHOOL_ADMIN_CODE = "SCHOOL_ADMIN";
    private static final String SYSTEM_ADMIN_CODE = "SYSTEM_ADMIN";

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleInitializer.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (roleRepository.count() > 0) {
            LOGGER.info("Role already exists in database. Skip initialize roles");
            return;
        }
        roleRepository.save(studentRole());
        roleRepository.save(teacherRole());
        roleRepository.save(schoolAdminRole());
        roleRepository.save(systemAdminRole());
    }

    private Role studentRole() {
        var initializedAt = OffsetDateTime.now();
        return new Role(
            new RoleCode(STUDENT_CODE), 
            "Học sinh", 
            initializedAt, 
            initializedAt, 
            null, 
            null
        );
    }

    private Role teacherRole() {
        var initializedAt = OffsetDateTime.now();
        return new Role(
            new RoleCode(TEACHER_CODE),
            "Giáo viên",
            initializedAt,
            initializedAt,
            null,
            null
        );
    }

    private Role schoolAdminRole() {
        var initializedAt = OffsetDateTime.now();
        return new Role(
            new RoleCode(SCHOOL_ADMIN_CODE),
            "Quản trị nhà trường",
            initializedAt,
            initializedAt,
            null,
            null
        );
    }

    private Role systemAdminRole() {
        var initializedAt = OffsetDateTime.now();
        return new Role(
            new RoleCode(SYSTEM_ADMIN_CODE), 
            "Quản trị hệ thống", 
            initializedAt, 
            initializedAt, 
            null, 
            null
        );
    }




}
