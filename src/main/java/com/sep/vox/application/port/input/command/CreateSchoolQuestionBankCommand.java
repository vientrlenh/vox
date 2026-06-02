package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSchoolQuestionBankCommand(
    UUID languageId, 
    UUID schoolId, 
    String code, 
    String name, 
    String description
) {
    
}
