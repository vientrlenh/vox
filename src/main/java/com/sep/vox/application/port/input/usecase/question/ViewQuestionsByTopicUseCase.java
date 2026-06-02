package com.sep.vox.application.port.input.usecase.question;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewQuestionsByTopicQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.languagelevel.LevelFramework;
import com.sep.vox.domain.model.languagelevel.StandardLevel;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.repository.LevelFrameworkRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.StandardLevelRepository;

@Service
public class ViewQuestionsByTopicUseCase implements IUseCase<ViewQuestionsByTopicQuery, PageResult<QuestionDto>> {

    private final QuestionRepository questionRepository;
    private final StandardLevelRepository standardLevelRepository;
    private final LevelFrameworkRepository levelFrameworkRepository;

    public ViewQuestionsByTopicUseCase(QuestionRepository questionRepository, StandardLevelRepository standardLevelRepository,
            LevelFrameworkRepository levelFrameworkRepository) {
        this.questionRepository = questionRepository;
        this.standardLevelRepository = standardLevelRepository;
        this.levelFrameworkRepository = levelFrameworkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuestionDto> execute(ViewQuestionsByTopicQuery input) {
        var result = questionRepository.findByTopicId(input.topicId(), new PageRequest(input.page(), input.size()));
        var standardLevels = fetchStandardLevels(result.content());
        var standardLevelCodeMap = standardLevels.stream()
            .collect(Collectors.toMap(StandardLevel::getId, sl -> sl.getCode().value()));
        var standardLevelToFrameworkMap = standardLevels.stream()
            .collect(Collectors.toMap(StandardLevel::getId, StandardLevel::getFrameworkId));
        var frameworkMap = fetchFrameworks(standardLevels).stream()
            .collect(Collectors.toMap(LevelFramework::getId, f -> f));
        return QuestionDtoMapper.toDtoPage(result, standardLevelCodeMap, standardLevelToFrameworkMap, frameworkMap);
    }

    private List<StandardLevel> fetchStandardLevels(List<Question> questions) {
        var ids = questions.stream().map(Question::getStandardLevelId).distinct().toList();
        return standardLevelRepository.findAllByIds(ids);
    }

    private List<LevelFramework> fetchFrameworks(List<StandardLevel> standardLevels) {
        var frameworkIds = standardLevels.stream().map(StandardLevel::getFrameworkId).distinct().toList();
        return levelFrameworkRepository.findAllByIds(frameworkIds);
    }
}
