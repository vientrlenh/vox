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
 * Counts questions by status within the scope the caller may see.
 *
 * <p>The context setup (systemAdmin / currentSchoolId / schoolAdmin) is IDENTICAL to
 * {@link ViewQuestionsUseCase} ON PURPOSE -- these three variables feed the access
 * condition, and if one of them differs the counts no longer match the list.
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
     * {@code GROUP BY} only returns statuses that actually have questions. Passing that out as is would make the
     * client chart change its number of columns with the data, and "no DRAFT question" would become
     * indistinguishable from "not loaded yet".
     *
     * <p>The order follows the declaration order of {@link QuestionStatus} -- which is also the
     * lifecycle order of a question, so it makes sense for the client to display it in the order received.
     */
    private List<QuestionStatusCountInfo> fillMissingStatuses(List<QuestionStatusCountInfo> counted) {
        Map<QuestionStatus, Long> countByStatus = counted.stream()
            .collect(Collectors.toMap(info -> info.status(), info -> info.count()));

        return Arrays.stream(QuestionStatus.values())
            .map(status -> new QuestionStatusCountInfo(status, countByStatus.getOrDefault(status, 0L)))
            .toList();
    }
}
