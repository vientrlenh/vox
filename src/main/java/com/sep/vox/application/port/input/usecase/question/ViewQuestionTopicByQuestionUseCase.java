package com.sep.vox.application.port.input.usecase.question;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class ViewQuestionTopicByQuestionUseCase {

    private final QuestionTopicRepository questionTopicRepository;

    public ViewQuestionTopicByQuestionUseCase(QuestionTopicRepository questionTopicRepository) {
        this.questionTopicRepository = questionTopicRepository;
    }

    @Transactional(readOnly = true)
    public QuestionTopicDto execute(UUID questionTopicId) {
        return questionTopicRepository.findById(questionTopicId)
            .map(QuestionTopicDtoMapper::toDto)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề"));
    }
}
