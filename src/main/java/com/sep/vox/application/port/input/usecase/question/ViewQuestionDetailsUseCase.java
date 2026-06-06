package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
public class ViewQuestionDetailsUseCase implements IUseCase<ViewQuestionDetailsQuery, QuestionDto> {

    private final QuestionRepository questionRepository;

    public ViewQuestionDetailsUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDto execute(ViewQuestionDetailsQuery input) {
        var question = questionRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        return QuestionDtoMapper.toDto(question);
    }
}
