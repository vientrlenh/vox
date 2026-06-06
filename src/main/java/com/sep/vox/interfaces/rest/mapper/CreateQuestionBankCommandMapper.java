package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateSchoolQuestionBankCommand;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateSchoolQuestionBankRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateSystemQuestionBankRequest;

public final class CreateQuestionBankCommandMapper {

    public static CreateSystemQuestionBankCommand fromSystemRequest(CreateSystemQuestionBankRequest request) {
        return new CreateSystemQuestionBankCommand(
            request.languageId(), 
            request.code(), 
            request.name(), 
            request.description()
        );
    }

    public static CreateSchoolQuestionBankCommand fromSchoolRequest(CreateSchoolQuestionBankRequest request) {
        return new CreateSchoolQuestionBankCommand(
            request.languageId(), 
            request.schoolId(), 
            request.code(), 
            request.name(), 
            request.description()
        );
    }
}
