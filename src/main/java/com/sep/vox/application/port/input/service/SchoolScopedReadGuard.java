package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;

/**
 * "Được đọc dữ liệu của trường nào" -- quản trị hệ thống đọc mọi trường, quản trị trường chỉ đọc
 * trường của chính mình.
 *
 * <p>Tách thành một chỗ vì đây KHÔNG phải một vai trò mà là quan hệ giữa người gọi và {@code schoolId}
 * họ hỏi, nên {@code @PreAuthorize} trên controller không diễn đạt được: {@code hasRole('SYSTEM_ADMIN')}
 * chặn mất chính quản trị trường, còn bỏ trống thì trường này đọc được ví của trường kia. Phép kiểm
 * phải nằm ở use case, nơi biết cả hai vế -- và vì thế nó bị chép lại ở mọi use case đọc theo trường.
 */
@Service
public class SchoolScopedReadGuard {

    private final UserContextPort userContextPort;

    public SchoolScopedReadGuard(UserContextPort userContextPort) {
        this.userContextPort = userContextPort;
    }

    public void requireCanRead(UUID schoolId) {
        if (!userContextPort.isSystemAdmin() && !schoolId.equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }
}
