package com.sep.vox.application.port.input.usecase.registration;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.common.CachePayload;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.RegisterVerificationOtpRequestedPayloadV1;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.registration.RegisterFromSchoolDirectoryCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.registration.RegisterFromSchoolDirectoryResponse;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormDocument;
import com.sep.vox.domain.model.registerform.RegisterFormStatus;
import com.sep.vox.domain.model.registerform.RegisterFormVerificationMethod;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.RegisterFormDocumentRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;


@Service
@RequiredArgsConstructor
public class RegisterFromSchoolDirectoryUseCase implements IUseCase<RegisterFromSchoolDirectoryCommand, RegisterFromSchoolDirectoryResponse> {

    private final RegisterFormRepository registerFormRepository;
    private final RegisterFormDocumentRepository registerFormDocumentRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final UserRepository userRepository;
    private final CacheManagerPort cacheManagerPort;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    private static final List<RegisterFormStatus> blockingFormStatuses = List.of(
        RegisterFormStatus.APPROVED, 
        RegisterFormStatus.AUTO_APPROVED, 
        RegisterFormStatus.PENDING
    );

    /** Phải >= TTL bên RegisterVerificationOtpEmailConsumer, nếu không hồ sơ hết hạn trước khi consumer kịp chèn mã. */
    private static final Duration TTL = Duration.ofMinutes(10);

    @Override
    @Transactional
    public RegisterFromSchoolDirectoryResponse execute(RegisterFromSchoolDirectoryCommand input) {
        RegisterFromSchoolDirectoryCommand command = normalize(input);

        SchoolDirectory schoolDirectory = schoolDirectoryRepository.findById(command.schoolDirectoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục trường theo yêu cầu"));
        
        if (schoolRepository.existsByCode(schoolDirectory.getCode())) {
            throw new ResourceDuplicatedException("Trường này đã được đăng ký");
        }

        if (registerFormRepository.existsBySchoolDirectoryIdAndStatusIn(command.schoolDirectoryId(), blockingFormStatuses)) {
            throw new ResourceDuplicatedException("Hiện đang có đơn đăng ký đang chờ phê duyệt cho danh mục trường này");
        }

        boolean formAlreadyPendingByContactInfo = registerFormRepository.existsByContactEmailAndStatus(command.contactEmail(), RegisterFormStatus.PENDING) || registerFormRepository.existsByContactPhoneAndStatus(command.contactPhone(), RegisterFormStatus.PENDING);

        if (formAlreadyPendingByContactInfo) {
            throw new ResourceDuplicatedException("Thông tin yêu cầu đang tồn tại đơn đăng ký cần được phê duyệt");
        }

        boolean alreadyUser = userRepository.existsByEmail(command.contactEmail()) || userRepository.existsByPhone(command.contactPhone());

        if (alreadyUser) {
            throw new ResourceDuplicatedException("Email hoặc số điện thoại yêu cầu đã được đăng ký");
        }

        RegisterFormVerificationMethod method = getVerificationMethod(schoolDirectory, command);
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

    private RegisterFormVerificationMethod getVerificationMethod(SchoolDirectory directory, RegisterFromSchoolDirectoryCommand command) {
        boolean verifiedDirOrigin = directory.isVerified();

        String email = command.contactEmail();
        String dirDomain = directory.getDomain();
        String emailDomain = email.substring(email.indexOf("@") + 1);
        boolean trustedEmailDomain = dirDomain != null && emailDomain.equalsIgnoreCase(dirDomain);

         if (verifiedDirOrigin && trustedEmailDomain) {
            return RegisterFormVerificationMethod.DOMAIN_OTP;
        }
        return RegisterFormVerificationMethod.DOCUMENT;
    }


    /**
     * Chỉ ghi hồ sơ đăng ký vào cache rồi phát sự kiện. OTP KHÔNG sinh ở đây nữa:
     * {@code RegisterVerificationOtpEmailConsumer} sinh mã ngay trước lúc gửi mail rồi chèn
     * bản hash vào chính dòng cache này, để credential không đi qua outbox hay Kafka.
     */
    private void saveAndSendRegisterVerificationOtp(SchoolDirectory schoolDir, RegisterFromSchoolDirectoryCommand command) {
        String registerVerificationKey = CacheKey.registerVerificationKey(command.contactEmail());

        CachePayload.RegisterVerificationPayload payload = new CachePayload.RegisterVerificationPayload(
            null,
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

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.REGISTER_FORM,
            command.schoolDirectoryId(),
            EventTypeConstant.REGISTER_VERIFICATION_OTP_REQUESTED,
            jsonSerializationPort.toJson(
                new RegisterVerificationOtpRequestedPayloadV1(command.contactEmail())),
            Instant.now()
        ));
    }

    private void saveRegisterFormWithDocuments(RegisterFromSchoolDirectoryCommand command, RegisterFormVerificationMethod method) {
        Instant now = Instant.now();
        RegisterForm registerForm = RegisterForm.fromDirectoryWithDocuments(
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
        RegisterForm savedForm = registerFormRepository.save(registerForm);
        List<RegisterFormDocument> documents = RegisterFormDocument.createMany(savedForm.getId(), command.documentUrls(), now);
        registerFormDocumentRepository.saveAll(documents);
    }
}
