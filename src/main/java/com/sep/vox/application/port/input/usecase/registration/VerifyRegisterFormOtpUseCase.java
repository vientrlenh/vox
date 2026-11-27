package com.sep.vox.application.port.input.usecase.registration;

import com.sep.vox.domain.model.school.SchoolDirectory;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.common.CachePayload;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.RegisterVerificationConsumedEvent;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.school.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.command.registration.VerifyRegisterFormOtpCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.registerform.RegisterFormVerificationMethod;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
@RequiredArgsConstructor
public class VerifyRegisterFormOtpUseCase implements IUseCase<VerifyRegisterFormOtpCommand, Void> {

    private final CacheManagerPort cacheManagerPort;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final RegisterFormRepository registerFormRepository;
    private final ProvisionSchoolService provisionSchoolService;
    private final EventPublisherPort eventPublisherPort;

    @Override
    @Transactional
    public Void execute(VerifyRegisterFormOtpCommand input) {
        VerifyRegisterFormOtpCommand command = normalize(input);

        String key = CacheKey.registerVerificationKey(command.email());
        CachePayload.RegisterVerificationPayload payload = cacheManagerPort.get(key, CachePayload.RegisterVerificationPayload.class);
        if (payload == null) {
            throw new UnauthorizedException("Verification request failed");
        }
        requireMatchingOtp(payload, command.otp());

        SchoolDirectory schoolDirectory = schoolDirectoryRepository.findById(payload.schoolDirectoryId())
            .orElseThrow(() -> new ResourceNotFoundException("School directory entry not found"));

        Instant now = Instant.now();
        provisionSchoolService.provision(toProvisionSchoolCommand(schoolDirectory, payload, now));

        RegisterForm registerForm = RegisterForm.fromDirectoryWithVerifiedOtp(payload.schoolDirectoryId(), RegisterFormVerificationMethod.DOMAIN_OTP, payload.fullName(), payload.identityNumber(), payload.email(), payload.phone(), payload.dateOfBirth(), payload.address(), payload.postalCode(), payload.position(), payload.studentCount(), now);
        registerFormRepository.save(registerForm);

        eventPublisherPort.publish(new RegisterVerificationConsumedEvent(key));

        return null;
    }

    private void requireMatchingOtp(CachePayload.RegisterVerificationPayload payload, String otp) {
        String otpHash = payload.otpHash();

        String hashedFromRequest = oneTimePasswordPort.hash(otp);
        if (!hashedFromRequest.equals(otpHash)) {
            throw new UnauthorizedException("Verification request failed");
        }
    }

    private ProvisionSchoolCommand toProvisionSchoolCommand(
            SchoolDirectory schoolDirectory, CachePayload.RegisterVerificationPayload payload, Instant now) {
        return new ProvisionSchoolCommand(
            schoolDirectory.getCode(),
            schoolDirectory.getName(),
            null,
            schoolDirectory.getDomain(),
            schoolDirectory.getAddress(),
            payload.studentCount(),
            payload.email(),
            payload.phone(),
            payload.fullName(),
            payload.dateOfBirth(),
            payload.address(),
            null,
            now
        );
    }

    private VerifyRegisterFormOtpCommand normalize(VerifyRegisterFormOtpCommand input) {
        return new VerifyRegisterFormOtpCommand(
            StringNormalization.normalizeEmail(input.email()), 
            StringNormalization.trimAndCollapseSpaces(input.otp())
        );
    }

    
}
