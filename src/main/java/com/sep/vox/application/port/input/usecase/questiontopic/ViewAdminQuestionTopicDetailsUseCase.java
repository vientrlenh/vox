package com.sep.vox.application.port.input.usecase.questiontopic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.QuestionTopicReadQueryRepository;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Service
public class ViewAdminQuestionTopicDetailsUseCase implements IUseCase<ViewQuestionTopicDetailsQuery, QuestionTopicDto> {

    private final QuestionTopicReadQueryRepository questionTopicReadQueryRepository;

    public ViewAdminQuestionTopicDetailsUseCase(QuestionTopicReadQueryRepository questionTopicReadQueryRepository) {
        this.questionTopicReadQueryRepository = questionTopicReadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionTopicDto execute(ViewQuestionTopicDetailsQuery input) {
        return questionTopicReadQueryRepository.findAdminTopicDetail(input.id())
            .orElseThrow(() -> new NotFoundException("Khong tim thay chu de cau hoi"));
    }
}
