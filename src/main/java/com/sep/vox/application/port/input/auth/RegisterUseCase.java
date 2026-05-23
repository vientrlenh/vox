package com.sep.vox.application.port.input.auth;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.command.RegisterCommand;
import com.sep.vox.application.port.input.IUseCase;
import com.sep.vox.application.response.RegisterResponse;
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
        final var ageThresHold = 18;
        var today = LocalDate.now().getYear();
        var ageFromDateOfBirth = today - input.dateOfBirth().getYear();
        if (ageFromDateOfBirth < ageThresHold) {
            throw new IllegalArgumentException("Bạn chưa đủ tuổi để thực hiện việc đăng ký vào hệ thống");
        }
        var now = OffsetDateTime.now();
        var newRegisterForm = new RegisterForm(
            input.contactFullName(), 
            input.identityNumber(), 
            input.contactPhone(), 
            input.contactEmail(),
            input.dateOfBirth(),
            input.contactAddress(), 
            input.schoolDomain(), 
            input.schoolName(), 
            input.schoolAddress(), 
            input.postalCode(), 
            input.position(), 
            input.studentCount(), 
            null, 
            RegisterFormStatus.PENDING, 
            now, 
            now, 
            null
        );
        registerFormRepository.save(newRegisterForm);
        return null;
    }
    
    
}
