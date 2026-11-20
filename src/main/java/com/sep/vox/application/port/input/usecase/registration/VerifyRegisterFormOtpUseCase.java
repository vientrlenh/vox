package com.sep.vox.application.port.input.usecase.registration;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.common.CachePayload;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.RegisterVerificationConsumedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.command.VerifyRegisterFormOtpCommand;
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
public class VerifyRegisterFormOtpUseCase implements IUseCase<VerifyRegisterFormOtpCommand, Void> {

    private final CacheManagerPort cacheManagerPort;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final RegisterFormRepository registerFormRepository;
    private final ProvisionSchoolService provisionSchoolService;
    private final EventPublisherPort eventPublisherPort;

    public VerifyRegisterFormOtpUseCase(
        CacheManagerPort cacheManagerPort, 
        OneTimePasswordPort oneTimePasswordPort, 
        SchoolDirectoryRepository schoolDirectoryRepository, 
        RegisterFormRepository registerFormRepository, 
        ProvisionSchoolService provisionSchoolService, EventPublisherPort eventPublisherPort) {
        this.cacheManagerPort = cacheManagerPort;
        this.schoolDirectoryRepository = schoolDirectoryRepository;
        this.registerFormRepository = registerFormRepository;
        this.oneTimePasswordPort = oneTimePasswordPort;
        this.provisionSchoolService = provisionSchoolService;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public Void execute(VerifyRegisterFormOtpCommand input) {
        var command = normalize(input);

        var key = CacheKey.registerVerificationKey(command.email());
        var payload = cacheManagerPort.get(key, CachePayload.RegisterVerificationPayload.class);
        if (payload == null) {
            throw new UnauthorizedException("Yêu cầu xác thực thất bại");
        }
        var otpHash = payload.otpHash();

        var hashedFromRequest = oneTimePasswordPort.hash(command.otp());
        if (!hashedFromRequest.equals(otpHash)) {
            throw new UnauthorizedException("Yêu cầu xác thực thất bại");
        }

        var schoolDirectory = schoolDirectoryRepository.findById(payload.schoolDirectoryId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục trường"));

        var now = Instant.now();
        provisionSchoolService.provision(new ProvisionSchoolCommand(
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
            null, 
            now
        ));

        var registerForm = RegisterForm.fromDirectoryWithVerifiedOtp(payload.schoolDirectoryId(), RegisterFormVerificationMethod.DOMAIN_OTP, payload.fullName(), payload.identityNumber(), payload.email(), payload.phone(), payload.dateOfBirth(), payload.address(), payload.postalCode(), payload.position(), payload.studentCount(), now);
        registerFormRepository.save(registerForm);

        eventPublisherPort.publish(new RegisterVerificationConsumedEvent(key));

        return null;
    }

    private VerifyRegisterFormOtpCommand normalize(VerifyRegisterFormOtpCommand input) {
        return new VerifyRegisterFormOtpCommand(
            StringNormalization.normalizeEmail(input.email()), 
            StringNormalization.trimAndCollapseSpaces(input.otp())
        );
    }

    
}
