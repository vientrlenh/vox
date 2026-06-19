package com.sep.vox.application.port.input.usecase.registration;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RegisterFromSchoolDirectoryCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.registerform.RegisterFormVerificationMethod;
import com.sep.vox.domain.model.school.SchoolDirectorySource;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;


@Service
public class RegisterFromSchoolDirectoryUseCase implements IUseCase<RegisterFromSchoolDirectoryCommand, Void> {

    private final RegisterFormRepository registerFormRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final UserRepository userRepository;

    public RegisterFromSchoolDirectoryUseCase(RegisterFormRepository registerFormRepository, SchoolRepository schoolRepository, SchoolDirectoryRepository schoolDirectoryRepository, UserRepository userRepository) {
        this.registerFormRepository = registerFormRepository;
        this.schoolRepository = schoolRepository;
        this.schoolDirectoryRepository = schoolDirectoryRepository;
        this.userRepository = userRepository;
    }

    private static final List<RegisterFormStatus> blockingFormStatuses = List.of(
        RegisterFormStatus.APPROVED, 
        RegisterFormStatus.AUTO_APPROVED, 
        RegisterFormStatus.PENDING
    );

    @Override
    @Transactional
    public Void execute(RegisterFromSchoolDirectoryCommand input) {
        var command = normalize(input);

        RegisterFormVerificationMethod method;
        var schoolDirectory = schoolDirectoryRepository.findById(command.schoolDirectoryId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục trường theo yêu cầu"));
        
        if (schoolRepository.existsByCode(schoolDirectory.getCode())) {
            throw new DuplicatedException("Trường này đã được đăng ký");
        }


        if (registerFormRepository.existsBySchoolDirectoryIdAndStatusIn(input.schoolDirectoryId(), blockingFormStatuses)) {
            throw new IllegalArgumentException("Hiện đang có đơn đăng ký đang chờ phê duyệt cho danh mục trường này");
        }

        var formAlreadyPendingByContactInfo = registerFormRepository.existsByContactEmailAndStatus(command.contactEmail(), RegisterFormStatus.PENDING) || registerFormRepository.existsByContactPhoneAndStatus(command.contactPhone(), RegisterFormStatus.PENDING);

        if (formAlreadyPendingByContactInfo) {
            throw new IllegalArgumentException("Thông tin yêu cầu đang tồn tại đơn đăng ký cần được phê duyệt");
        }

        var alreadyUser = userRepository.existsByEmail(command.contactEmail()) || userRepository.existsByPhone(command.contactPhone());

        if (alreadyUser) {
            throw new DuplicatedException("Email hoặc số điện thoại yêu cầu đã được đăng ký");
        }

        var verifiedDirectorySource = schoolDirectory.getSource() == SchoolDirectorySource.ADMIN_VERIFIED;

        var email = command.contactEmail();
        var dirDomain = schoolDirectory.getDomain();
        var emailDomain = email.substring(email.indexOf("@") + 1);
        var trustedEmailDomain = dirDomain != null && emailDomain.equalsIgnoreCase(dirDomain);

        var now = OffsetDateTime.now();

        if (verifiedDirectorySource && trustedEmailDomain) {
            method = RegisterFormVerificationMethod.DOMAIN_OTP;
        } else {
            method = RegisterFormVerificationMethod.DOCUMENT;
        }

        var registerForm = RegisterForm.fromDirectory(
            command.schoolDirectoryId(), 
            method, 
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
        
        registerFormRepository.save(registerForm);
        return null;
    }
    
    private RegisterFromSchoolDirectoryCommand normalize(RegisterFromSchoolDirectoryCommand input) {
        return new RegisterFromSchoolDirectoryCommand(
            input.schoolDirectoryId(),
            StringNormalization.trimAndCollapseSpaces(input.contactFullName()),
            StringNormalization.normalizeIdentityNumber(input.identityNumber()),
            StringNormalization.normalizePhone(input.contactPhone()),
            StringNormalization.normalizeEmail(input.contactEmail()),
            input.dateOfBirth(),
            StringNormalization.trimAndCollapseSpaces(input.contactAddress()),
            StringNormalization.normalizeIdentityNumber(input.postalCode()),
            StringNormalization.trimAndCollapseSpaces(input.position()),
            input.studentCount()
        );
    } 
}
