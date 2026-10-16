package com.sep.vox.application.port.input.command.framework;

public record CreateFrameworkResultBandCommand(
    String code, 
    String label, 
    String description, 
    Integer order
) {
    
}
