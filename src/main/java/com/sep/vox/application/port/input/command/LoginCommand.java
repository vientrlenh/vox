package com.sep.vox.application.port.input.command;

public record LoginCommand(
    String login,
    String password
) {
    
}
