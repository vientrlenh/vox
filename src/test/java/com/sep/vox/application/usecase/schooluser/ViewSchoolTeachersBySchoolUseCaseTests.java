package com.sep.vox.application.usecase.schooluser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolTeachersBySchoolQuery;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolTeachersBySchoolUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersBySchoolUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.valueobject.RoleCode;
import com.sep.vox.domain.model.user.UserStatus;

class ViewSchoolTeachersBySchoolUseCaseTests {

    private RoleRepository roleRepository;
    private ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;
    private ViewSchoolTeachersBySchoolUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
        useCase = new ViewSchoolTeachersBySchoolUseCase(roleRepository, viewSchoolUsersBySchoolUseCase);
    }

    @Test
    void execute_should_delegate_with_teacher_role_id() {
        UUID roleId = UUID.randomUUID();
        Role teacherRole = Role.builder()
            .id(roleId)
            .code(new RoleCode(SchoolRoleCodes.TEACHER))
            .name("Teacher")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        PageResult<SchoolUserDto> page = new PageResult<SchoolUserDto>(List.of(), 1, 20, 0, 0);

        when(roleRepository.findByCode(SchoolRoleCodes.TEACHER)).thenReturn(Optional.of(teacherRole));
        when(viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, "an", roleId, UserStatus.ACTIVE)))
            .thenReturn(page);

        PageResult<SchoolUserDto> result = useCase.execute(new ViewSchoolTeachersBySchoolQuery(schoolId, 1, 20, "an", UserStatus.ACTIVE));

        assertThat(result).isEqualTo(page);
        verify(viewSchoolUsersBySchoolUseCase).execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, "an", roleId, UserStatus.ACTIVE));
    }

    @Test
    void execute_should_throw_when_teacher_role_not_found() {
        when(roleRepository.findByCode(SchoolRoleCodes.TEACHER)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> useCase.execute(new ViewSchoolTeachersBySchoolQuery(schoolId, 1, 20, null, null))
        );
    }
}
