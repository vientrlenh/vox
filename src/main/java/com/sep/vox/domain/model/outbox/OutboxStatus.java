package com.sep.vox.domain.model.outbox;

public enum OutboxStatus {
    PENDING, 
    PROCESSING, 
    PUBLISHED, 
    FAILED
}
