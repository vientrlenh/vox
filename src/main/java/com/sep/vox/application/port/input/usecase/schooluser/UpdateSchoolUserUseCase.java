package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.UpdateSchoolUserResponse;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UpdateSchoolUserUseCase implements IUseCase<UpdateSchoolUserCommand, UpdateSchoolUserResponse> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public UpdateSchoolUserUseCase(UserContextPort userContextPort, UserRepository userRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UpdateSchoolUserResponse execute(UpdateSchoolUserCommand input) {
        if (!input.fullNameProvided() && !input.phoneProvided() && !input.addressProvided() && !input.dateOfBirthProvided()) {
            throw new IllegalArgumentException("Cần cung cấp ít nhất một trường để cập nhật");
        }

        var now = OffsetDateTime.now();
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        SchoolUserStatusValidator.requireActive(caller);
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var target = userRepository.findByIdForUpdate(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        SchoolUserStatusValidator.requireActive(target);
        if (!input.schoolId().equals(target.getSchoolId())) {
            throw new NotFoundException("Không tìm thấy người dùng");
        }

        if (input.fullNameProvided()) {
            var normalized = StringNormalization.trimAndCollapseSpaces(input.fullName());
            if (normalized == null || normalized.isBlank()) {
                throw new IllegalArgumentException("Họ tên không được để trống");
            }
            target.setFullName(new FullName(normalized));
        }

        if (input.phoneProvided()) {
            var normalized = StringNormalization.normalizePhone(input.phone());
            if (normalized == null || normalized.isBlank()) {
                throw new IllegalArgumentException("Số điện thoại không được để trống");
            }
            var existing = userRepository.findByPhone(normalized);
            if (existing.isPresent() && !existing.get().getId().equals(target.getId())) {
                throw new DuplicatedException("Số điện thoại đã tồn tại");
            }
            target.setPhone(new Phone(normalized));
        }

        if (input.addressProvided()) {
            target.setAddress(input.address() != null ? StringNormalization.trimAndCollapseSpaces(input.address()) : null);
        }

        if (input.dateOfBirthProvided()) {
            target.setDateOfBirth(input.dateOfBirth() != null ? new DateOfBirth(input.dateOfBirth()) : null);
        }

        target.setUpdatedAt(now);
        target.setUpdatedBy(callerId);
        userRepository.save(target);

        return new UpdateSchoolUserResponse(target.getId());
    }
}
