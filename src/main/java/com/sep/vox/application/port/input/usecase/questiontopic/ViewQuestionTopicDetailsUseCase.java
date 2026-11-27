package com.sep.vox.application.port.input.usecase.questiontopic;

import com.sep.vox.domain.model.question.QuestionTopic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.questiontopic.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
@RequiredArgsConstructor
public class ViewQuestionTopicDetailsUseCase implements IUseCase<ViewQuestionTopicDetailsQuery, QuestionTopicDto> {

    private final QuestionTopicRepository questionTopicRepository;

    @Override
    @Transactional(readOnly = true)
    public QuestionTopicDto execute(ViewQuestionTopicDetailsQuery input) {
        QuestionTopic topic = questionTopicRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Question topic not found"));
        return QuestionTopicDtoMapper.toDto(topic);
    }
}
