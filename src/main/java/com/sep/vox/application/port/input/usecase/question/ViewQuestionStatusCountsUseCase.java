package com.sep.vox.application.port.input.usecase.question;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewQuestionStatusCountsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.QuestionStatusCountInfo;
import com.sep.vox.application.query.repository.QuestionQueryRepository;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Đếm câu hỏi theo status trong phạm vi người gọi được xem.
 *
 * <p>Phần dựng ngữ cảnh (systemAdmin / currentSchoolId / schoolAdmin) giống hệt
 * {@link ViewQuestionsUseCase} một cách CÓ CHỦ Ý -- ba biến này là đầu vào của bộ điều kiện
 * truy cập, lệch một biến là con số đếm không còn khớp danh sách.
 */
@Service
public class ViewQuestionStatusCountsUseCase
        implements IUseCase<ViewQuestionStatusCountsQuery, List<QuestionStatusCountInfo>> {

    private final QuestionQueryRepository questionQueryRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ViewQuestionStatusCountsUseCase(
            QuestionQueryRepository questionQueryRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.questionQueryRepository = questionQueryRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionStatusCountInfo> execute(ViewQuestionStatusCountsQuery input) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var systemAdmin = userContextPort.isSystemAdmin();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));

        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var counted = questionQueryRepository.countAccessibleByStatus(
            currentUserId,
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            input.questionBankId(),
            input.questionTopicId(),
            // Chuyển sang LIKE pattern ở đây vì QuestionRepositoryImpl.findAccessible cũng
            // làm đúng vậy trước khi xuống query -- hai bên phải chuẩn hoá giống nhau.
            StringNormalization.toLikePattern(input.topicName()),
            input.type() == null ? null : input.type().name(),
            input.sharing() == null ? null : input.sharing().name(),
            input.scope(),
            StringNormalization.toLikePattern(input.keyword())
        );

        return fillMissingStatuses(counted);
    }

    /**
     * {@code GROUP BY} chỉ trả về status thực sự có câu hỏi. Trả nguyên vậy ra ngoài thì
     * biểu đồ phía client sẽ đổi số cột theo dữ liệu, và "không có câu DRAFT nào" trở nên
     * không phân biệt được với "chưa tải xong".
     *
     * <p>Thứ tự bám theo thứ tự khai báo của {@link QuestionStatus} -- đó cũng là thứ tự
     * vòng đời của câu hỏi, nên client hiển thị thẳng theo thứ tự nhận được là hợp lý.
     */
    private List<QuestionStatusCountInfo> fillMissingStatuses(List<QuestionStatusCountInfo> counted) {
        var countByStatus = counted.stream()
            .collect(Collectors.toMap(info -> info.status(), info -> info.count()));

        return Arrays.stream(QuestionStatus.values())
            .map(status -> new QuestionStatusCountInfo(status, countByStatus.getOrDefault(status, 0L)))
            .toList();
    }
}
