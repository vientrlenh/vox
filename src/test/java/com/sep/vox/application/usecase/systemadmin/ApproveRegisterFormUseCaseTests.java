package com.sep.vox.application.usecase.systemadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.PasswordSetUpEmailRequestedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ApproveRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.registration.ApproveRegisterFormUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.output.GeneratedPasswordSetUpToken;
import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.domain.valueobject.RoleCode;

public class ApproveRegisterFormUseCaseTests {

    private UserRepository userRepository;
    private SchoolRepository schoolRepository;
    private RegisterFormRepository registerFormRepository;
    private RoleRepository roleRepository;
    private UserRoleRepository userRoleRepository;
    private PasswordSetUpTokenPort passwordSetUpTokenPort;
    private PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private UserContextPort userContextPort;
    private EventPublisherPort eventPublisherPort;
    private ApproveRegisterFormUseCase approveRegisterFormUseCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        registerFormRepository = mock(RegisterFormRepository.class);
        roleRepository = mock(RoleRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        passwordSetUpTokenPort = mock(PasswordSetUpTokenPort.class);
        passwordSetUpTokenRepository = mock(PasswordSetUpTokenRepository.class);
        userContextPort = mock(UserContextPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);

        approveRegisterFormUseCase = new ApproveRegisterFormUseCase(
            userRepository,
            schoolRepository,
            registerFormRepository,
            roleRepository,
            userRoleRepository,
            passwordSetUpTokenPort,
            passwordSetUpTokenRepository,
            userContextPort,
            eventPublisherPort
        );
    }

    @Test
    void approve_register_form_should_create_school_admin_password_token_and_publish_email_event() {
        var currentUserId = UUID.randomUUID();
        var registerFormId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var schoolAdminId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var command = validCommand(registerFormId);
        var role = role(roleId, currentUserId);
        var token = new GeneratedPasswordSetUpToken("raw-token", "hashed-token");

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(roleRepository.findByCode("SCHOOL_ADMIN")).thenReturn(Optional.of(role));
        when(registerFormRepository.updateApprovedRegisterForm(eq(registerFormId), eq(currentUserId), any(OffsetDateTime.class)))
            .thenReturn(1);
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> {
            var school = invocation.getArgument(0, School.class);
            school.setId(schoolId);
            return school;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            var user = invocation.getArgument(0, User.class);
            user.setId(schoolAdminId);
            return user;
        });
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordSetUpTokenPort.generateToken()).thenReturn(token);
        when(passwordSetUpTokenRepository.save(any(PasswordSetUpToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = approveRegisterFormUseCase.execute(command);

        assertThat(response).isNull();

        var updatedAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(registerFormRepository).updateApprovedRegisterForm(
            eq(registerFormId),
            eq(currentUserId),
            updatedAtCaptor.capture()
        );
        assertThat(updatedAtCaptor.getValue()).isNotNull();

        var schoolCaptor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(schoolCaptor.capture());
        var savedSchool = schoolCaptor.getValue();
        assertThat(savedSchool.getCode().value()).isEqualTo("VOX_SCHOOL");
        assertThat(savedSchool.getName()).isEqualTo("Vox School");
        assertThat(savedSchool.getDescription()).isEqualTo("AI English evaluation school");
        assertThat(savedSchool.getContactPhone().value()).isEqualTo("0987654321");
        assertThat(savedSchool.getContactEmail().value()).isEqualTo("admin@vox.edu.vn");
        assertThat(savedSchool.getDomain().value()).isEqualTo("vox.edu.vn");
        assertThat(savedSchool.getAddress()).isEqualTo("456 School Street");
        assertThat(savedSchool.getStudentCount().value()).isEqualTo(500);
        assertThat(savedSchool.isActive()).isTrue();
        assertThat(savedSchool.getCreatedBy()).isEqualTo(currentUserId);
        assertThat(savedSchool.getUpdatedBy()).isEqualTo(currentUserId);

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        var savedSchoolAdmin = userCaptor.getValue();
        assertThat(savedSchoolAdmin.getEmail().value()).isEqualTo("admin@vox.edu.vn");
        assertThat(savedSchoolAdmin.getPasswordHash()).isEqualTo("__PASSWORD_NOT_SET__");
        assertThat(savedSchoolAdmin.getPhone().value()).isEqualTo("0987654321");
        assertThat(savedSchoolAdmin.getFullName().value()).isEqualTo("Nguyen Van A");
        assertThat(savedSchoolAdmin.getDateOfBirth().value()).isEqualTo(command.dateOfBirth());
        assertThat(savedSchoolAdmin.getAddress()).isEqualTo("123 Contact Street");
        assertThat(savedSchoolAdmin.getSchoolId()).isEqualTo(schoolId);
        assertThat(savedSchoolAdmin.getCreatedBy()).isEqualTo(currentUserId);
        assertThat(savedSchoolAdmin.getUpdatedBy()).isEqualTo(currentUserId);

        var userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(userRoleCaptor.capture());
        assertThat(userRoleCaptor.getValue().getUserId()).isEqualTo(schoolAdminId);
        assertThat(userRoleCaptor.getValue().getRoleId()).isEqualTo(roleId);

        var passwordTokenCaptor = ArgumentCaptor.forClass(PasswordSetUpToken.class);
        verify(passwordSetUpTokenRepository).save(passwordTokenCaptor.capture());
        var savedPasswordToken = passwordTokenCaptor.getValue();
        assertThat(savedPasswordToken.getUserId()).isEqualTo(schoolAdminId);
        assertThat(savedPasswordToken.getTokenHash()).isEqualTo("hashed-token");
        assertThat(savedPasswordToken.getUsedAt()).isNull();
        assertThat(savedPasswordToken.getExpiredAt()).isAfter(savedPasswordToken.getCreatedAt());

        var eventCaptor = ArgumentCaptor.forClass(PasswordSetUpEmailRequestedEvent.class);
        verify(eventPublisherPort).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new PasswordSetUpEmailRequestedEvent(
            "admin@vox.edu.vn",
            "Nguyen Van A",
            "Vox School",
            schoolAdminId,
            "raw-token"
        ));
    }

    @Test
    void approve_register_form_should_throw_when_school_admin_role_is_not_found() {
        var currentUserId = UUID.randomUUID();
        var command = validCommand(UUID.randomUUID());

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(roleRepository.findByCode("SCHOOL_ADMIN")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> approveRegisterFormUseCase.execute(command));

        verify(roleRepository).findByCode("SCHOOL_ADMIN");
        verify(registerFormRepository, never()).updateApprovedRegisterForm(
            any(UUID.class),
            any(UUID.class),
            any(OffsetDateTime.class)
        );
        verifyNoInteractions(schoolRepository, userRepository, userRoleRepository, passwordSetUpTokenPort,
            passwordSetUpTokenRepository, eventPublisherPort);
    }

    @Test
    void approve_register_form_should_throw_when_register_form_is_not_pending_or_not_found() {
        var currentUserId = UUID.randomUUID();
        var registerFormId = UUID.randomUUID();
        var command = validCommand(registerFormId);
        var role = role(UUID.randomUUID(), currentUserId);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(currentUserId);
        when(roleRepository.findByCode("SCHOOL_ADMIN")).thenReturn(Optional.of(role));
        when(registerFormRepository.updateApprovedRegisterForm(eq(registerFormId), eq(currentUserId), any(OffsetDateTime.class)))
            .thenReturn(0);

        assertThrows(IllegalStateException.class, () -> approveRegisterFormUseCase.execute(command));

        verify(registerFormRepository).updateApprovedRegisterForm(
            eq(registerFormId),
            eq(currentUserId),
            any(OffsetDateTime.class)
        );
        verifyNoInteractions(schoolRepository, userRepository, userRoleRepository, passwordSetUpTokenPort,
            passwordSetUpTokenRepository, eventPublisherPort);
    }

    private ApproveRegisterFormCommand validCommand(UUID registerFormId) {
        return new ApproveRegisterFormCommand(
            registerFormId,
            " vox_school ",
            "  Vox   School  ",
            "  AI   English   evaluation   school  ",
            " 098-765.43 21 ",
            " Admin@Vox.EDU.VN ",
            " VOX.EDU.VN ",
            "  456   School   Street  ",
            500,
            "  Nguyen   Van   A  ",
            LocalDate.of(2000, 5, 24),
            "  123   Contact   Street  "
        );
    }

    private Role role(UUID roleId, UUID currentUserId) {
        var now = OffsetDateTime.now();
        return new Role(
            roleId,
            new RoleCode("SCHOOL_ADMIN"),
            "School Admin",
            now,
            now,
            currentUserId,
            currentUserId
        );
    }
}
