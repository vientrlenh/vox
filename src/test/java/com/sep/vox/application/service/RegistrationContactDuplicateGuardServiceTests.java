package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.service.RegistrationContactDuplicateGuardService;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationContactDuplicateGuardServiceTests {

    private static final String EMAIL = "admin@school.edu.vn";
    private static final String PHONE = "0900000000";

    @Mock
    private RegisterFormRepository registerFormRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private RegistrationContactDuplicateGuardService guard;

    @Test
    void rejectsWhenPendingFormUsesTheEmail() {
        given(registerFormRepository.existsByContactEmailAndStatus(EMAIL, RegisterFormStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> guard.requireContactNotInUse(EMAIL, PHONE))
            .isInstanceOf(ResourceDuplicatedException.class);
    }

    @Test
    void rejectsWhenPendingFormUsesThePhone() {
        given(registerFormRepository.existsByContactPhoneAndStatus(PHONE, RegisterFormStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> guard.requireContactNotInUse(EMAIL, PHONE))
            .isInstanceOf(ResourceDuplicatedException.class);
    }

    @Test
    void rejectsWhenUserAlreadyHasThePhone() {
        given(userRepository.existsByPhone(PHONE)).willReturn(true);

        assertThatThrownBy(() -> guard.requireContactNotInUse(EMAIL, PHONE))
            .isInstanceOf(ResourceDuplicatedException.class);
    }

    @Test
    void acceptsWhenContactIsFree() {
        assertThatCode(() -> guard.requireContactNotInUse(EMAIL, PHONE)).doesNotThrowAnyException();
    }
}
