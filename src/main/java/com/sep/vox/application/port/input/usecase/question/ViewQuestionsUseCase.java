package com.sep.vox.application.port.input.usecase.question;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewQuestionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class ViewQuestionsUseCase implements IUseCase<ViewQuestionsQuery, PageResult<QuestionDto>> {

    private final QuestionRepository questionRepository;

    public ViewQuestionsUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewQuestionsQuery input) {
        var result = questionRepository.findAll(new PageRequest(input.page(), input.size()));
        
        return QuestionDtoMapper.toDtoPage(result);
    }
}
