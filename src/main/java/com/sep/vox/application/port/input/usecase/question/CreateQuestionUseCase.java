package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.valueobject.DifficultyLevel;
import com.sep.vox.domain.valueobject.QuestionType;

@Service
public class CreateQuestionUseCase implements IUseCase<CreateQuestionCommand, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;

    public CreateQuestionUseCase(QuestionRepository questionRepository, QuestionTopicRepository questionTopicRepository) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
    }

    @Override
    @Transactional
    public QuestionDto execute(CreateQuestionCommand input) {
        var command = normalize(input);

        if (!questionTopicRepository.existsById(command.topicId())) {
            throw new NotFoundException("Không tìm thấy chủ đề câu hỏi");
        }

        var question = new Question(
            command.topicId(),
            command.questionText(),
            command.audioUrl(),
            new DifficultyLevel(command.difficultyLevel()),
            new QuestionType(command.questionType()),
            command.durationSeconds(),
            true,
            OffsetDateTime.now()
        );

        var saved = questionRepository.save(question);
        return QuestionDtoMapper.toDto(saved);
    }

    private CreateQuestionCommand normalize(CreateQuestionCommand input) {
        return new CreateQuestionCommand(
            input.topicId(),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            input.audioUrl(),
            StringNormalization.trimAndCollapseSpaces(input.difficultyLevel()),
            StringNormalization.trimAndCollapseSpaces(input.questionType()),
            input.durationSeconds()
        );
    }
}
