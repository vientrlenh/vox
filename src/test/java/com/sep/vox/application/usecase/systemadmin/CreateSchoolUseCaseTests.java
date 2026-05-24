package com.sep.vox.application.usecase.systemadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.command.CreateSchoolCommand;
import com.sep.vox.application.port.input.usecase.systemadmin.CreateSchoolUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.repository.SchoolRepository;

public class CreateSchoolUseCaseTests {

    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private CreateSchoolUseCase createSchoolUseCase;

    @BeforeEach
    void setUp() {
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        createSchoolUseCase = new CreateSchoolUseCase(schoolRepository, userContextPort);
    }

    @Test
    void create_school_should_save_active_school_when_command_is_valid() {
        var userId = UUID.randomUUID();
        var command = validCommand();
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = createSchoolUseCase.execute(command);

        var captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());

        var savedSchool = captor.getValue();
        assertThat(response).isNull();
        assertThat(savedSchool.getCode().value()).isEqualTo("SCH-001");
        assertThat(savedSchool.getName()).isEqualTo("Vox International School");
        assertThat(savedSchool.getDescription()).isEqualTo("A modern bilingual school");
        assertThat(savedSchool.getContactPhone().value()).isEqualTo("0987654321");
        assertThat(savedSchool.getContactEmail().value()).isEqualTo("admin@school.edu.vn");
        assertThat(savedSchool.getDomain().value()).isEqualTo("school.edu.vn");
        assertThat(savedSchool.getAddress()).isEqualTo("123 Education Street");
        assertThat(savedSchool.getStudentCount().value()).isEqualTo(500);
        assertThat(savedSchool.isActive()).isTrue();
        assertThat(savedSchool.getCreatedAt()).isNotNull();
        assertThat(savedSchool.getUpdatedAt()).isNotNull();
        assertThat(savedSchool.getCreatedBy()).isEqualTo(userId);
        assertThat(savedSchool.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    void create_school_should_reject_when_student_count_is_not_positive() {
        var userId = UUID.randomUUID();
        var command = new CreateSchoolCommand(
            "SCH-001",
            "Vox International School",
            "A modern bilingual school",
            "0987654321",
            "admin@school.edu.vn",
            "school.edu.vn",
            "123 Education Street",
            0
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);

        assertThrows(IllegalArgumentException.class, () -> createSchoolUseCase.execute(command));

        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void create_school_should_reject_when_domain_is_not_education_domain() {
        var userId = UUID.randomUUID();
        var command = new CreateSchoolCommand(
            "SCH-001",
            "Vox International School",
            "A modern bilingual school",
            "0987654321",
            "admin@school.edu.vn",
            "school.com",
            "123 Education Street",
            500
        );
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);

        assertThrows(IllegalArgumentException.class, () -> createSchoolUseCase.execute(command));

        verify(schoolRepository, never()).save(any(School.class));
    }

    private CreateSchoolCommand validCommand() {
        return new CreateSchoolCommand(
            " sch-001 ",
            "  Vox   International   School  ",
            "  A   modern   bilingual   school  ",
            " 098-765.43 21 ",
            " Admin@School.EDU.VN ",
            " SCHOOL.EDU.VN ",
            "  123   Education   Street  ",
            500
        );
    }
}
