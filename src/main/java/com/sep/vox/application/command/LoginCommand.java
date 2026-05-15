package com.sep.vox.application.command;

public record LoginCommand(
    String login,
    String password
) {
    
}
