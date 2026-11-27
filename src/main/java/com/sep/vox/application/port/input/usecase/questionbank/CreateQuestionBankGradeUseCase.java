package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.model.question.QuestionBank;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.questionbank.CreateQuestionBankGradeCommand;
import com.sep.vox.application.port.input.service.QuestionBankManageAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.QuestionBankGradeDto;
import com.sep.vox.domain.mapper.QuestionBankGradeDtoMapper;
import com.sep.vox.domain.model.question.QuestionBankGrade;
import com.sep.vox.domain.repository.QuestionBankGradeRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

@Service
@RequiredArgsConstructor
public class CreateQuestionBankGradeUseCase implements IUseCase<CreateQuestionBankGradeCommand, QuestionBankGradeDto> {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionBankGradeRepository questionBankGradeRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final QuestionBankManageAccessService questionBankManageAccessService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public QuestionBankGradeDto execute(CreateQuestionBankGradeCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        QuestionBank questionBank = questionBankRepository.findById(input.questionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));

        questionBankManageAccessService.requireManageAccess(questionBank, currentUserId);

        schoolGradeRepository.findById(input.schoolGradeId())
            .orElseThrow(() -> new ResourceNotFoundException("Grade level not found"));

        if (questionBankGradeRepository.existsByQuestionBankIdAndSchoolGradeId(questionBank.getId(), input.schoolGradeId())) {
            throw new ResourceDuplicatedException("This grade level is already attached to the question bank");
        }

        QuestionBankGrade grade = QuestionBankGrade.attached(
                questionBank.getId(), input.schoolGradeId(), Instant.now(), currentUserId);
        QuestionBankGrade saved = questionBankGradeRepository.save(grade);
        return QuestionBankGradeDtoMapper.toDto(saved);
    }

}
