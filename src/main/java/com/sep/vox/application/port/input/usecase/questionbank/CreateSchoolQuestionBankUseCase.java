package com.sep.vox.application.port.input.usecase.questionbank;

import com.sep.vox.domain.model.school.SchoolUser;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.mapper.questionbank.CreateQuestionBankResponseMapper;
import com.sep.vox.application.port.input.command.questionbank.CreateSchoolQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
@RequiredArgsConstructor
public class CreateSchoolQuestionBankUseCase implements IUseCase<CreateSchoolQuestionBankCommand, CreateQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public CreateQuestionBankResponse execute(CreateSchoolQuestionBankCommand input) {
        CreateSchoolQuestionBankCommand command = normalize(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new ForbiddenException("Invalid access"));

        QuestionBank questionBank = QuestionBank.create(
            command.languageId(), 
            schoolUser.getSchoolId(), 
            command.code(), 
            command.name(), 
            command.description(), 
            QuestionBankOwnerType.SCHOOL
        );

        QuestionBank saved = questionBankRepository.save(questionBank);
        return CreateQuestionBankResponseMapper.toResponse(saved.getId());
    }

    private CreateSchoolQuestionBankCommand normalize(CreateSchoolQuestionBankCommand input) {
        return new CreateSchoolQuestionBankCommand(
            input.languageId(), 
            input.schoolId(), 
            StringNormalization.normalizeCode(input.code()), 
            StringNormalization.trimAndCollapseSpaces(input.name()), 
            StringNormalization.trimAndCollapseSpaces(input.description())
        );
    }
    
}
