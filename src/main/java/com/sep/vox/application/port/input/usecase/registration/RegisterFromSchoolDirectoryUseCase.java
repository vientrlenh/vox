package com.sep.vox.application.port.input.usecase.registration;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.common.CachePayload;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.SendRegisterVerificationOtpEvent;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RegisterFromSchoolDirectoryCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.application.response.input.registration.RegisterFromSchoolDirectoryResponse;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.registerform.RegisterFormVerificationMethod;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.model.school.SchoolDirectorySource;
import com.sep.vox.domain.repository.RegisterFormDocumentRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;


@Service
public class RegisterFromSchoolDirectoryUseCase implements IUseCase<RegisterFromSchoolDirectoryCommand, RegisterFromSchoolDirectoryResponse> {

    private final RegisterFormRepository registerFormRepository;
    private final RegisterFormDocumentRepository registerFormDocumentRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final UserRepository userRepository;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final CacheManagerPort cacheManagerPort;
    private final EventPublisherPort eventPublisherPort;

    public RegisterFromSchoolDirectoryUseCase(RegisterFormRepository registerFormRepository, RegisterFormDocumentRepository registerFormDocumentRepository, SchoolRepository schoolRepository, SchoolDirectoryRepository schoolDirectoryRepository, UserRepository userRepository, OneTimePasswordPort oneTimePasswordPort, CacheManagerPort cacheManagerPort, EventPublisherPort eventPublisherPort) {
        this.registerFormRepository = registerFormRepository;
        this.registerFormDocumentRepository = registerFormDocumentRepository;
        this.schoolRepository = schoolRepository;
        this.schoolDirectoryRepository = schoolDirectoryRepository;
        this.userRepository = userRepository;
        this.oneTimePasswordPort = oneTimePasswordPort;
        this.cacheManagerPort = cacheManagerPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    private static final List<RegisterFormStatus> blockingFormStatuses = List.of(
        RegisterFormStatus.APPROVED, 
        RegisterFormStatus.AUTO_APPROVED, 
        RegisterFormStatus.PENDING
    );

    private static final int OTP_SIZE = 6;
    private static final Duration TTL = Duration.ofMinutes(10);

    @Override
    @Transactional
    public RegisterFromSchoolDirectoryResponse execute(RegisterFromSchoolDirectoryCommand input) {
        var command = normalize(input);

        var schoolDirectory = schoolDirectoryRepository.findById(command.schoolDirectoryId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục trường theo yêu cầu"));
        
        if (schoolRepository.existsByCode(schoolDirectory.getCode())) {
            throw new DuplicatedException("Trường này đã được đăng ký");
        }

        if (registerFormRepository.existsBySchoolDirectoryIdAndStatusIn(command.schoolDirectoryId(), blockingFormStatuses)) {
            throw new DuplicatedException("Hiện đang có đơn đăng ký đang chờ phê duyệt cho danh mục trường này");
        }

        var formAlreadyPendingByContactInfo = registerFormRepository.existsByContactEmailAndStatus(command.contactEmail(), RegisterFormStatus.PENDING) || registerFormRepository.existsByContactPhoneAndStatus(command.contactPhone(), RegisterFormStatus.PENDING);

        if (formAlreadyPendingByContactInfo) {
            throw new DuplicatedException("Thông tin yêu cầu đang tồn tại đơn đăng ký cần được phê duyệt");
        }

        var alreadyUser = userRepository.existsByEmail(command.contactEmail()) || userRepository.existsByPhone(command.contactPhone());

        if (alreadyUser) {
            throw new DuplicatedException("Email hoặc số điện thoại yêu cầu đã được đăng ký");
        }

        var method = getVerificationMethod(schoolDirectory, command);
        if (method == RegisterFormVerificationMethod.DOMAIN_OTP) {
            saveAndSendRegisterVerificationOtp(schoolDirectory, command);
            return new RegisterFromSchoolDirectoryResponse("Mã OTP xác thực đã được gửi");
        }

        if (command.documentUrls() == null || command.documentUrls().isEmpty()) {
            throw new IllegalArgumentException("Cần gửi tài liệu để xác thực");
        }
        saveRegisterFormWithDocuments(command, method);
        return new RegisterFromSchoolDirectoryResponse("Vui lòng đợi quản trị viên xác thực");
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
            input.studentCount(), 
            input.documentUrls() == null ? null : input.documentUrls().stream()
            .map(StringNormalization::trimAndCollapseSpaces)
            .toList()
        );
    } 

    private RegisterFormVerificationMethod getVerificationMethod(SchoolDirectory schoolDir, RegisterFromSchoolDirectoryCommand command) {
        var verifiedDirSource = schoolDir.getSource() == SchoolDirectorySource.ADMIN_VERIFIED;

        var email = command.contactEmail();
        var dirDomain = schoolDir.getDomain();
        var emailDomain = email.substring(email.indexOf("@") + 1);
        var trustedEmailDomain = dirDomain != null && emailDomain.equalsIgnoreCase(dirDomain);

         if (verifiedDirSource && trustedEmailDomain) {
            return RegisterFormVerificationMethod.DOMAIN_OTP;
        }
        return RegisterFormVerificationMethod.DOCUMENT;
    }


    private void saveAndSendRegisterVerificationOtp(SchoolDirectory schoolDir, RegisterFromSchoolDirectoryCommand command) {
        var registerVerificationKey = CacheKey.registerVerificationKey(command.contactEmail());
        var otp = oneTimePasswordPort.generate(OTP_SIZE);
        var otpHash = oneTimePasswordPort.hash(otp);

        var payload = new CachePayload.RegisterVerificationPayload(
            otpHash, 
            command.contactEmail(), 
            command.schoolDirectoryId(), 
            command.contactFullName(), 
            command.identityNumber(), 
            command.contactPhone(), 
            command.dateOfBirth(), 
            command.contactAddress(), 
            command.postalCode(), 
            command.position(), 
            command.studentCount()
        );

        cacheManagerPort.save(registerVerificationKey, payload, TTL);
        eventPublisherPort.publish(new SendRegisterVerificationOtpEvent(command.contactEmail(), otp));
    }

    private void saveRegisterFormWithDocuments(RegisterFromSchoolDirectoryCommand command, RegisterFormVerificationMethod method) {
        var now = OffsetDateTime.now();
        var registerForm = RegisterForm.fromDirectoryWithDocuments(
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
        var savedForm = registerFormRepository.save(registerForm);
        var documents = RegisterFormDocument.createMany(savedForm.getId(), command.documentUrls(), now);
        registerFormDocumentRepository.saveAll(documents);
    }
}
