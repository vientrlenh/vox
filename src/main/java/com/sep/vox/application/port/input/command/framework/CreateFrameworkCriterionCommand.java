package com.sep.vox.application.port.input.command.framework;

public record CreateFrameworkCriterionCommand(
    String code, 
    String name, 
    String description
) {
    
}
