package com.sep.vox.application.port.input.usecase.registration;

import java.time.Instant;

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
import com.sep.vox.domain.repository.UserRepository;

@Service
public class RegisterBySelfDeclaredUseCase implements IUseCase<RegisterBySelfDeclaredCommand, Void> {

    private final RegisterFormRepository registerFormRepository;
    private final UserRepository userRepository;
    private final RegisterFormDocumentRepository registerFormDocumentRepository;

    public RegisterBySelfDeclaredUseCase(RegisterFormRepository registerFormRepository, UserRepository userRepository, RegisterFormDocumentRepository registerFormDocumentRepository) {
        this.registerFormRepository = registerFormRepository;
        this.userRepository = userRepository;
        this.registerFormDocumentRepository = registerFormDocumentRepository;
    }

    @Override
    @Transactional
    public Void execute(RegisterBySelfDeclaredCommand input) {
        var command = normalize(input);

        // KHÔNG chặn theo tên miền nữa: một trường nhiều cơ sở dùng chung 1 tên miền là chuyện
        // bình thường (vd Phổ thông Năng Khiếu có 2 cơ sở trên ptnk.edu.vn), mà mỗi cơ sở là một
        // School độc lập. Chặn theo tên miền thì cơ sở nộp đơn sau vĩnh viễn không đăng ký được --
        // blockingFormStatuses cũ tính cả APPROVED nên không phải chờ hết hạn gì cả. Danh tính
        // thật của trường là MÃ TRƯỜNG, và mã vẫn được ProvisionSchoolService chống trùng.
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
