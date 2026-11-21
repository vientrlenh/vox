package com.sep.vox.infrastructure.initializer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.model.user.Role;
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
        LOGGER.info("Roles initialized successfully");
    }

    private Role studentRole() {
        return Role.builder()
            .code(new RoleCode(STUDENT_CODE))
            .name("Học sinh")
            .build();
    }

    private Role teacherRole() {
        return Role.builder()
            .code(new RoleCode(TEACHER_CODE))
            .name("Giáo viên")
            .build();
    }

    private Role schoolAdminRole() {
        return Role.builder()
            .code(new RoleCode(SCHOOL_ADMIN_CODE))
            .name("Quản trị nhà trường")
            .build();
    }

    private Role systemAdminRole() {
        return Role.builder()
            .code(new RoleCode(SYSTEM_ADMIN_CODE))
            .name("Quản trị hệ thống")
            .build();
    }

}
