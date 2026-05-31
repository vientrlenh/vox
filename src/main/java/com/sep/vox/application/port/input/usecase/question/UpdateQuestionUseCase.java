package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.valueobject.DifficultyLevel;
import com.sep.vox.domain.valueobject.QuestionType;

@Service
public class UpdateQuestionUseCase implements IUseCase<UpdateQuestionCommand, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;

    public UpdateQuestionUseCase(QuestionRepository questionRepository, QuestionTopicRepository questionTopicRepository) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
    }

    @Override
    @Transactional
    public QuestionDto execute(UpdateQuestionCommand input) {
        var command = normalize(input);

        if (!questionTopicRepository.existsById(command.topicId())) {
            throw new NotFoundException("Không tìm thấy chủ đề câu hỏi");
        }

        var question = questionRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));

        question.setTopicId(command.topicId());
        question.setQuestionText(command.questionText());
        question.setAudioUrl(command.audioUrl());
        question.setDifficultyLevel(new DifficultyLevel(command.difficultyLevel()));
        question.setQuestionType(new QuestionType(command.questionType()));
        question.setDurationSeconds(command.durationSeconds());
        question.setActive(command.isActive());

        var saved = questionRepository.save(question);
        return QuestionDtoMapper.toDto(saved);
    }

    private UpdateQuestionCommand normalize(UpdateQuestionCommand input) {
        return new UpdateQuestionCommand(
            input.id(),
            input.topicId(),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            input.audioUrl(),
            StringNormalization.trimAndCollapseSpaces(input.difficultyLevel()),
            StringNormalization.trimAndCollapseSpaces(input.questionType()),
            input.durationSeconds(),
            input.isActive()
        );
    }
}
