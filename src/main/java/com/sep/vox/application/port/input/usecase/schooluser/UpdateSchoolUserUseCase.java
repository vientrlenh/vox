package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.AvatarUrlPolicy;
import com.sep.vox.application.common.StringNormalization;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.UpdateSchoolUserResponse;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UpdateSchoolUserUseCase implements IUseCase<UpdateSchoolUserCommand, UpdateSchoolUserResponse> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final String allowedAvatarHosts;

    public UpdateSchoolUserUseCase(UserContextPort userContextPort, UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            @Value("${app.avatar.allowed-hosts:}") String allowedAvatarHosts) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.allowedAvatarHosts = allowedAvatarHosts;
    }

    @Override
    @Transactional
    public UpdateSchoolUserResponse execute(UpdateSchoolUserCommand input) {
        if (!input.fullNameProvided() && !input.phoneProvided() && !input.addressProvided()
                && !input.dateOfBirthProvided() && !input.avatarUrlProvided()) {
            throw new IllegalArgumentException("Cần cung cấp ít nhất một trường để cập nhật");
        }

        var now = Instant.now();
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        var callerSchoolUser = schoolUserRepository.findByUserId(caller.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không có quyền thực hiện thao tác này"));
        if (!input.schoolId().equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var target = userRepository.findByIdForUpdate(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        var targetSchoolUser = schoolUserRepository.findByUserId(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(targetSchoolUser.getSchoolId())) {
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

        // Đây là đường DUY NHẤT đặt ảnh đại diện cho học sinh: UpdateProfileUseCase từ chối học sinh
        // tự đổi ảnh, vì ảnh chân dung ở sản phẩm này là dữ liệu định danh dùng lúc giám thị điểm
        // danh. School admin đặt hộ thì tấm ảnh mới có người chịu trách nhiệm.
        // Vẫn kiểm URL dù người gọi là school admin: nhận URL host lạ đồng nghĩa mọi giáo viên mở
        // danh sách chấm sẽ tự gọi sang đó. Xem AvatarUrlPolicy.
        if (input.avatarUrlProvided()) {
            target.setAvatarUrl(AvatarUrlPolicy.normalizeOrThrow(allowedAvatarHosts, input.avatarUrl()));
        }

        target.setUpdatedAt(now);
        target.setUpdatedBy(callerId);
        try {
            userRepository.saveAndFlush(target);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Số điện thoại đã tồn tại");
        }

        return new UpdateSchoolUserResponse(target.getId());
    }
}
