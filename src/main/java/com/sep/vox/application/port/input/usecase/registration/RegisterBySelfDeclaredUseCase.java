package com.sep.vox.application.port.input.usecase.registration;

import java.time.Instant;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.registration.RegisterBySelfDeclaredCommand;
import com.sep.vox.application.port.input.service.RegistrationContactDuplicateGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.domain.repository.RegisterFormDocumentRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;

@Service
@RequiredArgsConstructor
public class RegisterBySelfDeclaredUseCase implements IUseCase<RegisterBySelfDeclaredCommand, Void> {

    private final RegisterFormRepository registerFormRepository;
    private final RegisterFormDocumentRepository registerFormDocumentRepository;
    private final RegistrationContactDuplicateGuardService registrationContactDuplicateGuardService;

    @Override
    @Transactional
    public Void execute(RegisterBySelfDeclaredCommand input) {
        RegisterBySelfDeclaredCommand command = normalize(input);

        rejectDuplicateOrIncompleteRegistration(command);

        Instant now = Instant.now();
        RegisterForm registerForm = toSelfDeclaredForm(command, now);

        RegisterForm savedForm = registerFormRepository.save(registerForm);
        List<RegisterFormDocument> documents = RegisterFormDocument.createMany(savedForm.getId(), command.documentUrls(), now);
        registerFormDocumentRepository.saveAll(documents);
        return null;
    }

    private RegisterForm toSelfDeclaredForm(RegisterBySelfDeclaredCommand command, Instant now) {
        return RegisterForm.selfDeclared(
            command.schoolName(),
            command.schoolDomain(),
            command.schoolAddress(),
            command.schoolProvince(),
            command.schoolDistrict(),
            command.contactFullName(),
            command.identityNumber(),
            command.contactEmail(),
            command.contactPhone(),
            command.dateOfBirth(),
            command.contactAddress(),
            command.postalCode(),
            command.position(),
            command.studentCount(),
            now
        );
    }

    private void rejectDuplicateOrIncompleteRegistration(RegisterBySelfDeclaredCommand command) {
        // Do NOT block by domain any more: several campuses of one school sharing a single domain is normal
        // (e.g. Pho Thong Nang Khieu high school has 2 campuses on ptnk.edu.vn), and each campus is an independent School.
        // Blocking by domain would make the campus that applies later unable to register for good -- the old
        // blockingFormStatuses also counted APPROVED, so there was no expiry to wait for. The real identity of a
        // school is the SCHOOL CODE, and ProvisionSchoolService still guards the code against duplicates.
        registrationContactDuplicateGuardService.requireContactNotInUse(command.contactEmail(), command.contactPhone());

        if (command.documentUrls() == null || command.documentUrls().isEmpty()) {
            throw new IllegalArgumentException("A verification document is required");
        }
    }

    private RegisterBySelfDeclaredCommand normalize(RegisterBySelfDeclaredCommand input) {
        return new RegisterBySelfDeclaredCommand(
            StringNormalization.trimAndCollapseSpaces(input.schoolName()), 
            StringNormalization.normalizeDomain(input.schoolDomain()), 
            StringNormalization.trimAndCollapseSpaces(input.schoolDistrict()), 
            StringNormalization.trimAndCollapseSpaces(input.schoolProvince()), 
            StringNormalization.trimAndCollapseSpaces(input.schoolAddress()), 
            StringNormalization.trimAndCollapseSpaces(input.contactFullName()), 
            StringNormalization.normalizeIdentityNumber(input.identityNumber()), 
            StringNormalization.normalizePhone(input.contactPhone()), 
            StringNormalization.normalizeEmail(input.contactEmail()), 
            input.dateOfBirth(), 
            StringNormalization.trimAndCollapseSpaces(input.contactAddress()), 
            StringNormalization.trimAndCollapseSpaces(input.postalCode()), 
            StringNormalization.trimAndCollapseSpaces(input.position()), 
            input.studentCount(), 
            input.documentUrls() == null ? null : input.documentUrls().stream()
                .map(StringNormalization::trimAndCollapseSpaces)
                .toList()
        );
    }
    
}
