package com.sep.vox.application.usecase.schoolclassuser;

import com.sep.vox.application.usecase.TestUserSchoolResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolClassUsersQuery;
import com.sep.vox.application.port.input.usecase.schoolclassuser.ViewSchoolClassUsersUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.ClassCode;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

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
            TestUserSchoolResolver.create()
        );
    }

    @Test
    void execute_should_return_page_with_memberships_without_nested_users() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var user1Id = UUID.randomUUID();
        var user2Id = UUID.randomUUID();
        var user3Id = UUID.randomUUID();
        var now = OffsetDateTime.now();
        var memberships = List.of(
            membership(UUID.randomUUID(), user1Id, classId, true, now),
            membership(UUID.randomUUID(), user2Id, classId, false, now.plusMinutes(1)),
            membership(UUID.randomUUID(), user3Id, classId, true, now.plusMinutes(2))
        );

        mockValidContext(currentUserId, schoolId, classId);
        when(schoolClassUserRepository.findBySchoolClassId(classId)).thenReturn(memberships);

        var result = useCase.execute(new ViewSchoolClassUsersQuery(classId, 1, 2));

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3L);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).userId()).isEqualTo(user1Id);
        assertThat(result.content().get(0).schoolClassId()).isEqualTo(classId);
        assertThat(result.content().get(0).user()).isNull();
        assertThat(result.content().get(1).isActive()).isFalse();
        assertThat(result.content().get(1).userId()).isEqualTo(user2Id);
        assertThat(result.content().get(1).user()).isNull();
    }

    @Test
    void execute_should_keep_membership_user_field_null_for_nested_graphql_resolver() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var memberships = List.of(membership(UUID.randomUUID(), userId, classId, true, OffsetDateTime.now()));

        mockValidContext(currentUserId, schoolId, classId);
        when(schoolClassUserRepository.findBySchoolClassId(classId)).thenReturn(memberships);

        var result = useCase.execute(new ViewSchoolClassUsersQuery(classId, 1, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().userId()).isEqualTo(userId);
        assertThat(result.content().getFirst().user()).isNull();
    }

    @Test
    void execute_should_return_empty_page_when_page_is_out_of_range() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var memberships = List.of(membership(UUID.randomUUID(), UUID.randomUUID(), classId, true, OffsetDateTime.now()));

        mockValidContext(currentUserId, schoolId, classId);
        when(schoolClassUserRepository.findBySchoolClassId(classId)).thenReturn(memberships);

        var result = useCase.execute(new ViewSchoolClassUsersQuery(classId, 2, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void execute_should_throw_when_class_not_found() {
        var currentUserId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var classId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId, "admin@example.com")));
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
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId, "admin@example.com")));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, UUID.randomUUID())));

        assertThrows(NotFoundException.class, () -> useCase.execute(new ViewSchoolClassUsersQuery(classId, 1, 20)));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void execute_should_throw_when_current_user_is_inactive() {
        var currentUserId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user(currentUserId, UUID.randomUUID(), "admin@example.com", UserStatus.INACTIVE)));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new ViewSchoolClassUsersQuery(UUID.randomUUID(), 1, 20)));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassUserRepository);
    }

    @Test
    void execute_should_throw_when_current_user_has_no_school() {
        var currentUserId = UUID.randomUUID();

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, null, "admin@example.com")));

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
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId, "admin@example.com")));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        assertThrows(IllegalStateException.class,
            () -> useCase.execute(new ViewSchoolClassUsersQuery(UUID.randomUUID(), 1, 20)));

        verifyNoInteractions(schoolClassRepository, schoolClassUserRepository);
    }

    private void mockValidContext(UUID currentUserId, UUID schoolId, UUID classId) {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId, "admin@example.com")));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, schoolId)));
    }

    private static SchoolClassUser membership(UUID id, UUID userId, UUID classId, boolean active, OffsetDateTime joinedAt) {
        return new SchoolClassUser(id, userId, classId, active, joinedAt, null, UUID.randomUUID());
    }

    private static User activeUser(UUID id, UUID schoolId, String email) {
        return user(id, schoolId, email, UserStatus.ACTIVE);
    }

    private static User user(UUID id, UUID schoolId, String email, UserStatus status) {
        var now = OffsetDateTime.now();
        TestUserSchoolResolver.remember(id, schoolId);
        return new User(
            id,
            new Email(email),
            "password-hash",
            new Phone("0987654321"),
            new FullName("Test User"),
            null,
            new DateOfBirth(LocalDate.of(2000, 1, 1)),
            "Ho Chi Minh City",
            null,
            status,
            now,
            now,
            null,
            null
        );
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
