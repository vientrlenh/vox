package com.sep.vox.application.port.input.usecase.question;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewQuestionsByTopicQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class ViewQuestionsByTopicUseCase implements IUseCase<ViewQuestionsByTopicQuery, PageResult<QuestionDto>> {

    private final QuestionRepository questionRepository;

    public ViewQuestionsByTopicUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewQuestionsByTopicQuery input) {
        var result = questionRepository.findByTopicId(input.topicId(), input.page(), input.size());
        return QuestionDtoMapper.toDtoPage(result);
    }

}
