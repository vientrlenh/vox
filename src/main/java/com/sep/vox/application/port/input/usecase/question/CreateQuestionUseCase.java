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
import com.sep.vox.domain.repository.LevelFrameworkRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.StandardLevelRepository;
import com.sep.vox.domain.valueobject.QuestionType;

@Service
public class CreateQuestionUseCase implements IUseCase<CreateQuestionCommand, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final StandardLevelRepository standardLevelRepository;
    private final LevelFrameworkRepository levelFrameworkRepository;

    public CreateQuestionUseCase(QuestionRepository questionRepository, QuestionTopicRepository questionTopicRepository,
            StandardLevelRepository standardLevelRepository, LevelFrameworkRepository levelFrameworkRepository) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.standardLevelRepository = standardLevelRepository;
        this.levelFrameworkRepository = levelFrameworkRepository;
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
            command.standardLevelId(),
            new QuestionType(command.questionType()),
            command.durationSeconds(),
            true,
            OffsetDateTime.now()
        );

        var saved = questionRepository.save(question);
        var standardLevel = standardLevelRepository.findById(saved.getStandardLevelId()).orElse(null);
        var slCode = standardLevel != null ? standardLevel.getCode().value() : null;
        var framework = standardLevel != null
            ? levelFrameworkRepository.findById(standardLevel.getFrameworkId()).orElse(null)
            : null;
        var fwCode = framework != null ? framework.getCode().value() : null;
        var fwName = framework != null ? framework.getName() : null;
        return QuestionDtoMapper.toDto(saved, slCode, fwCode, fwName);
    }

    private CreateQuestionCommand normalize(CreateQuestionCommand input) {
        return new CreateQuestionCommand(
            input.topicId(),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            input.audioUrl(),
            input.standardLevelId(),
            StringNormalization.trimAndCollapseSpaces(input.questionType()),
            input.durationSeconds()
        );
    }
}
