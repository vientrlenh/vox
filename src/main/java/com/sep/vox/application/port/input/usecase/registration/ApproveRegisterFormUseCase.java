package com.sep.vox.application.port.input.usecase.registration;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ApproveRegisterFormCommand;
import com.sep.vox.application.port.input.command.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.valueobject.SchoolDomain;

@Service
public class ApproveRegisterFormUseCase implements IUseCase<ApproveRegisterFormCommand, Void> {

    private final RegisterFormRepository registerFormRepository;
    private final UserContextPort userContextPort;
    private final ProvisionSchoolService provisionSchoolService;

    public ApproveRegisterFormUseCase(
            RegisterFormRepository registerFormRepository,
            UserContextPort userContextPort, 
            ProvisionSchoolService provisionSchoolService
    ) {
        this.registerFormRepository = registerFormRepository;
        this.userContextPort = userContextPort;
        this.provisionSchoolService = provisionSchoolService;
    }

    @Override
    @Transactional
    public Void execute(ApproveRegisterFormCommand input) {
        var command = normalize(input);
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var registerForm = registerFormRepository.findById(command.registerFormId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đăng ký theo yêu cầu"));
        
        var updatedRows = registerFormRepository.updateApprovedRegisterForm(command.registerFormId(), currentUserId, now);
        if (updatedRows == 0) {
            throw new IllegalStateException("Đơn đăng ký không ở trạng thái chờ hoặc không tồn tại");
        }


        provisionSchoolService.provision(new ProvisionSchoolCommand(
            command.schoolCode(), 
            registerForm.getSchoolName(), 
            command.description(), 
            valueOf(registerForm.getSchoolDomain()), 
            registerForm.getSchoolAddress(), 
            registerForm.getStudentCount().value(), 
            registerForm.getContactEmail().value(), 
            registerForm.getContactPhone().value(), 
            registerForm.getContactFullName().value(), 
            registerForm.getDateOfBirth().value(), 
            registerForm.getContactAddress(), 
            null, 
            currentUserId, 
            now)
        );

        return null;
    }

    private ApproveRegisterFormCommand normalize(ApproveRegisterFormCommand input) {
        return new ApproveRegisterFormCommand(
            input.registerFormId(),
            StringNormalization.normalizeCode(input.schoolCode()), 
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }

    private String valueOf(SchoolDomain domain) {
        return domain == null ? null : domain.value();
    }
}
