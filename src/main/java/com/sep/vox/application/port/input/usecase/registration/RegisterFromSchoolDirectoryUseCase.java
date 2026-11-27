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
import com.sep.vox.application.port.input.service.RegistrationContactDuplicateGuardService;
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


@Service
@RequiredArgsConstructor
public class RegisterFromSchoolDirectoryUseCase implements IUseCase<RegisterFromSchoolDirectoryCommand, RegisterFromSchoolDirectoryResponse> {

    private final RegisterFormRepository registerFormRepository;
    private final RegisterFormDocumentRepository registerFormDocumentRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final CacheManagerPort cacheManagerPort;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final RegistrationContactDuplicateGuardService registrationContactDuplicateGuardService;

    private static final List<RegisterFormStatus> blockingFormStatuses = List.of(
        RegisterFormStatus.APPROVED, 
        RegisterFormStatus.AUTO_APPROVED, 
        RegisterFormStatus.PENDING
    );

    /** Must be >= the TTL in RegisterVerificationOtpEmailConsumer, otherwise the record expires before the consumer can insert the code. */
    private static final Duration TTL = Duration.ofMinutes(10);

    @Override
    @Transactional
    public RegisterFromSchoolDirectoryResponse execute(RegisterFromSchoolDirectoryCommand input) {
        RegisterFromSchoolDirectoryCommand command = normalize(input);

        SchoolDirectory schoolDirectory = schoolDirectoryRepository.findById(command.schoolDirectoryId())
            .orElseThrow(() -> new ResourceNotFoundException("The requested school directory entry was not found"));

        rejectAlreadyRegisteredSchool(schoolDirectory, command);
        registrationContactDuplicateGuardService.requireContactNotInUse(command.contactEmail(), command.contactPhone());

        RegisterFormVerificationMethod method = getVerificationMethod(schoolDirectory, command);
        if (method == RegisterFormVerificationMethod.DOMAIN_OTP) {
            saveAndSendRegisterVerificationOtp(schoolDirectory, command);
            return new RegisterFromSchoolDirectoryResponse("The verification OTP has been sent");
        }

        if (command.documentUrls() == null || command.documentUrls().isEmpty()) {
            throw new IllegalArgumentException("A verification document is required");
        }
        saveRegisterFormWithDocuments(command, method);
        return new RegisterFromSchoolDirectoryResponse("Please wait for an administrator to verify");
    }
    
    private void rejectAlreadyRegisteredSchool(SchoolDirectory schoolDirectory, RegisterFromSchoolDirectoryCommand command) {
        if (schoolRepository.existsByCode(schoolDirectory.getCode())) {
            throw new ResourceDuplicatedException("This school has already been registered");
        }

        if (registerFormRepository.existsBySchoolDirectoryIdAndStatusIn(command.schoolDirectoryId(), blockingFormStatuses)) {
            throw new ResourceDuplicatedException("A register form is already awaiting approval for this school directory entry");
        }
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
     * Only writes the register form into the cache and then publishes the event. The OTP is NO LONGER generated
     * here: {@code RegisterVerificationOtpEmailConsumer} generates the code right before sending the mail and
     * inserts its hash into this same cache row, so the credential never passes through the outbox or Kafka.
     */
    private void saveAndSendRegisterVerificationOtp(SchoolDirectory schoolDir, RegisterFromSchoolDirectoryCommand command) {
        String registerVerificationKey = CacheKey.registerVerificationKey(command.contactEmail());

        cacheManagerPort.save(registerVerificationKey, toVerificationPayload(command), TTL);

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.REGISTER_FORM,
            command.schoolDirectoryId(),
            EventTypeConstant.REGISTER_VERIFICATION_OTP_REQUESTED,
            jsonSerializationPort.toJson(
                new RegisterVerificationOtpRequestedPayloadV1(command.contactEmail())),
            Instant.now()
        ));
    }

    private CachePayload.RegisterVerificationPayload toVerificationPayload(RegisterFromSchoolDirectoryCommand command) {
        return new CachePayload.RegisterVerificationPayload(
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
