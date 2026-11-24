package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.command.user.UpdateProfileCommand;
import com.sep.vox.application.port.input.usecase.user.UpdateProfileUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewProfileUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUserDetailsUseCase;
import com.sep.vox.application.port.input.usecase.user.ViewUsersUseCase;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends profile operations through the real schema and asserts the typed values: {@code Date} for the date of
 * birth and {@code DateTime} for the audit timestamps, on output and on the PATCH-style
 * {@code UpdateProfileInput}.
 */
@GraphQLControllerTest(GraphQLUserController.class)
class UserGraphQLTests {

    private static final String UPDATE_PROFILE_WITH_VARIABLE =
        "mutation($dateOfBirth: Date) { updateProfile(input: { dateOfBirth: $dateOfBirth }) { id } }";

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewUserDetailsUseCase viewUserDetailsUseCase;
    @MockitoBean
    private ViewUsersUseCase viewUsersUseCase;
    @MockitoBean
    private ViewProfileUseCase viewProfileUseCase;
    @MockitoBean
    private UpdateProfileUseCase updateProfileUseCase;
    @MockitoBean
    private SchoolUserRepository schoolUserRepository;
    @MockitoBean
    private SchoolRepository schoolRepository;

    @Test
    @WithMockUser
    void profile_should_return_date_of_birth_as_date_and_timestamps_as_date_time() {
        when(viewProfileUseCase.execute(any())).thenReturn(user());

        graphQlTester.document("{ profile { dateOfBirth createdAt updatedAt } }").execute()
            .errors().verify()
            .path("profile.dateOfBirth").entity(String.class).isEqualTo("2008-05-01")
            .path("profile.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("profile.updatedAt").entity(String.class).isEqualTo("2026-07-29T09:00:00Z");
    }

    @Test
    @WithMockUser
    void update_profile_should_deliver_a_date_variable_as_a_local_date() {
        when(updateProfileUseCase.execute(any())).thenReturn(user());

        graphQlTester.document(UPDATE_PROFILE_WITH_VARIABLE).variable("dateOfBirth", "2008-05-01").execute()
            .errors().verify();

        UpdateProfileCommand command = capturedCommand();
        assertThat(command.dateOfBirth()).isEqualTo(LocalDate.of(2008, 5, 1));
        assertThat(command.dateOfBirthProvided()).isTrue();
    }

    @Test
    @WithMockUser
    void update_profile_should_tell_an_explicit_null_date_of_birth_from_an_absent_one() {
        when(updateProfileUseCase.execute(any())).thenReturn(user());

        graphQlTester.document("mutation { updateProfile(input: { dateOfBirth: null }) { id } }").execute()
            .errors().verify();
        graphQlTester.document("mutation { updateProfile(input: { fullName: \"Nguyen Van A\" }) { id } }").execute()
            .errors().verify();

        ArgumentCaptor<UpdateProfileCommand> captor = ArgumentCaptor.forClass(UpdateProfileCommand.class);
        verify(updateProfileUseCase, times(2)).execute(captor.capture());
        UpdateProfileCommand explicitNull = captor.getAllValues().get(0);
        UpdateProfileCommand absent = captor.getAllValues().get(1);
        assertThat(explicitNull.dateOfBirth()).isNull();
        assertThat(explicitNull.dateOfBirthProvided()).isTrue();
        assertThat(absent.dateOfBirth()).isNull();
        assertThat(absent.dateOfBirthProvided()).isFalse();
    }

    @Test
    @WithMockUser
    void update_profile_should_tell_an_explicit_null_avatar_from_an_absent_one() {
        when(updateProfileUseCase.execute(any())).thenReturn(user());

        graphQlTester.document("mutation { updateProfile(input: { avatarUrl: null }) { id } }").execute()
            .errors().verify();
        graphQlTester.document("mutation { updateProfile(input: { phone: \"0911000001\" }) { id } }").execute()
            .errors().verify();

        ArgumentCaptor<UpdateProfileCommand> captor = ArgumentCaptor.forClass(UpdateProfileCommand.class);
        verify(updateProfileUseCase, times(2)).execute(captor.capture());
        UpdateProfileCommand explicitNull = captor.getAllValues().get(0);
        UpdateProfileCommand absent = captor.getAllValues().get(1);
        assertThat(explicitNull.avatarUrl()).isNull();
        assertThat(explicitNull.avatarUrlProvided()).isTrue();
        assertThat(absent.avatarUrl()).isNull();
        assertThat(absent.avatarUrlProvided()).isFalse();
    }

    @Test
    @WithMockUser
    void update_profile_should_reject_a_date_of_birth_that_is_not_yyyy_mm_dd() {
        graphQlTester.document(UPDATE_PROFILE_WITH_VARIABLE).variable("dateOfBirth", "01/05/2008").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("dateOfBirth"))
            .verify();

        verifyNoInteractions(updateProfileUseCase);
    }

    private UpdateProfileCommand capturedCommand() {
        ArgumentCaptor<UpdateProfileCommand> captor = ArgumentCaptor.forClass(UpdateProfileCommand.class);
        verify(updateProfileUseCase).execute(captor.capture());
        return captor.getValue();
    }

    private UserDto user() {
        return new UserDto(UUID.randomUUID(), "student@example.com", "0911000001", "Nguyen Van A", null,
            LocalDate.of(2008, 5, 1), null, null, Instant.parse("2026-07-28T02:30:00Z"),
            Instant.parse("2026-07-29T09:00:00Z"));
    }
}
