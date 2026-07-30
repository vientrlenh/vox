package com.sep.vox.application.port.input.usecase.registration;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.RegisterBySelfDeclaredCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.repository.RegisterFormDocumentRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class RegisterBySelfDeclaredUseCase implements IUseCase<RegisterBySelfDeclaredCommand, Void> {

    private final RegisterFormRepository registerFormRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final RegisterFormDocumentRepository registerFormDocumentRepository;

    public RegisterBySelfDeclaredUseCase(RegisterFormRepository registerFormRepository, SchoolRepository schoolRepository, UserRepository userRepository, RegisterFormDocumentRepository registerFormDocumentRepository) {
        this.registerFormRepository = registerFormRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.registerFormDocumentRepository = registerFormDocumentRepository;
    }

    private static final List<RegisterFormStatus> blockingFormStatuses = List.of(
        RegisterFormStatus.APPROVED, 
        RegisterFormStatus.AUTO_APPROVED, 
        RegisterFormStatus.PENDING
    );

    @Override
    @Transactional
    public Void execute(RegisterBySelfDeclaredCommand input) {
        var command = normalize(input);

        if (command.schoolDomain() != null) {
            if (schoolRepository.existsByDomain(command.schoolDomain())) {
                throw new DuplicatedException("Domain yêu cầu đã được đăng ký");
            }
            if (registerFormRepository.existsBySchoolDomainAndStatusIn(command.schoolDomain(), blockingFormStatuses)) {
                throw new DuplicatedException("Hiện đang có đơn đăng ký đang chờ phê duyệt cho domain trường này");
            }
        } 

        var formAlreadyPendingByContactInfo = registerFormRepository.existsByContactEmailAndStatus(command.contactEmail(), RegisterFormStatus.PENDING) || registerFormRepository.existsByContactPhoneAndStatus(command.contactPhone(), RegisterFormStatus.PENDING);

        if (formAlreadyPendingByContactInfo) {
            throw new DuplicatedException("Thông tin yêu cầu đang tồn tại đơn đăng ký cần được phê duyệt");
        }

        var alreadyUser = userRepository.existsByEmail(command.contactEmail()) || userRepository.existsByPhone(command.contactPhone());

        if (alreadyUser) {
            throw new DuplicatedException("Email hoặc số điện thoại yêu cầu đã được đăng ký");
        }

        if (command.documentUrls() == null || command.documentUrls().isEmpty()) {
            throw new IllegalArgumentException("Cần gửi tài liệu để xác thực");
        }

        var now = Instant.now();
        var registerForm = RegisterForm.selfDeclared(
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

        var savedForm = registerFormRepository.save(registerForm);
        var documents = RegisterFormDocument.createMany(savedForm.getId(), command.documentUrls(), now);
        registerFormDocumentRepository.saveAll(documents);
        return null;
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
