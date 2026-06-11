package com.sep.vox.application.port.input.usecase.questiontopic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionReadQueryRepository;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Service
public class ViewAdminQuestionTopicDetailsUseCase implements IUseCase<ViewQuestionTopicDetailsQuery, QuestionTopicDto> {

    private final QuestionReadQueryRepository questionReadQueryRepository;

    public ViewAdminQuestionTopicDetailsUseCase(QuestionReadQueryRepository questionReadQueryRepository) {
        this.questionReadQueryRepository = questionReadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionTopicDto execute(ViewQuestionTopicDetailsQuery input) {
        return questionReadQueryRepository.findAdminTopicDetail(input.id())
            .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y chá»§ Ä‘á» cÃ¢u há»i"));
    }
}
