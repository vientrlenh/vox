package com.sep.vox.application.usecase.schoolclassuser;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.BulkCreateSchoolClassUsersCommand;
import com.sep.vox.application.port.input.usecase.schoolclassuser.BulkCreateSchoolClassUsersUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclassuser.BulkCreateSchoolClassUserFailure;
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

class BulkCreateSchoolClassUsersUseCaseTests {

    private SchoolClassUserRepository schoolClassUserRepository;
    private SchoolClassRepository schoolClassRepository;
    private SchoolRepository schoolRepository;
    private UserRepository userRepository;
    private UserContextPort userContextPort;
    private BulkCreateSchoolClassUsersUseCase useCase;

    private UUID currentUserId;
    private UUID schoolId;
    private UUID classId;

    @BeforeEach
    void setUp() {
        schoolClassUserRepository = mock(SchoolClassUserRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        userRepository = mock(UserRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new BulkCreateSchoolClassUsersUseCase(
            schoolClassUserRepository,
            schoolClassRepository,
            schoolRepository,
            userRepository,
            userContextPort,
            TestSchoolUserRepository.create()
        );

        currentUserId = UUID.randomUUID();
        schoolId = UUID.randomUUID();
        classId = UUID.randomUUID();
    }

    @Test
    void bulk_create_should_add_every_valid_user_in_a_single_save() {
        var firstUserId = UUID.randomUUID();
        var secondUserId = UUID.randomUUID();
        mockValidContext(firstUserId, secondUserId);
        mockNoExistingMemberships();
        mockSaveAllEchoesInput();

        var response = useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(firstUserId, secondUserId)));

        assertThat(response.addedUserIds()).containsExactly(firstUserId, secondUserId);
        assertThat(response.failed()).isEmpty();

        var captor = memberships();
        verify(schoolClassUserRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
            .allSatisfy(membership -> {
                assertThat(membership.getSchoolClassId()).isEqualTo(classId);
                assertThat(membership.isActive()).isTrue();
                assertThat(membership.getLeftAt()).isNull();
                assertThat(membership.getJoinedAt()).isNotNull();
                assertThat(membership.getAssignedBy()).isEqualTo(currentUserId);
            });
    }

    @Test
    void bulk_create_should_report_failure_when_user_not_found() {
        var validUserId = UUID.randomUUID();
        var missingUserId = UUID.randomUUID();
        mockValidContext(validUserId);
        mockNoExistingMemberships();
        mockSaveAllEchoesInput();

        var response = useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(validUserId, missingUserId)));

        assertThat(response.addedUserIds()).containsExactly(validUserId);
        assertThat(response.failed())
            .containsExactly(new BulkCreateSchoolClassUserFailure(missingUserId, "Không tìm thấy người dùng"));
    }

    @Test
    void bulk_create_should_report_failure_when_user_is_inactive() {
        var inactiveUserId = UUID.randomUUID();
        mockContext();
        mockTargetUsers(user(inactiveUserId, schoolId, UserStatus.INACTIVE));
        mockNoExistingMemberships();

        var response = useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(inactiveUserId)));

        assertThat(response.addedUserIds()).isEmpty();
        assertThat(response.failed())
            .containsExactly(new BulkCreateSchoolClassUserFailure(inactiveUserId, "Người dùng không hoạt động"));
        verify(schoolClassUserRepository, never()).saveAll(anyCollection());
    }

    @Test
    void bulk_create_should_report_failure_when_user_belongs_to_other_school() {
        var otherSchoolUserId = UUID.randomUUID();
        mockContext();
        mockTargetUsers(activeUser(otherSchoolUserId, UUID.randomUUID()));
        mockNoExistingMemberships();

        var response = useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(otherSchoolUserId)));

        assertThat(response.addedUserIds()).isEmpty();
        assertThat(response.failed())
            .containsExactly(new BulkCreateSchoolClassUserFailure(otherSchoolUserId, "Người dùng không thuộc trường hiện tại"));
    }

    @Test
    void bulk_create_should_report_failure_when_user_already_in_class() {
        var existingUserId = UUID.randomUUID();
        mockValidContext(existingUserId);
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(anyCollection(), anyCollection()))
            .thenReturn(List.of(new SchoolClassUser(existingUserId, classId, true, Instant.now(), null, currentUserId)));

        var response = useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(existingUserId)));

        assertThat(response.addedUserIds()).isEmpty();
        assertThat(response.failed())
            .containsExactly(new BulkCreateSchoolClassUserFailure(existingUserId, "Người dùng đã thuộc lớp học"));
        verify(schoolClassUserRepository, never()).saveAll(anyCollection());
    }

    @Test
    void bulk_create_should_reactivate_membership_of_user_who_left_the_class() {
        var returningUserId = UUID.randomUUID();
        var membershipId = UUID.randomUUID();
        mockValidContext(returningUserId);
        var leftMembership = new SchoolClassUser(
            membershipId, returningUserId, classId, false, Instant.now().minus(5, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS), UUID.randomUUID());
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(anyCollection(), anyCollection()))
            .thenReturn(List.of(leftMembership));
        mockSaveAllEchoesInput();

        var response = useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(returningUserId)));

        assertThat(response.addedUserIds()).containsExactly(returningUserId);
        assertThat(response.failed()).isEmpty();

        var captor = memberships();
        verify(schoolClassUserRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(membership -> {
            assertThat(membership.getId()).isEqualTo(membershipId);
            assertThat(membership.isActive()).isTrue();
            assertThat(membership.getLeftAt()).isNull();
        });
    }

    @Test
    void bulk_create_should_deduplicate_repeated_user_ids() {
        var userId = UUID.randomUUID();
        mockValidContext(userId);
        mockNoExistingMemberships();
        mockSaveAllEchoesInput();

        var response = useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(userId, userId, userId)));

        assertThat(response.addedUserIds()).containsExactly(userId);
        var captor = memberships();
        verify(schoolClassUserRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    void bulk_create_should_throw_duplicated_when_save_hits_unique_violation() {
        var userId = UUID.randomUUID();
        mockValidContext(userId);
        mockNoExistingMemberships();
        when(schoolClassUserRepository.saveAll(anyCollection()))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("unique violation"));

        assertThrows(DuplicatedException.class, () -> useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(userId))));
    }

    @Test
    void bulk_create_should_throw_when_user_ids_are_empty() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of())));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassUserRepository);
    }

    @Test
    void bulk_create_should_throw_when_class_is_not_active() {
        var userId = UUID.randomUUID();
        mockContext();
        var archivedClass = activeSchoolClass(classId, schoolId);
        archivedClass.setStatus(SchoolClassStatus.ARCHIVED);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(archivedClass));

        assertThrows(IllegalStateException.class, () -> useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(userId))));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void bulk_create_should_throw_when_class_belongs_to_other_school() {
        var userId = UUID.randomUUID();
        mockContext();
        when(schoolClassRepository.findById(classId))
            .thenReturn(Optional.of(activeSchoolClass(classId, UUID.randomUUID())));

        assertThrows(NotFoundException.class, () -> useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(userId))));

        verifyNoInteractions(schoolClassUserRepository);
    }

    @Test
    void bulk_create_should_throw_when_requested_school_differs_from_current_user_school() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, UUID.randomUUID())));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
            new BulkCreateSchoolClassUsersCommand(schoolId, classId, List.of(UUID.randomUUID()))));

        verifyNoInteractions(schoolRepository, schoolClassRepository, schoolClassUserRepository);
    }

    private void mockContext() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(activeUser(currentUserId, schoolId)));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId)));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(activeSchoolClass(classId, schoolId)));
    }

    private void mockValidContext(UUID... targetUserIds) {
        mockContext();
        mockTargetUsers(java.util.Arrays.stream(targetUserIds)
            .map(userId -> activeUser(userId, schoolId))
            .toArray(User[]::new));
    }

    private void mockTargetUsers(User... users) {
        when(userRepository.findByIdIn(anyCollection())).thenReturn(List.of(users));
    }

    private void mockNoExistingMemberships() {
        when(schoolClassUserRepository.findByUserIdInAndSchoolClassIdIn(anyCollection(), anyCollection()))
            .thenReturn(List.of());
    }

    private void mockSaveAllEchoesInput() {
        when(schoolClassUserRepository.saveAll(anyCollection()))
            .thenAnswer(invocation -> List.copyOf(invocation.getArgument(0, Collection.class)));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Collection<SchoolClassUser>> memberships() {
        return ArgumentCaptor.forClass((Class<Collection<SchoolClassUser>>) (Class<?>) Collection.class);
    }

    private static User activeUser(UUID id, UUID schoolId) {
        return user(id, schoolId, UserStatus.ACTIVE);
    }

    private static User user(UUID id, UUID schoolId, UserStatus status) {
        var user = new User();
        user.setId(id);
        TestSchoolUserRepository.remember(id, schoolId);
        user.setStatus(status);
        return user;
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
