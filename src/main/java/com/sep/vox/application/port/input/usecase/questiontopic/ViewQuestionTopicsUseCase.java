package com.sep.vox.application.port.input.usecase.questiontopic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewQuestionTopicsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class ViewQuestionTopicsUseCase implements IUseCase<ViewQuestionTopicsQuery, PageResult<QuestionTopicDto>> {

    private final QuestionTopicRepository questionTopicRepository;

    public ViewQuestionTopicsUseCase(QuestionTopicRepository questionTopicRepository) {
        this.questionTopicRepository = questionTopicRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionTopicDto> execute(ViewQuestionTopicsQuery input) {
        return null;
    }
}
