package com.sep.vox.application.port.input.usecase.questiontopic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class ViewQuestionTopicDetailsUseCase implements IUseCase<ViewQuestionTopicDetailsQuery, QuestionTopicDto> {

    private final QuestionTopicRepository questionTopicRepository;

    public ViewQuestionTopicDetailsUseCase(QuestionTopicRepository questionTopicRepository) {
        this.questionTopicRepository = questionTopicRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionTopicDto execute(ViewQuestionTopicDetailsQuery input) {
        var topic = questionTopicRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi"));
        return QuestionTopicDtoMapper.toDto(topic);
    }
}
