package com.sep.vox.application.usecase.schoolclassuser;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;

class ViewSchoolClassUsersUseCaseTests {

    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private UserContextPort userContextPort;
    private ViewSchoolClassUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSchoolClassUsersUseCase(
            schoolClassUserRepository,
            schoolClassRepository,
            userRepository,
            schoolRepository,
            userContextPort,
            TestSchoolUserRepository.create()
        );
    }


    @Test
    void execute_should_throw_when_class_not_found() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        TestSchoolUserRepository.remember(currentUserId, schoolId);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(new ViewSchoolClassUsersQuery(classId, 1, 20)));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void execute_should_throw_when_class_belongs_to_other_school() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        TestSchoolUserRepository.remember(currentUserId, schoolId);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, UUID.randomUUID())));

        assertThrows(NotFoundException.class, () -> useCase.execute(new ViewSchoolClassUsersQuery(classId, 1, 20)));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void execute_should_throw_when_current_user_is_inactive() {
        var currentUserId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(false);

        assertThrows(UnauthorizedException.class,
            () -> useCase.execute(new ViewSchoolClassUsersQuery(UUID.randomUUID(), 1, 20)));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassUserRepository);
    }

    @Test
    void execute_should_throw_when_current_user_has_no_school() {
        var currentUserId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new ViewSchoolClassUsersQuery(UUID.randomUUID(), 1, 20)));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassUserRepository);
    }

    @Test
    void execute_should_throw_when_school_is_inactive() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var school = activeSchool(schoolId);
        school.setActive(false);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)).thenReturn(true);
        TestSchoolUserRepository.remember(currentUserId, schoolId);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new ViewSchoolClassUsersQuery(UUID.randomUUID(), 1, 20)));

        verifyNoInteractions(schoolClassRepository, schoolClassUserRepository);
    }


    private static School activeSchool(UUID id) {
        var school = new School();
        school.setId(id);
        school.setActive(true);
        return school;
    }

    private static SchoolClass activeSchoolClass(UUID id, UUID schoolId) {
        var schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setCode(new ClassCode("ENG-01"));
        schoolClass.setName("English 01");
        schoolClass.setStatus(SchoolClassStatus.ACTIVE);
        return schoolClass;
    }
}
