package com.sep.vox.application.port.input.usecase.questionbank;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.mapper.questionbank.CreateQuestionBankResponseMapper;
import com.sep.vox.application.port.input.command.CreateSchoolQuestionBankCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.questionbank.CreateQuestionBankResponse;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class CreateSchoolQuestionBankUseCase implements IUseCase<CreateSchoolQuestionBankCommand, CreateQuestionBankResponse> {

    private final QuestionBankRepository questionBankRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolQuestionBankUseCase(QuestionBankRepository questionBankRepository, SchoolUserRepository schoolUserRepository, UserContextPort userContextPort) {
        this.questionBankRepository = questionBankRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public CreateQuestionBankResponse execute(CreateSchoolQuestionBankCommand input) {
        var command = normalize(input);

        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolUser = schoolUserRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập không hợp lệ"));

        var now = Instant.now();
        var questionBank = QuestionBank.create(
            command.languageId(), 
            schoolUser.getSchoolId(), 
            command.code(), 
            command.name(), 
            command.description(), 
            QuestionBankOwnerType.SCHOOL, 
            now, 
            currentUserId
        );

        var saved = questionBankRepository.save(questionBank);
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
