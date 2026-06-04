package com.sep.vox.application.common;

public record UploadedFile(
    String fileName, 
    String contentType, 
    long size, 
    byte[] content
) {
    
}
