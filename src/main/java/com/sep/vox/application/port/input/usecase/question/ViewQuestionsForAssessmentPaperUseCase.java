package com.sep.vox.application.port.input.usecase.question;

import com.sep.vox.domain.model.question.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.question.ViewQuestionsQuery;
import com.sep.vox.application.port.input.service.QuestionStatusActorResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.service.question.QuestionStatusTransition;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class ViewQuestionsForAssessmentPaperUseCase implements IUseCase<ViewQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionRepository questionRepository;
    private final QuestionStatusActorResolver questionStatusActorResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewQuestionsQuery input) {
        QuestionStatusTransition.Actor actor = questionStatusActorResolver.resolveForListing();

        PageResult<Question> result = questionRepository.findAccessibleForAssessmentPaper(
            actor.userId(),
            actor.schoolId(),
            actor.systemAdmin(),
            actor.schoolAdmin(),
            input.questionBankId(),
            input.questionTopicId(),
            input.topicName(),
            input.status(),
            input.type(),
            input.sharing(),
            input.assetType(),
            input.scope(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return QuestionDtoMapper.toDtoPage(result);
    }
}
