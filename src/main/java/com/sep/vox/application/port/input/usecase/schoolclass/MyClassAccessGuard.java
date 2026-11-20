package com.sep.vox.application.port.input.usecase.schoolclass;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Kiểm tra quyền truy cập dùng chung cho các use case "lớp của tôi" của giáo viên.
 * Gom về một chỗ vì cả ba use case cần đúng một luật, tách ra thì dễ lệch.
 */
@Component
public class MyClassAccessGuard {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;

    public MyClassAccessGuard(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository,
            SchoolClassUserRepository schoolClassUserRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
    }

    /** Trả về id người gọi sau khi chắc chắn tài khoản còn hoạt động. */
    public UUID requireActiveCaller() {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Trạng thái người dùng không hợp lệ");
        }
        return callerId;
    }

    /** Như trên, kèm điều kiện người gọi thuộc trường được yêu cầu. */
    public UUID requireSchoolMembership(UUID schoolId) {
        var callerId = requireActiveCaller();
        if (!schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)) {
            throw new ForbiddenException("Quyền truy cập không hợp lệ");
        }
        return callerId;
    }

    /**
     * Như trên, kèm điều kiện người gọi đang là thành viên active của lớp.
     * Cố tình ném NotFound chứ không phải Forbidden: người ngoài lớp không nên
     * phân biệt được "lớp không tồn tại" với "lớp tồn tại nhưng không phải của tôi".
     */
    public UUID requireClassMembership(UUID schoolClassId) {
        var callerId = requireActiveCaller();
        var membership = schoolClassUserRepository
            .findByUserIdAndSchoolClassId(callerId, schoolClassId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));
        if (!membership.isActive()) {
            throw new NotFoundException("Không tìm thấy lớp học");
        }
        return callerId;
    }
}
