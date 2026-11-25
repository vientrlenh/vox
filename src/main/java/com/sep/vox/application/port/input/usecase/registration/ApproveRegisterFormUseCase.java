package com.sep.vox.application.port.input.usecase.registration;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.registration.ApproveRegisterFormCommand;
import com.sep.vox.application.port.input.command.school.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;
import com.sep.vox.domain.valueobject.SchoolDomain;

@Service
@RequiredArgsConstructor
public class ApproveRegisterFormUseCase implements IUseCase<ApproveRegisterFormCommand, Void> {

    private final RegisterFormRepository registerFormRepository;
    private final UserContextPort userContextPort;
    private final ProvisionSchoolService provisionSchoolService;
    private final SchoolDirectoryRepository schoolDirectoryRepository;

    @Override
    @Transactional
    public Void execute(ApproveRegisterFormCommand input) {
        ApproveRegisterFormCommand command = normalize(input);
        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        RegisterForm registerForm = registerFormRepository.findById(command.registerFormId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đăng ký theo yêu cầu"));

        int updatedRows = registerFormRepository.updateApprovedRegisterForm(command.registerFormId(), currentUserId);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("Đơn đăng ký không ở trạng thái chờ hoặc không tồn tại");
        }

        SchoolDirectory schoolDirectory = resolveSchoolDirectory(registerForm, command);
        provisionSchoolService.provision(toProvisionSchoolCommand(registerForm, schoolDirectory, command, now));
        return null;
    }

    private ProvisionSchoolCommand toProvisionSchoolCommand(
        RegisterForm registerForm,
        SchoolDirectory schoolDirectory,
        ApproveRegisterFormCommand command,
        Instant now
    ) {
        return new ProvisionSchoolCommand(
            schoolDirectory.getCode(),
            schoolDirectory.getName(),
            command.description(),
            schoolDirectory.getDomain(),
            schoolDirectory.getAddress(),
            registerForm.getStudentCount().value(),
            registerForm.getContactEmail().value(),
            registerForm.getContactPhone().value(),
            registerForm.getContactFullName().value(),
            registerForm.getDateOfBirth().value(),
            registerForm.getContactAddress(),
            null,
            now);
    }

    private SchoolDirectory resolveSchoolDirectory(RegisterForm registerForm, ApproveRegisterFormCommand command) {
        // A form from the directory already has an entry: reuse it and verify it if needed
        if (registerForm.getSchoolDirectoryId() != null) {
            return verifyExistingSchoolDirectory(registerForm.getSchoolDirectoryId());
        }
        return createVerifiedUserSubmittedDirectory(registerForm, command);
    }

    private SchoolDirectory verifyExistingSchoolDirectory(UUID schoolDirectoryId) {
        SchoolDirectory directory = schoolDirectoryRepository.findById(schoolDirectoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục trường theo yêu cầu"));
        if (!directory.isVerified()) {
            directory.verify();
            schoolDirectoryRepository.save(directory);
        }
        return directory;
    }

    // A self-declared form is not in the directory yet: create a USER_SUBMITTED entry, then verify it
    private SchoolDirectory createVerifiedUserSubmittedDirectory(
        RegisterForm registerForm,
        ApproveRegisterFormCommand command
    ) {
        if (schoolDirectoryRepository.existsByCode(command.schoolCode())) {
            throw new ResourceDuplicatedException("Danh mục trường với mã yêu cầu đã tồn tại");
        }
        SchoolDirectory directory = SchoolDirectory.createByUserSubmitted(
            command.schoolCode(),
            registerForm.getSchoolName(),
            command.schoolProvinceCode(),
            registerForm.getSchoolProvince(),
            registerForm.getSchoolDistrict(),
            valueOf(registerForm.getSchoolDomain()),
            registerForm.getSchoolAddress()
        );
        directory.verify();
        return schoolDirectoryRepository.save(directory);
    }

    private ApproveRegisterFormCommand normalize(ApproveRegisterFormCommand input) {
        return new ApproveRegisterFormCommand(
            input.registerFormId(),
            StringNormalization.normalizeCode(input.schoolCode()), 
            StringNormalization.trimAndCollapseSpaces(input.description()), 
            StringNormalization.trimAndCollapseSpaces(input.schoolProvinceCode())
        );
    }

    private String valueOf(SchoolDomain domain) {
        return domain == null ? null : domain.value();
    }
}
