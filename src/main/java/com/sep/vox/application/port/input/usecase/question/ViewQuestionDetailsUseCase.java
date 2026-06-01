package com.sep.vox.application.port.input.usecase.question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.repository.LevelFrameworkRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.StandardLevelRepository;

@Service
public class ViewQuestionDetailsUseCase implements IUseCase<ViewQuestionDetailsQuery, QuestionDto> {

    private final QuestionRepository questionRepository;
    private final StandardLevelRepository standardLevelRepository;
    private final LevelFrameworkRepository levelFrameworkRepository;

    public ViewQuestionDetailsUseCase(QuestionRepository questionRepository, StandardLevelRepository standardLevelRepository,
            LevelFrameworkRepository levelFrameworkRepository) {
        this.questionRepository = questionRepository;
        this.standardLevelRepository = standardLevelRepository;
        this.levelFrameworkRepository = levelFrameworkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDto execute(ViewQuestionDetailsQuery input) {
        var question = questionRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        var standardLevel = standardLevelRepository.findById(question.getStandardLevelId()).orElse(null);
        var slCode = standardLevel != null ? standardLevel.getCode().value() : null;
        var framework = standardLevel != null
            ? levelFrameworkRepository.findById(standardLevel.getFrameworkId()).orElse(null)
            : null;
        var fwCode = framework != null ? framework.getCode().value() : null;
        var fwName = framework != null ? framework.getName() : null;
        return QuestionDtoMapper.toDto(question, slCode, fwCode, fwName);
    }
}
