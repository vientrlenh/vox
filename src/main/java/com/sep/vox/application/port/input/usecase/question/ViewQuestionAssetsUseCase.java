package com.sep.vox.application.port.input.usecase.question;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.repository.QuestionAssetRepository;

@Service
public class ViewQuestionAssetsUseCase {

    private final QuestionAssetRepository questionAssetRepository;

    public ViewQuestionAssetsUseCase(QuestionAssetRepository questionAssetRepository) {
        this.questionAssetRepository = questionAssetRepository;
    }

    @Transactional(readOnly = true)
    public List<QuestionAssetDto> execute(UUID questionId) {
        return questionAssetRepository.findByQuestionId(questionId)
            .stream()
            .map(QuestionAssetDtoMapper::toDto)
            .toList();
    }
}
