package com.sep.vox.application.port.input.usecase.user;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.AvatarUrlPolicy;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateProfileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

/**
 * Người dùng tự sửa hồ sơ của mình. Trước đây gói {@code usecase/user} chỉ có ba use case ĐỌC, nên
 * {@code users.avatar_url} thực tế luôn null: {@code OAuth2LoginUseCase} có nhận {@code avatarUrl}
 * trong command nhưng KHÔNG dùng tới, còn lại chỉ {@code CreateSchoolUseCase} ghi được một lần lúc
 * tạo trường. Đây là đường ghi đầu tiên sau khi tài khoản đã tồn tại.
 *
 * <p>Chuẩn hóa và kiểm trùng bám theo {@code UpdateSchoolUserUseCase} để hai đường sửa cùng một
 * bảng không lệch luật nhau (họ tên không được rỗng, số điện thoại không được rỗng và không trùng).
 */
@Service
public class UpdateProfileUseCase implements IUseCase<UpdateProfileCommand, UserDto> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final String allowedAvatarHosts;

    public UpdateProfileUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            @Value("${app.avatar.allowed-hosts:}") String allowedAvatarHosts) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.allowedAvatarHosts = allowedAvatarHosts;
    }

    @Override
    @Transactional
    public UserDto execute(UpdateProfileCommand input) {
        if (!input.fullNameProvided()
                && !input.phoneProvided()
                && !input.addressProvided()
                && !input.dateOfBirthProvided()
                && !input.avatarUrlProvided()) {
            throw new IllegalArgumentException("Cần cung cấp ít nhất một trường để cập nhật");
        }

        var now = Instant.now();
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var target = userRepository.findByIdForUpdate(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (target.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Tài khoản không ở trạng thái hoạt động");
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

        if (input.avatarUrlProvided()) {
            target.setAvatarUrl(resolveAvatarUrl(input.avatarUrl()));
        }

        target.setUpdatedAt(now);
        target.setUpdatedBy(callerId);
        try {
            userRepository.saveAndFlush(target);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedException("Số điện thoại đã tồn tại");
        }

        return UserDtoMapper.toUserDto(target);
    }

    /**
     * Học sinh KHÔNG tự đổi ảnh đại diện của mình; ảnh của học sinh do trường đặt qua
     * {@code updateSchoolUser}.
     *
     * <p>Lý do thuộc về nghiệp vụ chứ không phải kỹ thuật: ở sản phẩm này ảnh chân dung là dữ liệu
     * ĐỊNH DANH. Màn {@code TeacherProctorAttendancePage} để giám thị điểm danh "Có mặt", gắn cờ
     * phiên thi, chặn thí sinh và kết thúc bài thi -- một tấm ảnh do chính thí sinh tự chọn sẽ trông
     * như bằng chứng nhận dạng trong khi thực chất là lời tự khai, tức là tệ hơn không có ảnh. Thêm
     * nữa người dùng ở đây là học sinh phổ thông, và repo chưa có bất kỳ công cụ kiểm duyệt ảnh nào.
     *
     * <p>Viết dạng allowlist (phải là một trong ba vai) thay vì {@code isStudent()} là có chủ đích:
     * tài khoản không mang vai nào cũng bị từ chối, thay vì lọt qua vì "không phải học sinh".
     */
    private String resolveAvatarUrl(String rawUrl) {
        var mayManageOwnAvatar = userContextPort.isSystemAdmin()
            || userContextPort.isSchoolAdmin()
            || userContextPort.isTeacher();
        if (!mayManageOwnAvatar) {
            throw new ForbiddenException("Ảnh đại diện của học sinh do nhà trường cập nhật");
        }

        // null/rỗng = yêu cầu GỠ ảnh; phân biệt được với "không gửi trường" nhờ cờ avatarUrlProvided.
        return AvatarUrlPolicy.normalizeOrThrow(allowedAvatarHosts, rawUrl);
    }
}
