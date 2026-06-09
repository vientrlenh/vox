package com.sep.vox.application.port.input.usecase.questiontopic;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class UpdateQuestionTopicUseCase implements IUseCase<UpdateQuestionTopicCommand, QuestionTopicDto> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserContextPort userContextPort;

    public UpdateQuestionTopicUseCase(QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository, UserContextPort userContextPort) {
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public QuestionTopicDto execute(UpdateQuestionTopicCommand input) {
        var command = normalize(input);

        if (!questionBankRepository.existsById(command.bankId())) {
            throw new NotFoundException("Không tìm thấy ngân hàng câu hỏi");
        }

        var topic = questionTopicRepository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi"));

        topic.setName(command.topicName());
        topic.setDescription(command.description());
        topic.setUpdatedAt(OffsetDateTime.now());
        topic.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());

        var saved = questionTopicRepository.save(topic);
        return QuestionTopicDtoMapper.toDto(saved);
    }

    private UpdateQuestionTopicCommand normalize(UpdateQuestionTopicCommand input) {
        return new UpdateQuestionTopicCommand(
            input.id(),
            input.bankId(),
            StringNormalization.trimAndCollapseSpaces(input.topicName()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
