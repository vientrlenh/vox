package com.sep.vox.domain.model.importfile;

public enum ImportSessionStatus {
    PREVIEWED, 
    VALIDATING, 
    IMPORTING, 
    QUEUED, 
    COMPLETED, 
    FAILED, 
    EXPIRED,
    CANCELLED
}
