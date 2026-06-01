package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.LevelFrameworkRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.StandardLevelRepository;
import com.sep.vox.domain.valueobject.QuestionType;

@Service
public class UpdateQuestionUseCase implements IUseCase<UpdateQuestionCommand, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final StandardLevelRepository standardLevelRepository;
    private final LevelFrameworkRepository levelFrameworkRepository;

    public UpdateQuestionUseCase(QuestionRepository questionRepository, QuestionTopicRepository questionTopicRepository,
            StandardLevelRepository standardLevelRepository, LevelFrameworkRepository levelFrameworkRepository) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.standardLevelRepository = standardLevelRepository;
        this.levelFrameworkRepository = levelFrameworkRepository;
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
        question.setStandardLevelId(command.standardLevelId());
        question.setQuestionType(new QuestionType(command.questionType()));
        question.setDurationSeconds(command.durationSeconds());
        question.setActive(command.isActive());

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

    private UpdateQuestionCommand normalize(UpdateQuestionCommand input) {
        return new UpdateQuestionCommand(
            input.id(),
            input.topicId(),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            input.audioUrl(),
            input.standardLevelId(),
            StringNormalization.trimAndCollapseSpaces(input.questionType()),
            input.durationSeconds(),
            input.isActive()
        );
    }
}
