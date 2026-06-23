package com.sep.vox.application.port.input.service;

public record ImportCommitResult(
    long created, 
    long updated, 
    long skipped, 
    long invalid
) {
    
}
