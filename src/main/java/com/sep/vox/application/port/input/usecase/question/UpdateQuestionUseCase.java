package com.sep.vox.application.port.input.usecase.question;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class UpdateQuestionUseCase implements IUseCase<UpdateQuestionCommand, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;

    public UpdateQuestionUseCase(
            QuestionRepository questionRepository,
            QuestionTopicRepository questionTopicRepository) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
    }

    @Override
    @Transactional
    public QuestionDto execute(UpdateQuestionCommand input) {
        var command = normalize(input);

        if (!questionTopicRepository.existsById(command.topicId())) {
            throw new NotFoundException("Khong tim thay chu de cau hoi");
        }

        var question = questionRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Khong tim thay cau hoi"));

        question.setQuestionTopicId(command.topicId());
        question.setQuestionText(command.questionText());
        question.setStandardLevelVersionId(command.standardLevelId());
        question.setSchoolLevelVersionId(null);
        question.setType(QuestionType.valueOf(command.questionType()));
        question.setMaxResponseSeconds(command.durationSeconds());
        question.setStatus(command.isActive() ? QuestionStatus.PUBLISHED : QuestionStatus.ARCHIVED);
        question.setUpdatedAt(OffsetDateTime.now());

        var saved = questionRepository.save(question);
        return QuestionDtoMapper.toDto(saved);
    }

    private UpdateQuestionCommand normalize(UpdateQuestionCommand input) {
        return new UpdateQuestionCommand(
            input.id(),
            input.topicId(),
            StringNormalization.trimAndCollapseSpaces(input.questionText()),
            StringNormalization.trimAndCollapseSpaces(input.audioUrl()),
            input.standardLevelId(),
            StringNormalization.trimAndCollapseSpaces(input.questionType()),
            input.durationSeconds(),
            input.isActive()
        );
    }
}
