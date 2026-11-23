package com.sep.vox.application.port.input.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.question.QuestionStatusTransition;

/**
 * Resolve bối cảnh phân quyền của người đang thao tác trên câu hỏi.
 *
 * <p>Tách riêng để đường đi hàng loạt gọi đúng MỘT lần cho cả batch. Trước đây ba query này chạy
 * lại cho từng câu hỏi trong vòng lặp, nên cập nhật 50 câu là 150 query thừa.
 *
 * <p>Cố ý KHÔNG đánh {@code @Transactional}: chỉ đọc, và luôn được gọi từ trong transaction của
 * use case nên tự tham gia sẵn. Thêm proxy transaction ở đây chỉ mở lại đúng cái bẫy rollback-only
 * mà {@link QuestionStatusTransition} được thiết kế để tránh.
 */
@Service
@RequiredArgsConstructor
public class QuestionStatusActorResolver {

    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public QuestionStatusTransition.Actor resolve() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean systemAdmin = userContextPort.isSystemAdmin();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = !systemAdmin
            && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .anyMatch(role -> RoleConstant.SCHOOL_ADMIN_ROLE.equals(role.roleCode()));

        return new QuestionStatusTransition.Actor(currentUserId, currentSchoolId, systemAdmin, schoolAdmin);
    }
}
