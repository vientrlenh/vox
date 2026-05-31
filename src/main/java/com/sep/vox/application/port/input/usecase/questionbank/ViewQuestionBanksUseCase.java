package com.sep.vox.application.port.input.usecase.questionbank;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.repository.QuestionBankRepository;

@Service
public class ViewQuestionBanksUseCase implements IUseCase<ViewQuestionBanksQuery, PageResult<QuestionBankDto>> {

    private final QuestionBankRepository questionBankRepository;

    public ViewQuestionBanksUseCase(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionBankDto> execute(ViewQuestionBanksQuery input) {
        var result = questionBankRepository.findAll(new PageRequest(input.page(), input.size()));
        return QuestionBankDtoMapper.toDtoPage(result);
    }
}
