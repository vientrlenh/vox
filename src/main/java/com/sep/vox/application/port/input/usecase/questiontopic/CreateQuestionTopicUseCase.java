package com.sep.vox.application.port.input.usecase.questiontopic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.questiontopic.CreateQuestionTopicResponseMapper;
import com.sep.vox.application.port.input.command.CreateQuestionTopicCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.questiontopic.CreateQuestionTopicResponse;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class CreateQuestionTopicUseCase implements IUseCase<CreateQuestionTopicCommand, CreateQuestionTopicResponse> {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;

    public CreateQuestionTopicUseCase(QuestionTopicRepository questionTopicRepository, QuestionBankRepository questionBankRepository) {
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
    }

    @Override
    @Transactional
    public CreateQuestionTopicResponse execute(CreateQuestionTopicCommand input) {
        var command = normalize(input);

        if (!questionBankRepository.existsById(command.bankId())) {
            throw new NotFoundException("Không tìm thấy ngân hàng câu hỏi");
        }

        var topic = new QuestionTopic();
        var saved = questionTopicRepository.save(topic);
        return CreateQuestionTopicResponseMapper.toResponse(saved.getId());
    }

    private CreateQuestionTopicCommand normalize(CreateQuestionTopicCommand input) {
        return new CreateQuestionTopicCommand(
            input.bankId(),
            StringNormalization.trimAndCollapseSpaces(input.topicName()),
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
}
