package com.sep.vox.application.port.input.usecase.auth;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.RegisterCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.IdentityNumber;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.PostalCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;


@Service
public class RegisterUseCase implements IUseCase<RegisterCommand, Void> {

    private final RegisterFormRepository registerFormRepository;

    public RegisterUseCase(RegisterFormRepository registerFormRepository) {
        this.registerFormRepository = registerFormRepository;
    }

    @Override
    @Transactional
    public Void execute(RegisterCommand input) {
        var command = normalize(input);

        final var ageThresHold = 18;
        var today = LocalDate.now().getYear();
        var ageFromDateOfBirth = today - input.dateOfBirth().getYear();
        if (ageFromDateOfBirth < ageThresHold) {
            throw new IllegalArgumentException("Bạn chưa đủ tuổi để thực hiện việc đăng ký vào hệ thống");
        }
        var now = OffsetDateTime.now();
        var newRegisterForm = new RegisterForm(
            new FullName(command.contactFullName()), 
            new IdentityNumber(command.identityNumber()), 
            new Phone(command.contactPhone()), 
            new Email(command.contactEmail()),
            command.dateOfBirth(),
            command.contactAddress(), 
            new SchoolDomain(command.schoolDomain()), 
            command.schoolName(), 
            command.schoolAddress(), 
            new PostalCode(command.postalCode()), 
            command.position(), 
            new StudentCount(command.studentCount()), 
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
