package com.sep.vox.application.port.input.usecase.question;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.question.ViewQuestionStatusCountsQuery;
import com.sep.vox.application.port.input.service.QuestionStatusActorResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.service.question.QuestionStatusTransition;
import com.sep.vox.application.query.dto.QuestionStatusCountInfo;
import com.sep.vox.application.query.repository.QuestionQueryRepository;
import com.sep.vox.domain.model.question.QuestionStatus;

/**
 * Đếm câu hỏi theo status trong phạm vi người gọi được xem.
 *
 * <p>Phần dựng ngữ cảnh (systemAdmin / currentSchoolId / schoolAdmin) giống hệt
 * {@link ViewQuestionsUseCase} một cách CÓ CHỦ Ý -- ba biến này là đầu vào của bộ điều kiện
 * truy cập, lệch một biến là con số đếm không còn khớp danh sách.
 */
@Service
@RequiredArgsConstructor
public class ViewQuestionStatusCountsUseCase
        implements IUseCase<ViewQuestionStatusCountsQuery, List<QuestionStatusCountInfo>> {

    private final QuestionQueryRepository questionQueryRepository;
    private final QuestionStatusActorResolver questionStatusActorResolver;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionStatusCountInfo> execute(ViewQuestionStatusCountsQuery input) {
        QuestionStatusTransition.Actor actor = questionStatusActorResolver.resolveForListing();

        List<QuestionStatusCountInfo> counted = questionQueryRepository.countAccessibleByStatus(
            actor.userId(),
            actor.schoolId(),
            actor.systemAdmin(),
            actor.schoolAdmin(),
            input.questionBankId(),
            input.questionTopicId(),
            // Convert to a LIKE pattern here because QuestionRepositoryImpl.findAccessible does exactly
            // the same before going down to the query -- both sides must normalise the same way.
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
        Map<QuestionStatus, Long> countByStatus = counted.stream()
            .collect(Collectors.toMap(info -> info.status(), info -> info.count()));

        return Arrays.stream(QuestionStatus.values())
            .map(status -> new QuestionStatusCountInfo(status, countByStatus.getOrDefault(status, 0L)))
            .toList();
    }
}
