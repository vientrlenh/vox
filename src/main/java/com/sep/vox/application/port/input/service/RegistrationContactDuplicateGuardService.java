package com.sep.vox.application.port.input.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class RegistrationContactDuplicateGuardService {

    private final RegisterFormRepository registerFormRepository;
    private final UserRepository userRepository;

    /**
     * Rejects a registration whose contact email or phone already has a PENDING register form or already belongs
     * to a user.
     *
     * @throws ResourceDuplicatedException when either contact value is already in use
     */
    public void requireContactNotInUse(String contactEmail, String contactPhone) {
        boolean formAlreadyPendingByContactInfo =
            registerFormRepository.existsByContactEmailAndStatus(contactEmail, RegisterFormStatus.PENDING)
                || registerFormRepository.existsByContactPhoneAndStatus(contactPhone, RegisterFormStatus.PENDING);

        if (formAlreadyPendingByContactInfo) {
            throw new ResourceDuplicatedException("The submitted information already has a registration awaiting approval");
        }

        boolean alreadyUser = userRepository.existsByEmail(contactEmail) || userRepository.existsByPhone(contactPhone);

        if (alreadyUser) {
            throw new ResourceDuplicatedException("The requested email or phone number is already registered");
        }
    }
}
