package com.sep.vox.application.port.input.usecase.question;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.mapper.QuestionEvaluationGuideDtoMapper;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;

@Service
public class ViewQuestionEvaluationGuideUseCase {

    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;

    public ViewQuestionEvaluationGuideUseCase(QuestionEvaluationGuideRepository questionEvaluationGuideRepository) {
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
    }

    @Transactional(readOnly = true)
    public Optional<QuestionEvaluationGuideDto> execute(UUID questionId) {
        return questionEvaluationGuideRepository.findByQuestionId(questionId)
            .map(QuestionEvaluationGuideDtoMapper::toDto);
    }
}
