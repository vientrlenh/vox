package com.sep.vox.application.port.input.usecase.auth;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.RegisterCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.auth.RegisterResponse;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormRepository;


@Service
public class RegisterUseCase implements IUseCase<RegisterCommand, RegisterResponse> {

    private final RegisterFormRepository registerFormRepository;

    public RegisterUseCase(RegisterFormRepository registerFormRepository) {
        this.registerFormRepository = registerFormRepository;
    }

    @Override
    @Transactional
    public RegisterResponse execute(RegisterCommand input) {
        var command = normalize(input);

        final var ageThresHold = 18;
        var today = LocalDate.now().getYear();
        var ageFromDateOfBirth = today - input.dateOfBirth().getYear();
        if (ageFromDateOfBirth < ageThresHold) {
            throw new IllegalArgumentException("Bạn chưa đủ tuổi để thực hiện việc đăng ký vào hệ thống");
        }
        var now = OffsetDateTime.now();
        var newRegisterForm = new RegisterForm(
            command.contactFullName(), 
            command.identityNumber(), 
            command.contactPhone(), 
            command.contactEmail(),
            command.dateOfBirth(),
            command.contactAddress(), 
            command.schoolDomain(), 
            command.schoolName(), 
            command.schoolAddress(), 
            command.postalCode(), 
            command.position(), 
            command.studentCount(), 
            null, 
            RegisterFormStatus.PENDING, 
            now, 
            now, 
            null
        );
        registerFormRepository.save(newRegisterForm);
        return null;
    }
    
    private RegisterCommand normalize(RegisterCommand input) {
        return new RegisterCommand(
            StringNormalization.trimAndCollapseSpaces(input.contactFullName()),
            StringNormalization.normalizeIdentityNumber(input.identityNumber()),
            StringNormalization.normalizePhone(input.contactPhone()),
            StringNormalization.normalizeEmail(input.contactEmail()),
            input.dateOfBirth(),
            StringNormalization.trimAndCollapseSpaces(input.contactAddress()),
            StringNormalization.normalizeDomain(input.schoolDomain()),
            StringNormalization.trimAndCollapseSpaces(input.schoolName()),
            StringNormalization.trimAndCollapseSpaces(input.schoolAddress()),
            StringNormalization.normalizeIdentityNumber(input.postalCode()),
            StringNormalization.trimAndCollapseSpaces(input.position()),
            input.studentCount()
        );
    } 
}
