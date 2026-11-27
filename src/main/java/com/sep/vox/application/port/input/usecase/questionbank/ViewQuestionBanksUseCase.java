package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.model.question.QuestionBank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.questionbank.ViewQuestionBanksQuery;
import com.sep.vox.application.port.input.service.QuestionStatusActorResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.service.question.QuestionStatusTransition;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.repository.QuestionBankRepository;

@Service
@RequiredArgsConstructor
public class ViewQuestionBanksUseCase implements IUseCase<ViewQuestionBanksQuery, PageResult<QuestionBankDto>> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionStatusActorResolver questionStatusActorResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionBankDto> execute(ViewQuestionBanksQuery input) {
        QuestionStatusTransition.Actor actor = questionStatusActorResolver.resolveForListing();

        PageResult<QuestionBank> result = questionBankRepository.findAccessible(
            actor.schoolId(),
            actor.systemAdmin(),
            actor.schoolAdmin(),
            input.ownerType(),
            input.status(),
            input.languageId(),
            input.schoolId(),
            input.schoolGradeId(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return QuestionBankDtoMapper.toDtoPage(result);
    }
}
