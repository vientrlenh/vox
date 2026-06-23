package com.sep.vox.application.port.input.usecase.registration;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ApproveRegisterFormCommand;
import com.sep.vox.application.port.input.command.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.registerform.RegisterForm;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.RegisterFormRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;
import com.sep.vox.domain.valueobject.SchoolDomain;

@Service
public class ApproveRegisterFormUseCase implements IUseCase<ApproveRegisterFormCommand, Void> {

    private final RegisterFormRepository registerFormRepository;
    private final UserContextPort userContextPort;
    private final ProvisionSchoolService provisionSchoolService;
    private final SchoolDirectoryRepository schoolDirectoryRepository;

    public ApproveRegisterFormUseCase(
            RegisterFormRepository registerFormRepository,
            UserContextPort userContextPort, 
            ProvisionSchoolService provisionSchoolService, 
            SchoolDirectoryRepository schoolDirectoryRepository
    ) {
        this.registerFormRepository = registerFormRepository;
        this.userContextPort = userContextPort;
        this.provisionSchoolService = provisionSchoolService;
        this.schoolDirectoryRepository = schoolDirectoryRepository;
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

        var schoolDirectory = resolveSchoolDirectory(registerForm, command, currentUserId, now);

        provisionSchoolService.provision(new ProvisionSchoolCommand(
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
            currentUserId,
            now)
        );

        return null;
    }

    private SchoolDirectory resolveSchoolDirectory(RegisterForm registerForm, ApproveRegisterFormCommand command, UUID currentUserId, OffsetDateTime now) {
        // Đơn từ danh mục: trường đã có entry sẵn -> dùng lại, xác minh nếu chưa
        if (registerForm.getSchoolDirectoryId() != null) {
            var directory = schoolDirectoryRepository.findById(registerForm.getSchoolDirectoryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục trường theo yêu cầu"));
            if (!directory.isVerified()) {
                directory.verify(currentUserId, now);
                schoolDirectoryRepository.save(directory);
            }
            return directory;
        }

        // Đơn tự khai: chưa có trong danh mục -> tạo entry USER_SUBMITTED rồi xác minh
        if (schoolDirectoryRepository.existsByCode(command.schoolCode())) {
            throw new DuplicatedException("Danh mục trường với mã yêu cầu đã tồn tại");
        }
        var directory = SchoolDirectory.createByUserSubmitted(
            command.schoolCode(),
            registerForm.getSchoolName(),
            command.schoolProvinceCode(),
            registerForm.getSchoolProvince(),
            registerForm.getSchoolDistrict(),
            valueOf(registerForm.getSchoolDomain()),
            registerForm.getSchoolAddress(),
            now,
            currentUserId
        );
        directory.verify(currentUserId, now);
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
