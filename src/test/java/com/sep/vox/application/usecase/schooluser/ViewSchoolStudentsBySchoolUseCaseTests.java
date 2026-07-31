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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolStudentsBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolStudentsBySchoolUseCase;
import com.sep.vox.application.port.input.usecase.schooluser.ViewSchoolUsersBySchoolUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.valueobject.RoleCode;

class ViewSchoolStudentsBySchoolUseCaseTests {

    private RoleRepository roleRepository;
    private ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;
    private ViewSchoolStudentsBySchoolUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        viewSchoolUsersBySchoolUseCase = mock(ViewSchoolUsersBySchoolUseCase.class);
        useCase = new ViewSchoolStudentsBySchoolUseCase(roleRepository, viewSchoolUsersBySchoolUseCase);
    }

    @Test
    void execute_should_delegate_with_student_role_id() {
        var roleId = UUID.randomUUID();
        var studentRole = new Role(roleId, new RoleCode(SchoolRoleCodes.STUDENT), "Student", Instant.now(), Instant.now(), null, null);
        var page = new PageResult<SchoolUserDto>(List.of(), 1, 20, 0, 0);

        when(roleRepository.findByCode(SchoolRoleCodes.STUDENT)).thenReturn(Optional.of(studentRole));
        when(viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, "an", roleId, "ACTIVE")))
            .thenReturn(page);

        var result = useCase.execute(new ViewSchoolStudentsBySchoolQuery(schoolId, 1, 20, "an", "ACTIVE"));

        assertThat(result).isEqualTo(page);
        verify(viewSchoolUsersBySchoolUseCase).execute(new ViewSchoolUsersBySchoolQuery(schoolId, 1, 20, "an", roleId, "ACTIVE"));
    }

    @Test
    void execute_should_throw_when_student_role_not_found() {
        when(roleRepository.findByCode(SchoolRoleCodes.STUDENT)).thenReturn(Optional.empty());

        assertThrows(
            NotFoundException.class,
            () -> useCase.execute(new ViewSchoolStudentsBySchoolQuery(schoolId, 1, 20, null, null))
        );
    }
}
